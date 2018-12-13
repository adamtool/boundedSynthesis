package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.solver.SolverOptions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public abstract class SolverQbfAndQbfCon<W extends WinningCondition, SOP extends SolverOptions> extends Solver<QbfSolvingObject<W>, SOP>{

	// steps of solving
	public QbfSolvingObject<W> originalSolvingObject;
	public PetriGame originalGame;
	public PetriGame unfolding;
	public PetriGame strategy;
	
	// variable to store keys of calculated components for later use (shared among all solvers)
	protected int in;
	protected int l;
	protected int u;
	protected int[] fl;
	protected int[] det;
	protected int[] dlt;
	protected int[] dl;
	protected int[] term;
	protected int[] seq;
	protected int[] win;
	protected int[] seqImpliesWin;
	
	// keys
	protected Map<Integer, String> exists_transitions = new HashMap<>();
	
	// storing QBF result for strategy generation
	protected String outputQBFsolver;
	
	// caches
	protected Transition[] transitions;
	protected File file;
	
	// solving
	protected BufferedWriter writer;
	protected int variablesCounter = 2; // 1 reserved for phi TODO why does sequential work with 1?!
	protected Map<String, Integer> numbersForVariables = new HashMap<>(); // map for storing keys and the corresponding value


	protected SolverQbfAndQbfCon(QbfSolvingObject<W> solverObject, SOP options) throws SolvingException {
		super(solverObject, options);
		
		originalSolvingObject = getSolvingObject().getCopy();
		
		// create random file in tmp directory which is deleted after solving it
		String prefix = "";
		final Random rand = new Random();
		final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";
		for (int i = 0; i < 20; ++i) {
			prefix += lexicon.charAt(rand.nextInt(lexicon.length()));
		}
		try {
			file = File.createTempFile(prefix, /* pn.getName() + */ ".qcir");
		} catch (IOException e) {
			throw new SolvingException("Generation of QBF-file failed.", e.fillInStackTrace());
		}
		file.deleteOnExit();
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			throw new SolvingException("Writing of QBF-file failed.", e.fillInStackTrace());
		}
	}
	
	protected void writeInitial() throws IOException {
		in = createUniqueID();
		writer.write(in + " = " + getInitial());
	}

	protected String getInitial() {
		Marking initialMarking = getSolvingObject().getGame().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (initialMarking.getToken(p).getValue() == 1) {
				initial.add(getVarNr(p.getId() + "." + 1, true));
			} else {
				initial.add(-getVarNr(p.getId() + "." + 1, true));
			}
		}
		return writeAnd(initial);
	}
	
	protected void initializeNandB(int n, int b) throws BoundedParameterMissingException {
		if (n == -1) {
			if (getSolvingObject().hasBoundedParameterNinExtension()) {
				n = getSolvingObject().getBoundedParameterNFromExtension();
			}
		}
		if (b == -1) {
			if (getSolvingObject().hasBoundedParameterBinExtension()) {
				b = getSolvingObject().getBoundedParameterBFromExtension();
			}
		}
		if (n == -1 || b == -1) {
			throw new BoundedParameterMissingException(n, b);
		}
		getSolvingObject().setN(n);
		getSolvingObject().setB(b);
	}
	
	protected void initializeArrays(int n) { // TODO naming
		fl = new int[n + 1];
		det = new int[n + 1];
		dlt = new int[n + 1];
		dl = new int[n + 1];
		term = new int[n + 1];
		seq = new int[n + 1];
		win = new int[n + 1];
		seqImpliesWin = new int[n + 1];
	}
	
	protected int getVarNr(String id, boolean extraCheck) {
		Integer ret = numbersForVariables.get(id);
		if (ret != null) {
			return ret;
		} else if (extraCheck) {
			throw new IllegalArgumentException("Could not but should have found: " + id);
		} else {
			return createVariable(id);
		}
	}

	protected Pair<Boolean, Integer> getVarNrWithResult(String id) {
		Integer ret = numbersForVariables.get(id);
		if (ret != null) {
			return new Pair<>(false, ret);
		} else {
			return new Pair<>(true, createVariable(id));
		}
	}

	protected int createVariable(String id) {
		numbersForVariables.put(id, variablesCounter);
		return variablesCounter++;
	}

	protected int createUniqueID() {
		return variablesCounter++;
	}

	protected String getTruncatedId(String id) {
		int index = id.indexOf("__");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}

	// WRITERS:
	protected String writeExists(Set<Integer> input) {
		return writeString("exists", input);
	}

	protected String writeForall(Set<Integer> input) {
		return writeString("forall", input);
	}

	protected String writeOr(Set<Integer> input) {
		return writeString("or", input);
	}

	protected String writeAnd(Set<Integer> input) {
		return writeString("and", input);
	}

	private String writeString(String op, Set<Integer> input) {
		StringBuilder sb = new StringBuilder(op);
		sb.append("(");
		String delim = ""; // first element is added without ","

		for (int i : input) {
			sb.append(delim);
			delim = ",";
			sb.append(i);
		}
		sb.append(")" + QbfControl.linebreak);
		String result = sb.toString();
		return result;
	}

	protected int writeImplication(int from, int to) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("Variable:" + from + "=>Variable:" + to);
		int number = result.getSecond();
		if (result.getFirst()) {
			Set<Integer> or = new HashSet<>();
			or.add(-from);
			or.add(to);
			writer.write(number + " = " + writeOr(or));
		}
		return number;
	}
}
