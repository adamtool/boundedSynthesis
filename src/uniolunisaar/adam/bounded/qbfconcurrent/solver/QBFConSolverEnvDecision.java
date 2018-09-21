package uniolunisaar.adam.bounded.qbfconcurrent.solver;

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
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfControl;
import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

/**
 * 
 * @author Niklas Metzger
 *
 */

public abstract class QBFConSolverEnvDecision<W extends WinningCondition> extends QBFConSolver<W> {

	// Caches
	private Map<Transition, Set<Place>> restCache = new HashMap<>();
	private Map<Transition, Set<Place>> preMinusPostCache = new HashMap<>();

	// steps of solving
	public QBFSolvingObject<W> originalSolvingObject;
	public PetriGame originalGame;
	public PetriGame unfolding;
	public PetriGame strategy;

	protected BufferedWriter writer;
	protected int variablesCounter = 2; // 1 reserved for phi
	protected Map<String, Integer> numbersForVariables = new HashMap<>();

	protected Transition[] transitions;
	protected int[] flowSubFormulas;
	protected int[] deadlockSubFormulas;
	protected int[] terminatingSubFormulas;
	protected File file = null;
	protected int numTransitions;

	protected List<Map<Integer, Integer>> enabledlist; // First setindex, then iteration index
	protected Map<Transition, Integer> transitionmap;
	protected List<Set<Place>> postlist; // Only direct neighbours
	protected List<Set<Place>> prelist; // As in true concurrent case without linear env decision
	protected List<Set<Transition>> setlist; // TODO change this list to "new" cp sets

	protected QBFConSolverEnvDecision(PetriGame game, W winCon, QBFConSolverOptions so) throws SolvingException {
		super(game, winCon, so);
		getSolvingObject().setN(so.getN());
		getSolvingObject().setB(so.getB());
		transitions = new Transition[getSolvingObject().getGame().getTransitions().size()];
		int counter = 0;
		numTransitions = getSolvingObject().getGame().getTransitions().size();
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			transitions[counter++] = t;
		}

		// Create random file in tmp directory which is deleted after solving it
		String prefix = "";
		final Random rand = new Random();
		final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";
		for (int i = 0; i < 20; ++i) {
			prefix += lexicon.charAt(rand.nextInt(lexicon.length()));
		}
		try {
			file = File.createTempFile(prefix, ".txt");
		} catch (IOException e) {
			System.out.println("Error: File creation in temp directory caused an error.");
			e.printStackTrace();
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

	/**
	 * This method implements the encoding that satisfies the determinism of the
	 * Environment strategy.
	 * 
	 * @return A string representing the deterministic strategy encoding
	 * @throws IOException
	 */
	public String getDetEnv() throws IOException {
		Set<Integer> outer_and = new HashSet<>();
		Set<Transition> post_transitions = new HashSet<>();
		Set<Integer> inner_or = new HashSet<>();
		Set<Integer> inner_and = new HashSet<>();
		writer.write("#Start det env\n");
		for (Place p : getSolvingObject().getGame().getEnvPlaces()) {
			for (int i = 0; i <= getSolvingObject().getN(); i++) {
				for (Transition t : p.getPostset()) {
					post_transitions.addAll(p.getPostset());
					post_transitions.remove(t);
					for (Transition t_prime : post_transitions) {
						inner_and.add(-addEnvStrategy(p, t_prime, i));
					}
					inner_and.add(addEnvStrategy(p, t, i));
					int inner_and_var = createUniqueID();
					writer.write("# START detenv for transition: " + t + " iteration: " + i + "\n");
					writer.write(inner_and_var + " = " + writeAnd(inner_and));
					writer.write("# END detenv for transition: " + t + " iteration: " + i+ "\n");
					inner_or.add(inner_and_var);
					inner_and.clear();
					post_transitions.clear();
				}
				int inner_or_var = createUniqueID();
				writer.write(inner_or_var + " = " + writeOr(inner_or));
				outer_and.add(inner_or_var);
				inner_or.clear();
			}
		}
		writer.write("#last line of det env\n");
		return writeAnd(outer_and);
	}

	public String getInitial() {
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

	public String[] getDeadlock() throws IOException {
		writeDeadlockSubFormulas(1, getSolvingObject().getN());
		String[] deadlock = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.clear();
			for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
				and.add(deadlockSubFormulas[getSolvingObject().getGame().getTransitions().size() * (i - 1) + j]);
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
			for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
				t = transitions[j];
				number = createUniqueID();
				or.clear();
				for (Place p : t.getPreset()) {
					or.add(-getVarNr(p.getId() + "." + i, true)); // "p.i"
					strat = addSysStrategy(p, t);
					if (strat != 0)
						or.add(-strat);
				}
				writer.write(number + " = " + writeOr(or));
				deadlockSubFormulas[getSolvingObject().getGame().getTransitions().size() * (i - 1) + j] = number;
			}
		}
	}

	/**
	 * This method calculates sets that are used while computing which are the basis
	 * of the true concurrent flow. *NEW* only neighbours are computed, no recursive
	 * sets
	 */
	public void calculateSets() {
		this.setlist = new ArrayList<>();
		this.postlist = new ArrayList<>();
		this.prelist = new ArrayList<>();
		this.transitionmap = new HashMap<>();
		this.enabledlist = new ArrayList<>();
		List<Transition> allTransitions = new ArrayList<>(getSolvingObject().getGame().getTransitions());

		while (!allTransitions.isEmpty()) {
			Transition start = allTransitions.get(allTransitions.size() - 1);
			allTransitions.remove(start);
			Set<Transition> newSet = new HashSet<>();
			newSet.add(start);
			Set<Place> preset = new HashSet<>(start.getPreset());
			for (Place p : preset) {
				newSet.addAll(p.getPostset());
			}
			newSet.remove(start);
			this.setlist.add(newSet);// All neighbours exclusive current transition
			this.prelist.add(preset);
			Set<Place> postset = new HashSet<>();
			for (Transition t : newSet)
				postset.addAll(t.getPostset());
			this.postlist.add(postset);
			transitionmap.put(start, setlist.size() - 1);
		}
		for (int j = 0; j < setlist.size(); j++) {
			Map<Integer, Integer> newmap = new HashMap<>();
			enabledlist.add(newmap);
		}
		for (Set<Transition> s : setlist) {
			System.out.println(s);

		}
		for (Set<Place> p : prelist) {
			System.out.println(p);
		}
	}
	
	// TODO only for debugging purposes
	public int getEnabled (Transition t, int i) throws IOException {
		Set<Integer> and = new HashSet<>();
		for (Place pre : t.getPreset()) {
			and.add(getVarNr(pre.getId() + "." + i, true));
			if (getSolvingObject().getGame().getEnvPlaces().contains(pre)) {
				// ENV place
				and.add(getVarNr(pre.getId() + "**" + getTruncatedId(t.getId()) + "**" + i, true));
			} else {
				// SYS place
				and.add(getVarNr(pre.getId() + ".." + getTruncatedId(t.getId()), true));
			}
		}
		int and_number = createUniqueID();
		writer.write(and_number + " = " + writeAnd(and));
		return and_number;
	}
	
	// TODO only for debugging purposes
	// Jesko's working version
	public String[] getFlow() throws IOException {
		String[] flow = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); i++) {
			and.clear();
			// fire all enabled transitions
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				Set<Integer> or = new HashSet<>();
				for (Place pre : t.getPreset()) {
					or.add(-getVarNr(pre.getId() + "." + i, true));
					if (getSolvingObject().getGame().getEnvPlaces().contains(pre)) {
						// ENV place
						or.add(-getVarNr(pre.getId() + "**" + getTruncatedId(t.getId()) + "**" + i, true));
					} else {
						// SYS place
						or.add(-getVarNr(pre.getId() + ".." + getTruncatedId(t.getId()), true));
					}
				}
				or.add(getOnlyOneTransition(t, i));
				int or_number = createUniqueID();
				writer.write(or_number + " = " + writeOr(or));
				and.add(or_number);
			}
			// retain all untouched places
			for (Place p : getSolvingObject().getGame().getPlaces()) {
				Set<Integer> inner_and = new HashSet<>();
				for (Transition t : p.getPostset()) {
					inner_and.add(-getEnabled(t, i));
				}
				for (Transition t : p.getPreset()) {
					inner_and.add(-getEnabled(t, i));
				}
				int inner_and_number = createUniqueID();
				writer.write(inner_and_number + " = " + writeAnd(inner_and));
				
				int impl = writeImplication(getVarNr(p.getId() + "." + i, true), getVarNr(p.getId() + "." + (i + 1), true));
				int retu = writeImplication(getVarNr(p.getId() + "." + (i + 1), true), getVarNr(p.getId() + "." + i, true));
				Set<Integer> iff = new HashSet<>();
				iff.add(impl);
				iff.add(retu);
				int iff_number = createUniqueID();
				writer.write(iff_number + " = " + writeAnd(iff));
				
				and.add(writeImplication(inner_and_number, iff_number));
			}
			flow[i] = writeAnd(and);
		}
		return flow;
	}

	/**
	 * Get flow encodes the flow. No need for fallthrough needed (hopefully)
	 * 
	 * @return
	 * @throws IOException
	 */
	public String[] getFlowTRUECONCURRENT() throws IOException {
		calculateSets();
		String[] flow = new String[getSolvingObject().getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); i++) { //leq or less??

			or.clear();
			int someEnabled = oneSetEnabled(i);
			Set<Integer> fireDecision = new HashSet<>();
			// fireDecision.add(someEnabled); // probabily need this for termination?
			fireDecision.add(decision(i));
			int number_decision = createUniqueID();
			writer.write(number_decision + " = " + writeAnd(fireDecision));
			// int number_Fire_One = createUniqueID();
			// writer.write(number_Fire_One + " = " + writeAnd(fireOne));
			or.add(number_decision);
			flow[i] = writeOr(or);
		}
		return flow;
	}
	
	
	// TODO only for debugging purposes
	public String[] getFlowSEQUENTIAL() throws IOException {
		String[] flow = new String[getSolvingObject().getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			or.clear();
			for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
				// or.add(flowSubFormulas[pn.getTransitions().size() * (i - 1) + j]);
				or.add(getOneTransition(transitions[j], i));
			}
			flow[i] = writeOr(or);
		}
		return flow;
	}
	
	// TODO only for debugging purposes
	protected int getOneTransition(Transition t, int i) throws IOException {
			Set<Integer> and = new HashSet<>();
			int strat;
			for (Place p : t.getPreset()) {
				and.add(getVarNr(p.getId() + "." + i, true));
				strat = addSysStrategy(p, t);
				if (strat != 0) {
					and.add(strat);
				}
				if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
					and.add(getVarNr(p.getId() + "**" + t.getId() + "**" + i, true));
				}
			}
			for (Place p : t.getPostset()) {
				and.add(getVarNr(p.getId() + "." + (i + 1), true));
			}
			// Slight performance gain by using these caches
			Set<Place> rest = restCache.get(t);
			if (rest == null) {
				rest = new HashSet<>(getSolvingObject().getGame().getPlaces());
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

	/**
	 * Implements the Decision part of the formula.
	 * 
	 * @param i
	 * @return
	 * @throws IOException
	 */
	public int decision(int i) throws IOException {
		Set<Integer> outer_and = new HashSet<>();
		for (int j = 0; j < setlist.size(); j++) {
			Set<Integer> inner_or = new HashSet<>();
			Set<Integer> inner_and_fire = new HashSet<>();
			Set<Integer> inner_and_dont_fire = new HashSet<>();

			int is_enabled = enabledlist.get(j).get(i);
			inner_and_fire.add(is_enabled);
			inner_and_dont_fire.add(-is_enabled);
			inner_and_fire.add(fireOneTransitionSet(i, j));
			inner_and_dont_fire.add(dontFireOneTransitionSet(i, j));

			int number_fire = createUniqueID();
			int number_dont_fire = createUniqueID();
			writer.write(number_fire + " = " + writeAnd(inner_and_fire));
			writer.write(number_dont_fire + " = " + writeAnd(inner_and_dont_fire));
			inner_or.add(number_fire);
			inner_or.add(number_dont_fire);
			int number_inner_or = createUniqueID();
			writer.write(number_inner_or + " = " + writeOr(inner_or));
			outer_and.add(number_inner_or);
		}
		int number = createUniqueID();
		writer.write(number + " = " + writeAnd(outer_and));
		return number;
	}

	/**
	 * Fires one transition sequentially.
	 * 
	 * @param i
	 * @return
	 * @throws IOException
	 */
	public int fireOne(int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
			or.add(getOnlyOneTransition(transitions[j], i));
		}
		int number = createUniqueID();
		writer.write(number + " = " + writeOr(or));
		return number;
	}

	/**
	 * Fires a cp-set
	 * 
	 * @param i
	 * @param setindex
	 * @return
	 * @throws IOException
	 */
	public int fireOneTransitionSet(int i, int setindex) throws IOException {
		Set<Integer> outer_or = new HashSet<>();
		for (Transition t : setlist.get(setindex)) {
			int number = createUniqueID();
			Set<Integer> and = new HashSet<>();
			int strat;
			for (Place p : t.getPreset()) {
				// and.add(getVarNr(p.getId() + "." + i, true));
				strat = addSysStrategy(p, t);
				if (strat != 0)
					and.add(strat);
				strat = addEnvStrategy(p, t, i);
				if (strat != 0)
					and.add(strat); // move this to enabled???
			}
			for (Place p : t.getPostset()) {
				and.add(getVarNr(p.getId() + "." + (i + 1), true));
			}
			Set<Place> rest = new HashSet<>(prelist.get(setindex));
			rest.addAll(postlist.get(setindex));
			rest.removeAll(t.getPreset());
			rest.removeAll(t.getPostset());

			for (Place p : rest) {
				int p_i = getVarNr(p.getId() + "." + i, true);
				int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
				and.add(writeImplication(p_i, p_iSucc));
				and.add(writeImplication(p_iSucc, p_i));
			}

			Set<Place> preMinusPost = new HashSet<>(t.getPreset());
			preMinusPost.removeAll(t.getPostset());

			for (Place p : preMinusPost) {
				and.add(-getVarNr(p.getId() + "." + (i + 1), true));
			}
			writer.write(number + " = " + writeAnd(and));
			outer_or.add(number);
		}
		int number_outer = createUniqueID();
		writer.write(number_outer + " = " + writeOr(outer_or));
		Set<Integer> outer_and = new HashSet<>();
		outer_and.add(number_outer);
		outer_and.add(enabledlist.get(setindex).get(i));
		int number_outer_and = createUniqueID();
		writer.write(number_outer_and + " = " + writeAnd(outer_and));
		return number_outer_and;
	}

	/**
	 * Implements dont_fire of the encoding.
	 * 
	 * @param i
	 * @param setindex
	 * @return
	 * @throws IOException
	 */
	public int dontFireOneTransitionSet(int i, int setindex) throws IOException {
		Set<Integer> outer_and = new HashSet<>();
		for (Place p : prelist.get(setindex)) {
			int p_i = (getVarNr(p.getId() + "." + i, true));
			int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
			outer_and.add(writeImplication(p_i, p_iSucc));
			if (p.getPreset().isEmpty())
				outer_and.add(writeImplication(p_iSucc, p_i));
		}
		for (Place p : postlist.get(setindex)) {
			Set<Integer> inner_or = new HashSet<>();
			int p_i = (getVarNr(p.getId() + "." + i, true));
			int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
			inner_or.add(writeImplication(-p_i, -p_iSucc));
			for (Transition t : p.getPreset()) {
				if (!setlist.get(setindex).contains(t)) {
					inner_or.add(enabledlist.get(transitionmap.get(t)).get(i));
				}
			}
			int number_or = createUniqueID();
			writer.write(number_or + " = " + writeOr(inner_or));
			outer_and.add(number_or);
		}
		int number = createUniqueID();
		writer.write(number + " = " + writeAnd(outer_and));
		return number;

	}

	/**
	 * Encodes the existence of an enbled set.
	 * 
	 * @param i
	 * @return
	 * @throws IOException
	 */
	public int oneSetEnabled(int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int j = 0; j < setlist.size(); j++) {
			int enabled = isEnabledSet(i, j);
			or.add(enabled);

			enabledlist.get(j).put(i, enabled);
		}
		int number = createUniqueID();
		writer.write(number + " = " + writeOr(or));
		return number;
	}

	/**
	 * checks if the set 'setindex' is enabled
	 * 
	 * @param i
	 * @param setindex
	 * @return
	 * @throws IOException
	 */
	public int isEnabledSet(int i, int setindex) throws IOException {
		Set<Integer> outerAnd = new HashSet<>();
		Set<Integer> innerOr = new HashSet<>();
		Set<Integer> env_or = new HashSet<>();
		int strat;
		for (Place p : prelist.get(setindex)) {
			int p_number = getVarNr(p.getId() + "." + i, true);
			outerAnd.add(getVarNr(p.getId() + "." + i, true));
		}
		for (Transition t : setlist.get(setindex)) {
			Set<Integer> inner_and = new HashSet<>();
			for (Place p : t.getPreset()) {
				strat = addSysStrategy(p, t);
				if (strat != 0) {
					inner_and.add(strat);
				}
			}
			if (!inner_and.isEmpty()) {
				int inner_and_number = createUniqueID();
				writer.write(inner_and_number + " = " + writeAnd(inner_and));
				innerOr.add(inner_and_number);
			}
		}
		if (!innerOr.isEmpty()) {
			int inner_or_number = createUniqueID();
			writer.write(inner_or_number + " = " + writeOr(innerOr));
			outerAnd.add(inner_or_number);
		}
		int outer_and_number = createUniqueID();
		writer.write(outer_and_number + " = " + writeAnd(outerAnd));
		return outer_and_number;
	}

	public int getOnlyOneTransition(Transition t, int i) throws IOException {
		int number = createUniqueID();
		Set<Integer> and = new HashSet<>();
		int strat;
		int envStrat;
		for (Place p : t.getPreset()) {
			and.add(getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t);
			if (strat != 0)
				and.add(strat);
			envStrat = addEnvStrategy(p, t, i);
			if (envStrat != 0)
				and.add(envStrat);
		}
		for (Place p : t.getPostset()) {
			and.add(getVarNr(p.getId() + "." + (i + 1), true));
		}
		Set<Place> rest = restCache.get(t);
		if (rest == null) {
			rest = new HashSet<>(getSolvingObject().getGame().getPlaces());
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
		writer.write(number + " = " + writeAnd(and));
		return number;
	}

	public String[] getTerminating() throws IOException {
		writeTerminatingSubFormulas(1, getSolvingObject().getN());
		String[] terminating = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			if (getSolvingObject().getGame().getTransitions().size() >= 1) {
				and.clear();
				for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
					and.add(terminatingSubFormulas[getSolvingObject().getGame().getTransitions().size() * (i - 1) + j]);
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
			for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
				t = transitions[j];
				key = createUniqueID();
				pre = t.getPreset();
				or.clear();
				for (Place p : pre) {
					or.add(-getVarNr(p.getId() + "." + i, true));
				}

				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[getSolvingObject().getGame().getTransitions().size() * (i - 1) + j] = key;
			}
		}
	}

	public String[] getDeterministic() throws IOException { // faster than naive
															// implementation
		List<Set<Integer>> and = new ArrayList<>(getSolvingObject().getN() + 1);
		and.add(null); // first element is never accessed
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.add(new HashSet<Integer>());
		}
		Transition t1, t2;
		Transition[] sys_transitions;
		for (Place sys : getSolvingObject().getGame().getPlaces()) {
			// Additional system places are not forced to behave
			// deterministically, this is the faster variant (especially the
			// larger the PG becomes)
			if (!getSolvingObject().getGame().getEnvPlaces().contains(sys)
					&& !sys.getId().startsWith(QbfControl.additionalSystemName)) {
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

	public String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (int j = i + 2; j <= getSolvingObject().getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					int p_i = getVarNr(p.getId() + "." + i, true);
					int p_j = getVarNr(p.getId() + "." + j, true);
					and.add(writeImplication(p_i, p_j));
					and.add(writeImplication(p_j, p_i));
				}
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
			if (i == getSolvingObject().getN() - 1) {
				int j = getSolvingObject().getN();
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
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

	public int addSysStrategy(Place p, Transition t) {
		if (!getSolvingObject().getGame().getEnvPlaces().contains(p)) {
			if (p.getId().startsWith(QbfControl.additionalSystemName)) {
				return getVarNr(p.getId() + ".." + t.getId(), true);
			} else {
				return getVarNr(p.getId() + ".." + getTruncatedId(t.getId()), true);
			}
		} else {
			return 0;
		}
	}

	public int addEnvStrategy(Place p, Transition t, int i) {
		if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
			if (p.getId().startsWith(QbfControl.additionalSystemName)) {
				return getVarNr(p.getId() + "**" + t.getId() + "**" + i, true);
			} else {
				return getVarNr(p.getId() + "**" + getTruncatedId(t.getId()) + "**" + i, true);
			}
		} else {
			return 0;
		}
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
		sb.append(")" + QbfControl.linebreak);
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