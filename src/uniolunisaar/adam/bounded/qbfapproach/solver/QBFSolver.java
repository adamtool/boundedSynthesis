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
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.Unfolder;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 * @param <W>
 */

public abstract class QBFSolver<W extends WinningCondition> extends Solver<QBFPetriGame, W, QBFSolverOptions> {

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

	// results
	public Boolean solvable = null;
	public Boolean sat = null;
	public Boolean error = null;

	// keys
	public Map<Integer, String> exists_transitions = new HashMap<>();
	public Map<Integer, String> forall_places = new HashMap<>();

	protected String outputQBFsolver = "";

	// TODO maybe optional arguments
	public static String linebreak = "\n"; // Controller
	public static String additionalSystemName = "AS___"; // Controller
	public static String additionalSystemUniqueDivider = "_0_"; // Controller
	public static String solver = "quabs"; // Controller
	public static boolean deterministicStrat = true; // Controller

	// Caches
	private Map<Transition, Set<Place>> restCache = new HashMap<>(); // proven to be slightly useful in terms of performance
	private Map<Transition, Set<Place>> preMinusPostCache = new HashMap<>();

	// working copy of the game
	protected QBFPetriGame pg;
	protected PetriNet pn;

	public QBFPetriGame game;
	protected WinningCondition game_winCon;
	public QBFPetriGame unfolding;
	protected WinningCondition unfolding_winCon;
	public QBFPetriGame strategy;
	protected WinningCondition strategy_winCon;

	// Solving
	protected BufferedWriter writer;
	protected int variablesCounter = 2; // 1 reserved for phi
	protected Map<String, Integer> numbersForVariables = new HashMap<>(); // map for storing keys and the corresponding value

	protected Transition[] transitions;
	protected int[] flowSubFormulas;
	protected int[] deadlockSubFormulas;
	protected int[] terminatingSubFormulas;
	protected File file = null;

	public QBFSolver(QBFPetriGame game, W winCon, QBFSolverOptions so) {
		super(game, winCon, so);
		pg = game;
		pg.setN(so.getN());
		pg.setB(so.getB());
		pn = pg.getNet();

		fl = new int[pg.getN() + 1];
		det = new int[pg.getN() + 1];
		dlt = new int[pg.getN() + 1];
		dl = new int[pg.getN() + 1];
		term = new int[pg.getN() + 1];
		seq = new int[pg.getN() + 1];
		win = new int[pg.getN() + 1];
		seqImpliesWin = new int[pg.getN() + 1];

		// Create random file in tmp directory which is deleted after solving it
		String prefix = "";
		final Random rand = new Random();
		final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";
		for (int i = 0; i < 20; ++i) {
			prefix += lexicon.charAt(rand.nextInt(lexicon.length()));
		}
		try {
			file = File.createTempFile(prefix, /* pn.getName() + */ ".qcir");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		file.deleteOnExit();
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("Error: Your computer does not support \"utf-8\".");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("Error: The file created in the temp directory was already removed.");
			e.printStackTrace();
		}
	}

	protected void writeInitial() throws IOException {
		in = createUniqueID();
		writer.write(in + " = " + getInitial());
	}

	public String getInitial() {
		Marking initialMarking = pg.getNet().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : pn.getPlaces()) {
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
		for (int i = 1; i <= pg.getN(); ++i) {
			dl[i] = createUniqueID();
			writer.write(dl[i] + " = " + deadlock[i]);
		}
	}

	public String[] getDeadlock() throws IOException {
		writeDeadlockSubFormulas(1, pg.getN());
		String[] deadlock = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			and.clear();
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				and.add(deadlockSubFormulas[pn.getTransitions().size() * (i - 1) + j]);
			}
			deadlock[i] = writeAnd(and);
		}
		return deadlock;
	}

	private void writeDeadlockSubFormulas(int s, int e) throws IOException {
		Transition t;
		Set<Integer> or = new HashSet<>();
		int number;
		int strat;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				t = transitions[j];
				or.clear();
				for (Place p : t.getPreset()) {
					or.add(-getVarNr(p.getId() + "." + i, true)); // "p.i"
					strat = addSysStrategy(p, t);
					if (strat != 0)
						or.add(-strat);
				}
				number = createUniqueID();
				writer.write(number + " = " + writeOr(or));
				deadlockSubFormulas[pn.getTransitions().size() * (i - 1) + j] = number;
			}
		}
	}

	protected void writeTerminating() throws IOException {
		String[] terminating = new String[pg.getN() + 1];
		terminating = getTerminating();
		for (int i = 1; i <= pg.getN(); ++i) {
			term[i] = createUniqueID();
			writer.write(term[i] + " = " + terminating[i]);
		}
	}

	public String[] getTerminating() throws IOException {
		writeTerminatingSubFormulas(1, pg.getN());
		String[] terminating = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			if (pg.getNet().getTransitions().size() >= 1) {
				and.clear();
				for (int j = 0; j < pn.getTransitions().size(); ++j) {
					and.add(terminatingSubFormulas[pn.getTransitions().size() * (i - 1) + j]);
				}
				terminating[i] = writeAnd(and);
			} else {
				terminating[i] = "";
			}
		}
		return terminating;
	}

	private void writeTerminatingSubFormulas(int s, int e) throws IOException {
		Set<Integer> or = new HashSet<>();
		Set<Place> pre;
		Transition t;
		int key;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				t = transitions[j];
				pre = t.getPreset();
				or.clear();
				for (Place p : pre) {
					or.add(-getVarNr(p.getId() + "." + i, true));
				}
				key = createUniqueID();
				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[pn.getTransitions().size() * (i - 1) + j] = key;
			}
		}
	}

	protected void writeDeadlocksterm() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			or.clear();
			or.add(-dl[i]);
			or.add(term[i]);
			dlt[i] = createUniqueID();
			writer.write(dlt[i] + " = " + writeOr(or));
		}
	}

	protected void writeFlow() throws IOException {
		String[] flow = getFlow();
		for (int i = 1; i < pg.getN(); ++i) {
			fl[i] = createUniqueID();
			writer.write(fl[i] + " = " + flow[i]);
		}

	}

	public String[] getFlow() throws IOException {
		// writeFlowSubFormulas(); // slower
		String[] flow = new String[pg.getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			or.clear();
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				// or.add(flowSubFormulas[pn.getTransitions().size() * (i - 1) + j]);
				or.add(getOneTransition(transitions[j], i));
			}
			flow[i] = writeOr(or);
		}
		return flow;
	}

	public int getOneTransition(Transition t, int i) throws IOException {
		Set<Integer> and = new HashSet<>();
		int strat;
		for (Place p : t.getPreset()) {
			and.add(getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t);
			if (strat != 0)
				and.add(strat);
		}
		for (Place p : t.getPostset()) {
			and.add(getVarNr(p.getId() + "." + (i + 1), true));
		}
		// Slight performance gain by using these caches
		Set<Place> rest = restCache.get(t);
		if (rest == null) {
			rest = new HashSet<>(pg.getNet().getPlaces());
			rest.removeAll(t.getPreset());
			rest.removeAll(t.getPostset());
			restCache.put(t, rest);
		}

		for (Place p : rest) {
			int p_i = getVarNr(p.getId() + "." + i, true);
			int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
			and.add(writeImplication(p_i, p_iSucc));
			and.add(writeImplication(p_iSucc, p_i));
		}

		Set<Place> preMinusPost = preMinusPostCache.get(t);
		if (preMinusPost == null) {
			preMinusPost = new HashSet<>(t.getPreset());
			preMinusPost.removeAll(t.getPostset());
			preMinusPostCache.put(t, preMinusPost);
		}
		for (Place p : preMinusPost) {
			and.add(-getVarNr(p.getId() + "." + (i + 1), true));
		}
		int number = createUniqueID();
		writer.write(number + " = " + writeAnd(and));
		return number;
	}

	protected void writeSequence() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			and.clear();
			and.add(in);
			for (int j = 1; j <= i - 1; ++j) {
				// and.add(-dl[j]); // performance evaluation showed that leaving this out makes program faster as it is redundant
				and.add(fl[j]);
			}
			seq[i] = createUniqueID();
			writer.write(seq[i] + " = " + writeAnd(and));
		}
	}

	protected void writeDeterministic() throws IOException {
		if (deterministicStrat) {
			String[] deterministic = getDeterministic();
			for (int i = 1; i <= pg.getN(); ++i) {
				det[i] = createUniqueID();
				writer.write(det[i] + " = " + deterministic[i]);
			}
		}
	}

	public String[] getDeterministic() throws IOException { // faster than naive implementation
		List<Set<Integer>> and = new ArrayList<>(pg.getN() + 1);
		and.add(null); // first element is never accessed
		for (int i = 1; i <= pg.getN(); ++i) {
			and.add(new HashSet<Integer>());
		}
		Transition t1, t2;
		Transition[] sys_transitions;
		for (Place sys : pn.getPlaces()) {
			// Additional system places are not forced to behave deterministically, this is the faster variant (especially the larger the PG becomes)
			if (!pg.getEnvPlaces().contains(sys) && !sys.getId().startsWith(QBFSolver.additionalSystemName)) {
				if (sys.getPostset().size() > 1) {
					sys_transitions = sys.getPostset().toArray(new Transition[0]);
					for (int j = 0; j < sys_transitions.length; ++j) {
						t1 = sys_transitions[j];
						for (int k = j + 1; k < sys_transitions.length; ++k) {
							t2 = sys_transitions[k];
							for (int i = 1; i <= pg.getN(); ++i) {
								and.get(i).add(writeOneMissingPre(t1, t2, i));
							}
						}
					}
				}
			}
		}
		String[] deterministic = new String[pg.getN() + 1];
		for (int i = 1; i <= pg.getN(); ++i) {
			deterministic[i] = writeAnd(and.get(i));
		}
		return deterministic;
	}

	private int writeOneMissingPre(Transition t1, Transition t2, int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		int strat;
		for (Place p : t1.getPreset()) {
			or.add(-getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t1);
			if (strat != 0)
				or.add(-strat);
		}
		for (Place p : t2.getPreset()) {
			or.add(-getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t2);
			if (strat != 0)
				or.add(-strat);
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

	public String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					int p_i = getVarNr(p.getId() + "." + i, true);
					int p_j = getVarNr(p.getId() + "." + j, true);
					and.add(writeImplication(p_i, p_j));
					and.add(writeImplication(p_j, p_i));
				}
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
		}
		return writeOr(or);
	}

	// wahrscheinlich nur hilfreich für deterministic unfolding, macht aber auf jeden fall nichts kaputt, wohl nur langsamer
	public String getLoopIJunfolded() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
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
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				outerOr.add(andNumber);
			}
		}
		return writeOr(outerOr);
	}

	protected void writeUnfair() throws IOException {
		String unfair = getUnfair();
		u = createUniqueID();
		writer.write(u + " = " + unfair);
	}

	public String getUnfair() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 2; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					int from = getVarNr(p.getId() + "." + i, true);
					int to = getVarNr(p.getId() + "." + j, true);
					and.add(writeImplication(from, to));
					and.add(writeImplication(to, from));
				}
				Set<Integer> or = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					Set<Integer> outerAnd = new HashSet<>();
					for (int k = i; k < j; ++k) {
						outerAnd.add(getVarNr(p.getId() + "." + k, true));
						Set<Integer> innerOr = new HashSet<>();
						for (Transition t : p.getPostset()) {
							Set<Integer> innerAnd = new HashSet<>();
							for (Place place : t.getPreset()) {
								innerAnd.add(getVarNr(place.getId() + "." + k, true));
								int strat = addSysStrategy(place, t);
								if (strat != 0) {
									innerAnd.add(strat);
								}
							}
							int innerAndNumber = createUniqueID();
							writer.write(innerAndNumber + " = " + writeAnd(innerAnd));
							innerOr.add(innerAndNumber);

							outerAnd.add(-getOneTransition(t, k));
						}
						int innerOrNumber = createUniqueID();
						writer.write(innerOrNumber + " = " + writeOr(innerOr));
						outerAnd.add(innerOrNumber);
					}
					int outerAndNumber = createUniqueID();
					writer.write(outerAndNumber + " = " + writeAnd(outerAnd));
					or.add(outerAndNumber);
				}
				int orNumber = createUniqueID();
				writer.write(orNumber + " = " + writeOr(or));
				and.add(orNumber);
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				outerOr.add(andNumber);
			}
		}
		return writeOr(outerOr);
	}

	private Set<Place> unfoldingsOf(Place place) {
		Set<Place> result = new HashSet<>();
		for (Place p : pn.getPlaces()) {
			if (/* !p.equals(place) && */ getTruncatedId(place.getId()).equals(getTruncatedId(p.getId()))) { // forcing into different unfolded place yields more necessary unfoldings
				result.add(p);
			}
		}
		return result;
	}

	public int addSysStrategy(Place p, Transition t) {
		if (!pg.getEnvPlaces().contains(p)) {
			if (p.getId().startsWith(QBFSolver.additionalSystemName)) {
				return getVarNr(p.getId() + ".." + t.getId(), true);
			} else {
				return getVarNr(p.getId() + ".." + getTruncatedId(t.getId()), true);
			}
		} else {
			return 0;
		}
	}

	public void addExists() throws IOException {
		Set<Integer> exists = new HashSet<>();
		for (Place p : pg.getNet().getPlaces()) {
			if (!pg.getEnvPlaces().contains(p)) {
				if (p.getId().startsWith(QBFSolver.additionalSystemName)) {
					for (Transition t : p.getPostset()) {
						int number = createVariable(p.getId() + ".." + t.getId());
						exists.add(number);
						// System.out.println(number + " = " + p.getId() + ".." + t.getId());
						exists_transitions.put(number, p.getId() + ".." + t.getId());
					}
				} else {
					Set<String> truncatedIDs = new HashSet<>();
					for (Transition t : p.getPostset()) {
						String truncatedID = getTruncatedId(t.getId());
						if (!truncatedIDs.contains(truncatedID)) {
							truncatedIDs.add(truncatedID);
							int number = createVariable(p.getId() + ".." + truncatedID);
							exists.add(number);
							// System.out.println(number + " = " + p.getId() + ".." + truncatedID);
							exists_transitions.put(number, p.getId() + ".." + truncatedID);
						}
					}
				}
			}
		}
		writer.write(writeExists(exists));
	}

	protected void addForall() throws IOException {
		Set<Integer> forall = new HashSet<>();
		for (Place p : pg.getNet().getPlaces()) {
			for (int i = 1; i <= pg.getN(); ++i) {
				int number = createVariable(p.getId() + "." + i);
				forall.add(number);
				// System.out.println(number + " = " + p.getId() + "." + i);
				forall_places.put(number, p.getId() /* + "." + i */);
			}
		}
		writer.write(writeForall(forall));
	}

	// Additional information from nondeterministic unfolding is utilized:
	// for each place a set of transitions is given of which at least one has to
	// be activated by the strategy to be deadlock-avoiding
	public int enumerateStratForNonDetUnfold(Map<Place, Set<Transition>> additionalInfoForNonDetUnfl) throws IOException {
		if (additionalInfoForNonDetUnfl.keySet().isEmpty()) {
			return -1;
		}
		Set<Integer> and = new HashSet<>();
		Set<Transition> transitions;
		int or_index;
		Set<Integer> or = new HashSet<>();
		for (Place p : additionalInfoForNonDetUnfl.keySet()) {
			// if (endsWithEnvPlace(p)) { // TODO is this right?
			transitions = additionalInfoForNonDetUnfl.get(p);
			or.clear();
			for (Transition t : transitions) {
				or.add(getVarNr(p.getId() + ".." + t.getId(), true));
			}
			or_index = createUniqueID();
			writer.write(or_index + " = " + writeOr(or));
			and.add(or_index);
			// }
		}
		int index = createUniqueID();
		writer.write(index + " = " + writeAnd(and));
		return index;
	}

	// Additional information from nondeterministic unfolding is utilized:
	// for each place a set of transitions is given of which EXACTLY one has to
	// be activated by the strategy to be deadlock-avoiding
	public int enumerateStratForNonDetUnfoldEXACTLYONE(Map<Place, Set<Transition>> additionalInfoForNonDetUnfl) throws IOException {
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
				;
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

	private boolean endsWithEnvPlace(Place p) {
		String p_id = p.getId();
		String[] split = p_id.split("--");
		return pg.getEnvPlaces().contains(pn.getPlace(split[split.length - 1]));
	}

	public int getVarNr(String id, boolean extraCheck) {
		Integer ret = numbersForVariables.get(id);
		if (ret != null) {
			return ret;
		} else {
			if (extraCheck) {
				throw new IllegalArgumentException("Could not but should have found: " + id);
			} else {
				return createVariable(id);
			}
		}
	}

	public Pair<Boolean, Integer> getVarNrWithResult(String id) {
		Integer ret = numbersForVariables.get(id);
		if (ret != null) {
			return new Pair<>(false, ret);
		} else {
			return new Pair<>(true, createVariable(id));
		}
	}

	public int createVariable(String id) {
		numbersForVariables.put(id, variablesCounter);
		return variablesCounter++;
	}

	public int createUniqueID() {
		return variablesCounter++;
	}

	public String getTruncatedId(String id) {
		int index = id.indexOf("__");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}

	// WRITERS:
	public String writeExists(Set<Integer> input) {
		return writeString("exists", input);
	}

	public String writeForall(Set<Integer> input) {
		return writeString("forall", input);
	}

	public String writeOr(Set<Integer> input) {
		return writeString("or", input);
	}

	public String writeAnd(Set<Integer> input) {
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
		sb.append(")" + QBFSolver.linebreak);
		String result = sb.toString();
		return result;
	}

	public int writeImplication(int from, int to) throws IOException {
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
