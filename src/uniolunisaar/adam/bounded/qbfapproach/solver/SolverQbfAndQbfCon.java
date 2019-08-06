package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.connectivity.Connectivity;
import uniol.apt.analysis.coverability.CoverabilityGraph;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.McMillianUnfolder;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.Unfolder;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.solver.SolverOptions;
import uniolunisaar.adam.exceptions.pg.CalculationInterruptedException;
import uniolunisaar.adam.exceptions.pg.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.tools.AdamProperties;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public abstract class SolverQbfAndQbfCon<W extends Condition, SOP extends SolverOptions> extends Solver<QbfSolvingObject<W>, SOP>{

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
	protected int variablesCounter = 1;
	protected Map<String, Integer> numbersForVariables = new HashMap<>(); // map for storing keys and the corresponding value

	protected Map<Transition, Integer> transitionKeys = new HashMap<>();
	// TODO only used for construction?? reuse them?!
	protected int[][] oneTransitionFormulas; // (Transition, 1..n) -> fireTransitionID
	protected int[][] deadlockSubFormulas; // (Transition, 1..n) -> deadlockID
	protected int[][] terminatingSubFormulas; // (Transition, 1..n) -> terminatingID
	
	// StringBuilder
	private StringBuilder sb = new StringBuilder();
	
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
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
						or.add(-getVarNr(p.getId() + "." + i, true));
					}
				}
				key = createUniqueID();
				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[transitionKeys.get(t)][i] = key;
			}
		}
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
				or.add(terminatingSubFormulas[transitionKeys.get(t)][i]);  // ALTERNATIVE reuse terminatingSubFormula TODO decide for one alternative
				for (Place p : t.getPreset()) {
					//or.add(-getVarNr(p.getId() + "." + i, true)); // "p.i"  // ALTERNATIVE do not reuse terminatingSubFormula
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
	
	protected void writeDeterministic() throws IOException {
		if (QbfControl.deterministicStrategy) {
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
	
		// old alternative:
		/*int strat;
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
		}*/
		
		// new alternative: TODO why doesnt this work with true concurrent on robots_true.apt as returned strategy is not deadlock-avoiding?
		//System.out.println(getSolvingObject().getGame().getName());
		//System.out.println(deadlockSubFormulas[transitionKeys.get(t1)][i] + " " + deadlockSubFormulas[transitionKeys.get(t2)][i]);
		or.add(deadlockSubFormulas[transitionKeys.get(t1)][i]);
		or.add(deadlockSubFormulas[transitionKeys.get(t2)][i]);

		int number = createUniqueID();
		//System.out.println(number);
		writer.write(number + " = " + writeOr(or));
		return number;
	}
	
	protected void writeDeadlocksterm() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			or.clear();
			or.add(-dl[i]);
			or.add(term[i]);
			dlt[i] = createUniqueID();
			writer.write(dlt[i] + " = " + writeOr(or));
		}
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
	
	// Additional information from nondeterministic unfolding is utilized:
	// for each place a set of transitions is given of which at least one has to
	// be activated by the strategy to be deadlock-avoiding
	protected int enumerateStratForNonDetUnfold(Map<Place, Set<Transition>> additionalInfoForNonDetUnfl) throws IOException {
		if (additionalInfoForNonDetUnfl == null || additionalInfoForNonDetUnfl.keySet().isEmpty()) {
			return -1;
		}
		Set<Integer> and = new HashSet<>();
		Set<Transition> transitions;
		int or_index;
		Set<Integer> or = new HashSet<>();
		for (Place p : additionalInfoForNonDetUnfl.keySet()) {
			transitions = additionalInfoForNonDetUnfl.get(p);
			or.clear();
			for (Transition t : transitions) {
				or.add(getVarNr(p.getId() + ".." + t.getId(), true));
			}
			or_index = createUniqueID();
			writer.write(or_index + " = " + writeOr(or));
			and.add(or_index);
		}
		int index = createUniqueID();
		writer.write(index + " = " + writeAnd(and));
		return index;
	}

	// Additional information from nondeterministic unfolding is utilized:
	// for each place a set of transitions is given of which EXACTLY one has to
	// be activated by the strategy to be deadlock-avoiding
	protected int enumerateStratForNonDetUnfoldEXACTLYONE(Map<Place, Set<Transition>> additionalInfoForNonDetUnfl) throws IOException {
		if (additionalInfoForNonDetUnfl.keySet().isEmpty()) {
			return -1;
		}
		Set<Integer> outerAnd = new HashSet<>();
		int innerAnd_index;
		Set<Integer> innerAnd = new HashSet<>();
		Set<Transition> transitions;
		int or_index;
		Set<Integer> or = new HashSet<>();
		for (Place p : additionalInfoForNonDetUnfl.keySet()) {
			transitions = additionalInfoForNonDetUnfl.get(p);
			or.clear();
			for (Transition t : transitions) {
				innerAnd.clear();
				int transition_index = getVarNr(p.getId() + ".." + t.getId(), true);
				innerAnd.add(transition_index);
				for (Transition t2 : transitions) {
					int transition2_index = getVarNr(p.getId() + ".." + t2.getId(), true);
					if (t != t2) {
						innerAnd.add(-transition2_index);
					}
				}
				innerAnd_index = createUniqueID();
				writer.write(innerAnd_index + " = " + writeAnd(innerAnd));
				or.add(innerAnd_index);
			}
			or_index = createUniqueID();
			writer.write(or_index + " = " + writeOr(or));
			outerAnd.add(or_index);
		}
		int index = createUniqueID();
		writer.write(index + " = " + writeAnd(outerAnd));
		return index;
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
	
	protected void initializeBeforeUnfolding(int n) {
		fl = new int[n + 1];
		det = new int[n + 1];
		dlt = new int[n + 1];
		dl = new int[n + 1];
		term = new int[n + 1];
		seq = new int[n + 1];
		win = new int[n + 1];
		seqImpliesWin = new int[n + 1];
	}
	
	protected void initializeCaches() {
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
		//StringBuilder sb = new StringBuilder();
		sb.append(op + "(");
		String delim = ""; // first element is added without ","

		//System.out.println(input.size()); // TODO
		for (int i : input) {
			sb.append(delim);
			delim = ",";
			sb.append(i);
		}
		sb.append(")" + QbfControl.linebreak);
		String result = sb.toString();
		sb.setLength(0);
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
	
	protected abstract void writeQCIR() throws IOException;

	@Override
	protected boolean exWinStrat() {
		int exitcode = -1;
		try {
			writeQCIR();

			ProcessBuilder pb = new ProcessBuilder(AdamProperties.getInstance().getProperty(AdamProperties.QUABS), "--partial-assignment"/* , "--preprocessing", "0" */, file.getAbsolutePath());
			// Run solver on problem
			if (QbfControl.debug) {
				System.out.println("A temporary file is saved to \"" + file.getAbsolutePath() + "\".");
			}

			Process pr = pb.start();
			// Read caqe's output
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line_read;
			outputQBFsolver = "";
			while ((line_read = inputReader.readLine()) != null) {
				outputQBFsolver += line_read + "\n";
			}

			exitcode = pr.waitFor();
			inputReader.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		// Storing results
		if (exitcode == 20) {
			System.out.println("UNSAT ");
			return false;
		} else if (exitcode == 10) {
			System.out.println("SAT");
			return true;
		} else {
			System.out.println("QCIR ERROR with exitcode: " + exitcode + " and FULL output: " + outputQBFsolver);
			return false;
		}
	}

	protected Map<Place, Set<Transition>> unfoldPG() {
		originalGame = new PetriGame(getSolvingObject().getGame());
		
		Unfolder unfolder = null;
		Map<Place, Set<Transition>> result = null;
		CoverabilityGraph cover = CoverabilityGraph.get(getSolvingObject().getGame());
		try {
			TransitionSystem ts = cover.toReachabilityLTS();
			@SuppressWarnings("unchecked")
			Set<Set<State>> components = (Set<Set<State>>) Connectivity.getStronglyConnectedComponents(ts);
			int max = 1;
			for (Set<State> s : components) {
				// found loop
				if (s.size() > max) {
					max = s.size();
				}
				// check that one element component is not a direct self-loop
				if (s.size() == 1) {
					for (State state : s) {
						if (ts.getPostsetNodes(state).contains(state)) {
							max = 42;
						}
					}
				}
			}
			
			if (max <= 1) {
				QbfControl.rebuildingUnfolder = true;
			} else {
				QbfControl.rebuildingUnfolder = false;
			}
		} catch (UnboundedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO not using static rebuildingUnfolder would allow to only calculate SCC if B > 1?
		// Skip unfolding for b = 0 or b = 1:
		if (getSolvingObject().getB() > 1) {
			if (QbfControl.rebuildingUnfolder) {
				unfolder = new McMillianUnfolder(getSolvingObject(), null);
			} else {
				unfolder = new ForNonDeterministicUnfolder(getSolvingObject(), null); // null forces unfolder to use b as bound for every place
			}
			
			try {
	            unfolder.prepareUnfolding();
	        } catch (NetNotSafeException | SolvingException | UnboundedException | FileNotFoundException e1) {
	            System.out.println("Error: The bounded unfolding of the game failed.");
	            e1.printStackTrace();
	        }
			result = unfolder.systemHasToDecideForAtLeastOne;
		}
        
		unfolding = new PetriGame(getSolvingObject().getGame());
		return result;
	}
	
	protected PetriGame calculateStrategy(boolean trueConcurrent) throws NoStrategyExistentException, CalculationInterruptedException {
		if (existsWinningStrategy()) {
			for (String outputCAQE_line : outputQBFsolver.split("\n")) {
				if (outputCAQE_line.startsWith("V")) {
					String[] parts = outputCAQE_line.split(" ");
					for (int i = 0; i < parts.length; ++i) {
						if (!parts[i].equals("V")) {
							int num = Integer.parseInt(parts[i]);
							if (num > 0) {
								// System.out.println("ALLOW " + num);
							} else if (num < 0) {
								// System.out.println("DISALLOW " + num * (-1));
								String remove = exists_transitions.get(num * (-1));
								int in = remove.indexOf("..");
								if (in != -1) { // CTL has exists variables for path EF which mean not remove
									String place = remove.substring(0, in);
									String transition = remove.substring(in + 2, remove.length());
									if (place.startsWith(QbfControl.additionalSystemName)) {
										// additional system place exactly removes transitions
										// Transition might already be removed by recursion
										Set<Transition> transitions = new HashSet<>(getSolvingObject().getGame().getTransitions());
										for (Transition t : transitions) {
											if (t.getId().equals(transition)) {
												// System.out.println("starting " + t);
												getSolvingObject().removeTransitionRecursively(t);
											}
										}
									} else {
										// original system place removes ALL transitions
										Set<Place> places = new HashSet<>(getSolvingObject().getGame().getPlaces());
										for (Place p : places) {
											if (p.getId().equals(place)) {
												Set<Transition> transitions = new HashSet<>(p.getPostset());
												for (Transition post : transitions) {
													if (transition.equals(getTruncatedId(post.getId()))) {
														// System.out.println("starting " + post);
														getSolvingObject().removeTransitionRecursively(post);
													}
												}
											}
										}
									}
								}
							} else {
								// 0 is the last member
								// System.out.println("Finished reading strategy.");
								new PGSimplifier(getSolvingObject(), true, false, trueConcurrent).simplifyPG();
								strategy = new PetriGame(getSolvingObject().getGame());
								return getSolvingObject().getGame();
							}
						}
					}
				}
			}
			// There were no decision points for the system, thus the previous loop did not leave the method
			new PGSimplifier(getSolvingObject(), true, false, trueConcurrent).simplifyPG();
			strategy = new PetriGame(getSolvingObject().getGame());
			return getSolvingObject().getGame();
		}
		throw new NoStrategyExistentException();
	}
}
