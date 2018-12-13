package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

	protected Map<Transition, Integer> transitionKeys = new HashMap<>();
	// TODO only used for construction?? reuse them?!
	protected int[][] oneTransitionFormulas; // (Transition, 1..n) -> fireTransitionID
	protected int[][] deadlockSubFormulas; // (Transition, 1..n) -> deadlockID
	protected int[][] terminatingSubFormulas; // (Transition, 1..n) -> terminatingID
	
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
	
	protected void writeDeadlock() throws IOException {
		String[] deadlock = getDeadlock();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			dl[i] = createUniqueID();
			writer.write(dl[i] + " = " + deadlock[i]);
		}
	}

	protected String[] getDeadlock() throws IOException {
		writeDeadlockSubFormulas(1, getSolvingObject().getN());
		String[] deadlock = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.clear();
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				and.add(deadlockSubFormulas[transitionKeys.get(t)][i]);
			}
			deadlock[i] = writeAnd(and);
		}
		return deadlock;
	}

	protected void writeDeadlockSubFormulas(int s, int e) throws IOException {
		Set<Integer> or = new HashSet<>();
		int number;
		int strat;
		for (int i = s; i <= e; ++i) {
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				or.clear();
				for (Place p : t.getPreset()) {
					or.add(-getVarNr(p.getId() + "." + i, true)); // "p.i"
					strat = addSysStrategy(p, t);
					if (strat != 0) {
						or.add(-strat);
					}
				}
				number = createUniqueID();
				writer.write(number + " = " + writeOr(or));
				deadlockSubFormulas[transitionKeys.get(t)][i] = number;
			}
		}
	}
	
	protected void writeTerminating() throws IOException {
		String[] terminating = new String[getSolvingObject().getN() + 1];
		terminating = getTerminating();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			term[i] = createUniqueID();
			writer.write(term[i] + " = " + terminating[i]);
		}
	}

	protected String[] getTerminating() throws IOException {
		writeTerminatingSubFormulas(1, getSolvingObject().getN());
		String[] terminating = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			if (getSolvingObject().getGame().getTransitions().size() >= 1) {
				and.clear();
				for (Transition t : getSolvingObject().getGame().getTransitions()) {
					and.add(terminatingSubFormulas[transitionKeys.get(t)][i]);
				}
				terminating[i] = writeAnd(and);
			}
		}
		return terminating;
	}

	protected void writeTerminatingSubFormulas(int s, int e) throws IOException {
		Set<Integer> or = new HashSet<>();
		Set<Place> pre;
		int key;
		for (int i = s; i <= e; ++i) {
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				pre = t.getPreset();
				or.clear();
				for (Place p : pre) {
					or.add(-getVarNr(p.getId() + "." + i, true));
				}
				key = createUniqueID();
				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[transitionKeys.get(t)][i] = key;
			}
		}
	}
	
	protected void writeDeterministic() throws IOException {
		if (QbfControl.deterministicStrat) {
			String[] deterministic = getDeterministic();
			for (int i = 1; i <= getSolvingObject().getN(); ++i) {
				if (!deterministic[i].startsWith("and()")) {
					det[i] = createUniqueID();
					writer.write(det[i] + " = " + deterministic[i]);
				} else {
					Pair<Boolean, Integer> result = getVarNrWithResult("and()");
					if (result.getFirst()) {
						writer.write(result.getSecond() + " = " + writeAnd(new HashSet<>()));
					}
					det[i] = result.getSecond();
				}
			}
		}
	}

	protected String[] getDeterministic() throws IOException { // faster than naive implementation
		List<Set<Integer>> and = new ArrayList<>(getSolvingObject().getN() + 1);
		and.add(null); // first element is never accessed
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.add(new HashSet<Integer>());
		}
		Transition t1, t2;
		Transition[] sys_transitions;
		for (Place sys : getSolvingObject().getGame().getPlaces()) {
			// Additional system places ARE FORCED to behave deterministically in getDetAdditionalSys, not here since the true concurrent flow fired every enabled transition
			if (!getSolvingObject().getGame().isEnvironment(sys) && !sys.getId().startsWith(QbfControl.additionalSystemName)) {
				if (sys.getPostset().size() > 1) {
					sys_transitions = sys.getPostset().toArray(new Transition[0]);
					for (int j = 0; j < sys_transitions.length; ++j) {
						t1 = sys_transitions[j];
						for (int k = j + 1; k < sys_transitions.length; ++k) {
							t2 = sys_transitions[k];
							for (int i = 1; i <= getSolvingObject().getN(); ++i) {
								and.get(i).add(writeOneMissingPre(t1, t2, i));
							}
						}
					}
				}
			}
		}
		String[] deterministic = new String[getSolvingObject().getN() + 1];
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			deterministic[i] = writeAnd(and.get(i));
		}
		return deterministic;
	}

	protected int writeOneMissingPre(Transition t1, Transition t2, int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		int strat;
		for (Place p : t1.getPreset()) {
			or.add(-getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t1);
			if (strat != 0) {
				or.add(-strat);
			}
		}
		for (Place p : t2.getPreset()) {
			or.add(-getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t2);
			if (strat != 0) {
				or.add(-strat);
			}
		}

		int number = createUniqueID();
		writer.write(number + " = " + writeOr(or));
		return number;
	}
	
	protected void writeLoop() throws IOException {
		String loop = getLoopIJ();
		l = createUniqueID();
		writer.write(l + " = " + loop);
	}

	protected String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (int j = i + 1; j <= getSolvingObject().getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
						int p_i = getVarNr(p.getId() + "." + i, true);
						int p_j = getVarNr(p.getId() + "." + j, true);
						and.add(writeImplication(p_i, p_j));
						and.add(writeImplication(p_j, p_i));
					}
				}
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
		}
		return writeOr(or);
	}
	
	// wahrscheinlich nur hilfreich für deterministic unfolding, macht aber auf jeden fall nichts kaputt, wohl nur langsamer
	protected String getLoopIJunfolded() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (int j = i + 1; j <= getSolvingObject().getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
						int p_i = getVarNr(p.getId() + "." + i, true);
						Set<Integer> innerOr = new HashSet<>();
						for (Place unfoldedP : unfoldingsOf(p)) {
							innerOr.add(getVarNr(unfoldedP.getId() + "." + j, true));
						}
						int innerOrNumber = createUniqueID();
						writer.write(innerOrNumber + " = " + writeOr(innerOr));
						and.add(writeImplication(p_i, innerOrNumber));
						and.add(writeImplication(innerOrNumber, p_i));
					}
				}
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				outerOr.add(andNumber);
			}
		}
		return writeOr(outerOr);
	}

	private Set<Place> unfoldingsOf(Place place) {
		Set<Place> result = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (/* !p.equals(place) && */getTruncatedId(place.getId()).equals(getTruncatedId(p.getId()))) { // forcing into different unfolded place yields more necessary unfoldings
				result.add(p);
			}
		}
		return result;
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
	
	protected void initialize(int n) { // TODO naming? merge mit QCIR write?
		fl = new int[n + 1];
		det = new int[n + 1];
		dlt = new int[n + 1];
		dl = new int[n + 1];
		term = new int[n + 1];
		seq = new int[n + 1];
		win = new int[n + 1];
		seqImpliesWin = new int[n + 1];
	}
	
	protected void initializeForQcirWrite() {
		transitions = new Transition[getSolvingObject().getGame().getTransitions().size()];
		int i = 0;
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			transitions[i] = t;
			transitionKeys.put(t, i);
			i++;
		}
		
		int numberOfTransitions = getSolvingObject().getGame().getTransitions().size();
		int n = getSolvingObject().getN();
		deadlockSubFormulas = new int[numberOfTransitions][n + 1];
		terminatingSubFormulas = new int[numberOfTransitions][n + 1];
		oneTransitionFormulas = new int[numberOfTransitions][n + 1];
	}

	protected int addSysStrategy(Place p, Transition t) {
		if (!getSolvingObject().getGame().isEnvironment(p)) {
			if (p.getId().startsWith(QbfControl.additionalSystemName)) {
				return getVarNr(p.getId() + ".." + t.getId(), true);
			} else {
				return getVarNr(p.getId() + ".." + getTruncatedId(t.getId()), true);
			}
		} else {
			return 0;
		}
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
