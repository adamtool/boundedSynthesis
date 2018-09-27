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

	protected List<Map<Integer,Integer>> enabledlist; // First setindex, then iteration index
	protected Map<Transition, Integer> transitionmap; 
	protected List<Transition> setlist; 
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
					int check_truncated = addEnvStrategy(p,t,i);
					for (Transition t_prime : post_transitions) {
						if (addEnvStrategy(p,t_prime,i) != check_truncated)
							inner_and.add(-addEnvStrategy(p, t_prime, i));
					}
					inner_and.add(check_truncated);
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
		this.transitionmap = new HashMap<>();
		this.enabledlist = new ArrayList<>();
		List<Transition> allTransitions = new ArrayList<>(getSolvingObject().getGame().getTransitions());
		while (!allTransitions.isEmpty()) {
			Transition start = allTransitions.get(allTransitions.size() - 1);
			allTransitions.remove(start);
			this.setlist.add(start);// All neighbours exclusive current
			transitionmap.put(start, setlist.size() - 1);
		}
		for (int j = 0; j < setlist.size(); j++) {
			Map<Integer, Integer> newmap = new HashMap<>();
			enabledlist.add(newmap);
		}
	}
	
	public String[] getFlow() throws IOException {
		calculateSets();
		String[] flow = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); i++) {
			and.clear();
			computeEnabledTransitions(i);
			// fire all enabled transitions
			for (Transition t : transitions) {
				Set<Integer> or = new HashSet<>();
				or.add(-enabledlist.get(transitionmap.get(t)).get(i));
				or.add(fireOneTransition(i,transitionmap.get(t)));
				int or_number = createUniqueID();
				writer.write(or_number + " = " + writeOr(or));
				and.add(or_number);
			}
			// retain all untouched places
			for (Place p : getSolvingObject().getGame().getPlaces()) {
				Set<Integer> inner_and = new HashSet<>();
				for (Transition t : p.getPostset()) {
					inner_and.add(-enabledlist.get(transitionmap.get(t)).get(i));
				}
				for (Transition t : p.getPreset()) {
					inner_and.add(-enabledlist.get(transitionmap.get(t)).get(i));
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
	 * Fires one transition.
	 * @param i
	 * @param setindex
	 * @return
	 * @throws IOException
	 */
	public int fireOneTransition(int i, int setindex) throws IOException {
		writer.write("# STARTED fire iteration " + i + " transition " + setlist.get(setindex) + "\n" );
		Transition t = setlist.get(setindex);
		int number = createUniqueID();
		Set<Integer> and = new HashSet<>();
		for (Place p : t.getPostset()) {
			and.add(getVarNr(p.getId() + "." + (i + 1), true));
		}
		Set<Place> preMinusPost = new HashSet<>(t.getPreset());
		preMinusPost.removeAll(t.getPostset());

		for (Place p : preMinusPost) {
			and.add(-getVarNr(p.getId() + "." + (i + 1), true));
		}
		writer.write(number + " = " + writeAnd(and));

		writer.write("# ENDED fire iteration " + i + " transition " + setlist.get(setindex) + "\n" );
		return number;
	}

	/**
	 * writes the enabled encodings
	 * @param i
	 * @return
	 * @throws IOException
	 */
	public void computeEnabledTransitions(int i) throws IOException {
		for (int j = 0; j < setlist.size(); j++) {
			int enabled = isEnabledSet(i, j);			
			enabledlist.get(j).put(i, enabled);
		}
	}

	/**
	 * checks if the set 'setindex' is enabled
	 * @param i
	 * @param setindex
	 * @return
	 * @throws IOException
	 */
	public int isEnabledSet(int i, int setindex) throws IOException{
		writer.write("# STARTED is enabled for iteration " + i + " and transition " + setlist.get(setindex) + "\n");
		Set<Integer> outerAnd = new HashSet<>();
		int strat;
		int outer_and_number = createUniqueID();
		for (Place p: setlist.get(setindex).getPreset()){
			outerAnd.add(getVarNr(p.getId() + "." + i, true));
		}
		Transition t = setlist.get(setindex);
		for (Place p : t.getPreset()) {
			strat = addSysStrategy(p, t);
			if (strat != 0) {
				outerAnd.add(strat);
			}
			strat = addEnvStrategy(p, t, i);
			if (strat != 0)
				outerAnd.add(strat);
		}
		
		writer.write(outer_and_number + " = " + writeAnd(outerAnd));
		
		writer.write("# ENDED is enabled for iteration " + i + " and set " + setindex + "\n");
		return outer_and_number;
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
			if (p.getId().startsWith(QbfControl.additionalSystemName)) {//TODO unnötiger Fall
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