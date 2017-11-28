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
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.Unfolder;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.tools.AdamProperties;

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

	protected String outputQBFsolver = "";

	// TODO maybe optional arguments
	public static String linebreak = System.lineSeparator(); // Controller
	public static String additionalSystemName = "AS___"; // Controller
	public static String additionalSystemUniqueDivider = "_0_"; // Controller
	public static String solver = "quabs"; // Controller
	public static boolean deterministicStrat = true; // Controller
	public static boolean debug = true;

	// Caches
	private Map<Transition, Set<Place>> restCache = new HashMap<>(); // proven to be slightly useful in terms of performance
	private Map<Transition, Set<Place>> preMinusPostCache = new HashMap<>();

	// working copy of the game
	public QBFPetriGame pg;
	protected PetriNet pn;

	public PetriGame game;
	public PetriGame unfolding;
	public PetriGame strategy;

	// Solving
	protected BufferedWriter writer;
	protected int variablesCounter = 2; // 1 reserved for phi
	protected Map<String, Integer> numbersForVariables = new HashMap<>(); // map for storing keys and the corresponding value

	protected int[][] oneTransitionFormulas;
	protected Map<Transition, Integer> transitionKeys = new HashMap<>();

	protected Transition[] transitions;
	protected int[] flowSubFormulas;
	protected int[] deadlockSubFormulas;
	protected int[] terminatingSubFormulas;
	protected File file = null;

	public static boolean checkStrategy (PetriNet origNet, PetriNet strat) {
		// some preparation
		for (Place p : origNet.getPlaces()) {
			AdamExtensions.setOrigID(p, Unfolder.getTruncatedId(p.getId()));
		}
		for (Place p : strat.getPlaces()) {
			AdamExtensions.setOrigID(p, Unfolder.getTruncatedId(p.getId()));
		}
		for (Transition t : strat.getTransitions()) {
			t.setLabel(Unfolder.getTruncatedId(t.getId()));
		}
		return AdamTools.checkStrategy(origNet, strat);
	}
	
	public QBFSolver(QBFPetriGame game, W winCon, QBFSolverOptions so) throws BoundedParameterMissingException {
		super(game, winCon, so);

		pg = game;
		int n = so.getN();
		int b = so.getB();
		if (n == -1) {
			if (AdamExtensions.hasBoundedParameterN(game.getNet())) {
				n = AdamExtensions.getBoundedParameterN(game.getNet());
			}
		}
		if (b == -1) {
			if (AdamExtensions.hasBoundedParameterB(game.getNet())) {
				b = AdamExtensions.getBoundedParameterB(game.getNet());
			}
		}
		if (n == -1 || b == -1) {
			throw new BoundedParameterMissingException(n, b);
		}
		pg.setN(n);
		pg.setB(b);
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

	protected String getInitial() { // TODO adapt when extension initial exists
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

	protected String[] getDeadlock() throws IOException {
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

	protected void writeDeadlockSubFormulas(int s, int e) throws IOException {
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
					if (strat != 0) {
						or.add(-strat);
					}
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

	protected String[] getTerminating() throws IOException {
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
			}
		}
		return terminating;
	}

	protected void writeTerminatingSubFormulas(int s, int e) throws IOException {
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

	protected String[] getFlow() throws IOException {
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

	protected int getOneTransition(Transition t, int i) throws IOException {
		if (oneTransitionFormulas[transitionKeys.get(t)][i] == 0) {
			Set<Integer> and = new HashSet<>();
			int strat;
			for (Place p : t.getPreset()) {
				and.add(getVarNr(p.getId() + "." + i, true));
				strat = addSysStrategy(p, t);
				if (strat != 0) {
					and.add(strat);
				}
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
			// oneTransitionFormulas[transitionKeys.get(t)][i] = number;
			return number;
		} else {
			return oneTransitionFormulas[transitionKeys.get(t)][i];
		}

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
				if (!deterministic[i].matches("")) {
					det[i] = createUniqueID();
					writer.write(det[i] + " = " + deterministic[i]);
				} else {
					det[i] = getVarNr("and()", true);
				}
			}
		}
	}

	protected String[] getDeterministic() throws IOException { // faster than naive implementation
		List<Set<Integer>> and = new ArrayList<>(pg.getN() + 1);
		and.add(null); // first element is never accessed
		for (int i = 1; i <= pg.getN(); ++i) {
			and.add(new HashSet<Integer>());
		}
		Transition t1, t2;
		Transition[] sys_transitions;
		for (Place sys : pn.getPlaces()) {
			// Additional system places are not forced to behave deterministically, this is the faster variant (especially the larger the PG becomes)
			if (!AdamExtensions.isEnvironment(sys) && !sys.getId().startsWith(QBFSolver.additionalSystemName)) {
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
			if (!and.get(i).isEmpty()) {
				deterministic[i] = writeAnd(and.get(i));
			} else {
				Pair<Boolean, Integer> result = getVarNrWithResult("and()");
				if (result.getFirst()) {
					writer.write(result.getSecond() + " = and()" + QBFSolver.linebreak);
				}
				deterministic[i] = "";
			}
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
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(additionalSystemName)) {
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
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(additionalSystemName)) {
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
		for (Place p : pn.getPlaces()) {
			if (/* !p.equals(place) && */getTruncatedId(place.getId()).equals(getTruncatedId(p.getId()))) { // forcing into different unfolded place yields more necessary unfoldings
				result.add(p);
			}
		}
		return result;
	}

	protected void writeUnfair() throws IOException {
		String unfair = getUnfair();
		u = createUniqueID();
		writer.write(u + " = " + unfair);
	}

	protected String getUnfair() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 2; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(additionalSystemName)) {
						int p_i = getVarNr(p.getId() + "." + i, true);
						int p_j = getVarNr(p.getId() + "." + j, true);
						and.add(writeImplication(p_i, p_j));
						and.add(writeImplication(p_j, p_i));
					}
				}
				Set<Integer> or = new HashSet<>();
				for (Transition t : pn.getTransitions()) {
					Set<Integer> innerAnd = new HashSet<>();
					for (int k = i; k < j; ++k) {
						for (Place p : t.getPreset()) {
							innerAnd.add(getVarNr(p.getId() + "." + k, true));
							int sysDecision = addSysStrategy(p, t);
							if (sysDecision != 0) {
								innerAnd.add(sysDecision);
							}
							if (!p.getId().startsWith(additionalSystemName)) {
								for (Transition tt : p.getPostset()) {
									innerAnd.add(-getOneTransition(tt, k));
								}
							}
						}
					}
					int innerAndNumber = createUniqueID();
					writer.write(innerAndNumber + " = " + writeAnd(innerAnd));
					or.add(innerAndNumber);
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

	// this has one more quantifier alternation, it should therefore be slower
	protected String getUnfairMoreQuantifierAlternation() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 2; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(additionalSystemName)) {
						int p_i = getVarNr(p.getId() + "." + i, true);
						int p_j = getVarNr(p.getId() + "." + j, true);
						and.add(writeImplication(p_i, p_j));
						and.add(writeImplication(p_j, p_i));
					}
				}
				Set<Integer> or = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places are not responsible for unfair loops, exclude them
					if (!p.getId().startsWith(additionalSystemName)) {
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

								// outerAnd.add(-getOneTransition(t, k)); // with necessary expensive extension, this is redundant, but makes it faster
								// TODO this makes it correct but also expensive
								for (Place pp : t.getPreset()) {
									for (Transition tt : pp.getPostset()) {
										outerAnd.add(-getOneTransition(tt, k));
									}
								}
							}
							int innerOrNumber = createUniqueID();
							writer.write(innerOrNumber + " = " + writeOr(innerOr));
							outerAnd.add(innerOrNumber);
						}
						int outerAndNumber = createUniqueID();
						writer.write(outerAndNumber + " = " + writeAnd(outerAnd));
						or.add(outerAndNumber);
					}
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

	protected int addSysStrategy(Place p, Transition t) {
		if (!AdamExtensions.isEnvironment(p)) {
			if (p.getId().startsWith(QBFSolver.additionalSystemName)) {
				return getVarNr(p.getId() + ".." + t.getId(), true);
			} else {
				return getVarNr(p.getId() + ".." + getTruncatedId(t.getId()), true);
			}
		} else {
			return 0;
		}
	}

	protected void addExists() throws IOException {
		Set<Integer> exists = new HashSet<>();
		for (Place p : pg.getNet().getPlaces()) {
			if (!AdamExtensions.isEnvironment(p)) {
				if (p.getId().startsWith(QBFSolver.additionalSystemName)) {
					for (Transition t : p.getPostset()) {
						int number = createVariable(p.getId() + ".." + t.getId());
						exists.add(number);
						//System.out.println(number + " = " + p.getId() + ".." + t.getId());
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
							//System.out.println(number + " = " + p.getId() + ".." + truncatedID);
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
			}
		}
		writer.write(writeForall(forall));
		writer.write("output(1)" + QBFSolver.linebreak); // 1 = \phi
	}

	// Additional information from nondeterministic unfolding is utilized:
	// for each place a set of transitions is given of which at least one has to
	// be activated by the strategy to be deadlock-avoiding
	protected int enumerateStratForNonDetUnfold(Map<Place, Set<Transition>> additionalInfoForNonDetUnfl) throws IOException {
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

	public int getVarNr(String id, boolean extraCheck) {
		Integer ret = numbersForVariables.get(id);
		if (ret != null) {
			return ret;
		} else if (extraCheck) {
			throw new IllegalArgumentException("Could not but should have found: " + id);
		} else {
			return createVariable(id);
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

	protected Map<Place, Set<Transition>> unfoldPG() {
		game = new PetriGame(pg);

		ForNonDeterministicUnfolder unfolder = new ForNonDeterministicUnfolder(pg, null); // null forces unfolder to use b as bound for every place
		try {
			unfolder.prepareUnfolding();
		} catch (UnboundedException | FileNotFoundException | NetNotSafeException | NoSuitableDistributionFoundException e1) {
			System.out.println("Error: The bounded unfolding of the game failed.");
		}
		// Adding the newly unfolded places to the set of bad places
		getWinningCondition().buffer(pg);

		unfolding = new PetriGame(pg);

		return unfolder.systemHasToDecideForAtLeastOne;
	}

	protected void initializeVariablesForWriteQCIR() {
		transitions = pn.getTransitions().toArray(new Transition[0]);
		flowSubFormulas = new int[pg.getN() * pn.getTransitions().size()];
		deadlockSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];
		terminatingSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];

		oneTransitionFormulas = new int[pn.getTransitions().size()][pg.getN() + 1];
		for (int i = 0; i < transitions.length; ++i) {
			transitionKeys.put(transitions[i], i);
		}
	}

	protected abstract void writeQCIR() throws IOException;

	@Override
	protected boolean exWinStrat() {
		int exitcode = -1;
		try {
			writeQCIR();

			ProcessBuilder pb = null;
			// Run solver on problem
			String os = System.getProperty("os.name");

			if (os.startsWith("Mac")) {
				// pb = new ProcessBuilder("./" + solver + "_mac", "--partial-assignment", file.getAbsolutePath());
				pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_mac", "--partial-assignment"/*, "--preprocessing", "0"*/, file.getAbsolutePath());
			} else if (os.startsWith("Linux")) {
				// pb = new ProcessBuilder("./" + solver + "_unix", "--partial-assignment", file.getAbsolutePath());
				pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_unix", "--partial-assignment", file.getAbsolutePath());
			} else {
				System.out.println("You are using " + os + ".");
				System.out.println("Your operation system is not supported.");
				return false;
			}
			if (QBFSolver.debug) {
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
			solvable = true;
			sat = false;
			error = false;
			System.out.println("UNSAT ");
			return false;
		} else if (exitcode == 10) {
			solvable = true;
			sat = true;
			error = false;
			System.out.println("SAT");
			return true;
		} else {
			System.out.println("QCIR ERROR with FULL output:" + outputQBFsolver);
			solvable = false;
			sat = null;
			error = true;
			return false;
		}
	}

	@Override
	protected PetriNet calculateStrategy() throws NoStrategyExistentException {
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
									if (place.startsWith(QBFSolver.additionalSystemName)) {
										// additional system place exactly removes transitions
										// Transition might already be removed by recursion
										Set<Transition> transitions = new HashSet<>(pg.getNet().getTransitions());
										for (Transition t : transitions) {
											if (t.getId().equals(transition)) {
												// System.out.println("starting " + t);
												pg.removeTransitionRecursively(t);
											}
										}
									} else {
										// original system place removes ALL transitions
										Set<Place> places = new HashSet<>(pg.getNet().getPlaces());
										for (Place p : places) {
											if (p.getId().equals(place)) {
												Set<Transition> transitions = new HashSet<>(p.getPostset());
												for (Transition post : transitions) {
													if (transition.equals(getTruncatedId(post.getId()))) {
														// System.out.println("starting " + post);
														pg.removeTransitionRecursively(post);
													}
												}
											}
										}
									}
								}
							} else {
								// 0 is the last member
								// System.out.println("Finished reading strategy.");
								PGSimplifier.simplifyPG(pg, true, false);
								strategy = new PetriGame(pg);
								return pg.getNet();
							}
						}
					}
				}
			}
			// There were no decision points for the system, thus the previous loop did not leave the method
			PGSimplifier.simplifyPG(pg, true, false);
			strategy = new PetriGame(pg);
			return pg.getNet();
		}
		throw new NoStrategyExistentException();
	}
}
