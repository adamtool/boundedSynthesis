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
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

/**
 * 
 * @author niklasmetzger
 *
 *         Note: Lots of code duplication at this point, not a good way. Maybe
 *         get a solution with a doubly superclass
 *
 * @param <W>
 */

public abstract class QBFConSolver<W extends WinningCondition> extends Solver<QBFSolvingObject<W>, QBFConSolverOptions> {
	// TODO maybe optional arguments
	public static String linebreak = "\n\n"; // Controller
	public static String additionalSystemName = "AS___"; // Controller
	public static String solver = "quabs"; // Controller
	public static boolean deterministicStrat = true; // Controller

	// Caches
	private Map<Transition, Set<Place>> restCache = new HashMap<>();	
	private Map<Transition, Set<Place>> preMinusPostCache = new HashMap<>();

	protected QBFSolvingObject<? extends WinningCondition> pg;
	protected PetriNet pn;
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
	protected List<Set<Place>> postlist;
	protected List<Set<Place>> prelist;
	protected List<Set<Transition>> setlist;

	protected QBFConSolver(PetriGame game, W winCon, QBFConSolverOptions so) throws BoundedParameterMissingException {
		super(new QBFSolvingObject<>(game, winCon), so);
		pg = getSolvingObject();
		 int n = so.getN();
	        int b = so.getB();
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
	        pg.setN(n);
	        pg.setB(b);
		pn = pg.getGame();
		transitions = new Transition[pn.getTransitions().size()];
		int counter = 0;
		numTransitions = pn.getTransitions().size();
		for (Transition t : pn.getTransitions()) {
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

	public String getInitial() {
		Marking initialMarking = pg.getGame().getInitialMarking();
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
		writer.write("# Deadlock SubFOrmulas are written here!\n");
		Transition t;
		Set<Integer> or = new HashSet<>();
		int number;
		int strat;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
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
				deadlockSubFormulas[pn.getTransitions().size() * (i - 1) + j] = number;
			}
		}
		writer.write("# Deadlock SubFOrmulas are written here!   END\n");
	}

	public void calculateSets(){
		this.setlist = new ArrayList<>();
		this.postlist = new ArrayList<>();
		this.prelist = new ArrayList<>();
		this.transitionmap = new HashMap<>();
		this.enabledlist = new ArrayList<>();
		List<Transition> allTransitions = new ArrayList<>(pn.getTransitions());
		
		while (!allTransitions.isEmpty()){
			Transition start = allTransitions.get(allTransitions.size()-1);
			allTransitions.remove(start);
			Set<Transition> newSet = new HashSet<>();
			newSet.add(start);
			Set<Place> visited = new HashSet<>();
			Set<Place> preset = new HashSet<>(start.getPreset());
			Set<Place> newpreset = new HashSet<>();
			while (!preset.isEmpty()){
				newpreset.clear();
				for (Place p : preset){
					visited.add(p);
					for (Transition t : p.getPostset()){
						allTransitions.remove(t);
						newSet.add(t);
						newpreset.addAll(t.getPreset());
					}
				}
				newpreset.removeAll(visited);
				preset = new HashSet<>(newpreset);
			}
			this.setlist.add(newSet);
			this.prelist.add(visited);
			Set<Place> postset = new HashSet<>();
			for (Transition t: newSet)
				postset.addAll(t.getPostset());
			this.postlist.add(postset);
			
			for (Transition t: setlist.get(setlist.size()-1)){
				transitionmap.put(t, setlist.size()-1);
			}
			for (int j = 0; j<setlist.size(); j++){
				Map<Integer,Integer> newmap = new HashMap<>();
				enabledlist.add(newmap);
			}
			/*
			System.out.println("Transitionset: " + setlist.get(setlist.size()-1) + " index: " + (setlist.size()-1));
			for (Transition t: setlist.get(setlist.size()-1)){
				System.out.println(" transitionmap for T: " + t + "  is: "+transitionmap.get(t));
			}
			System.out.println("transition set: " +newSet);
			System.out.println("preset: " +visited);
			System.out.println("postset" +  postset); DEBUG MODE*/
		}
		
	}
	
	public String[] getFlow() throws IOException {
		calculateSets();
		String[] flow = new String[pg.getN() + 1];
		Set<Integer> or = new HashSet<>();

		writer.write("# get flow from here \n");
		for (int i = 1; i < pg.getN(); i++) {
			or.clear();
			int someEnabled = oneSetEnabled(i);
			Set<Integer> fireDecision = new HashSet<>();
			Set<Integer> fireOne = new HashSet<>();
			fireDecision.add(someEnabled);
			fireOne.add(-someEnabled);
			fireDecision.add(decision(i));
			fireOne.add(fireOne(i));
			
			int number_decision = createUniqueID();
			writer.write(number_decision + " = " + writeAnd(fireDecision));
			int number_Fire_One = createUniqueID();
			writer.write(number_Fire_One + " = " + writeAnd(fireOne));
			or.add(number_decision);
			or.add(number_Fire_One);
			flow[i] = writeOr(or) + "# getFlow, should be written in WriteFlow iterate:" + i + "\n";
		}
		return flow;
	}
	
	
	public int decision(int i) throws IOException {
		writer.write("#  DECISION IS HERE \n");
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
		writer.write("# Decision ends here! \n");
		return number;
	}
	
	public int fireOne(int i) throws IOException{
		Set<Integer> or = new HashSet<>();
		for (int j = 0; j < pn.getTransitions().size(); ++j) {
			or.add(getOnlyOneTransition(transitions[j], i));
		}
		int number = createUniqueID();
		writer.write(number + " = " + writeOr(or));
		return number;
	}
	
	
	
	public int fireOneTransitionSet(int i, int setindex) throws IOException {

		writer.write("# fire one transition set\n");
		Set<Integer> outer_or = new HashSet<>();
		//System.out.println(setlist.get(setindex));
		for (Transition t : setlist.get(setindex)) {
			int number = createUniqueID();
			Set<Integer> and = new HashSet<>();
			int strat;
			for (Place p : t.getPreset()) {
				//and.add(getVarNr(p.getId() + "." + i, true));
				strat = addSysStrategy(p, t);
				if (strat != 0)
					and.add(strat);
			}
			for (Place p : t.getPostset()) {
				and.add(getVarNr(p.getId() + "." + (i + 1), true));
			}
			Set<Place> rest = new HashSet<>(prelist.get(setindex));
			rest.addAll(postlist.get(setindex));
			rest.removeAll(t.getPreset());
			rest.removeAll(t.getPostset());
			//System.out.println("Das ist die transition: " + t + " Das ist der Rest: " + rest);

			for (Place p : rest) {
				int p_i = getVarNr(p.getId() + "." + i, true);
				int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
				and.add(writeImplication(p_i, p_iSucc));
				and.add(writeImplication(p_iSucc, p_i));
			}

			Set<Place> preMinusPost = new HashSet<>(t.getPreset());
			preMinusPost.removeAll(t.getPostset());
			//System.out.println("Pre minus post: " + preMinusPost);
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
	
	public int dontFireOneTransitionSet(int i, int setindex) throws IOException{
		writer.write("# dont fire one transition from here \n");
		Set<Integer> outer_and = new HashSet<>();
		for (Place p: prelist.get(setindex)){
			int p_i = (getVarNr(p.getId() + "." + i, true));
			int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
			outer_and.add(writeImplication(p_i, p_iSucc));
			if (p.getPreset().isEmpty())
				outer_and.add(writeImplication(p_iSucc, p_i));
		}
		for (Place p: postlist.get(setindex)){
			Set<Integer> inner_or = new HashSet<>();
			int p_i = (getVarNr(p.getId() + "." + i, true));
			int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
			inner_or.add(writeImplication(-p_i, -p_iSucc));
			for(Transition t: p.getPreset()){
				if (!setlist.get(setindex).contains(t)){
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

	
	
	
	public int oneSetEnabled(int i) throws IOException {
		
		Set<Integer> or = new HashSet<>();
		for (int j = 0; j < setlist.size(); j++) {
			int enabled = isEnabledSet(i, j);
			or.add(enabled);
			enabledlist.get(j).put(i, enabled);
		}
		int number = createUniqueID();
		writer.write("# one set enabled \n");
		writer.write(number + " = " + writeOr(or));
		return number;
	}
	
	public int isEnabledSet(int i, int setindex) throws IOException{

		writer.write("# isEnabledSet \n");
		Set<Integer> outerAnd = new HashSet<>();
		Set<Integer> innerOr = new HashSet<>();
		for (Place p: prelist.get(setindex)){
			outerAnd.add(getVarNr(p.getId() + "." + i, true));
		}
		for (Transition t: setlist.get(setindex)){
			Set<Integer> inner_and = new HashSet<>();
			for (Place p : t.getPreset()){
				int strat = addSysStrategy(p,t);
				if (strat != 0){
					inner_and.add(strat);
					//System.out.println("Transition: " + t + " preset strategy: " + p); DEBUG
				}
			}
			if (!inner_and.isEmpty()){
				int inner_and_number = createUniqueID();
				writer.write(inner_and_number + " = " + writeAnd(inner_and));
				innerOr.add(inner_and_number);
			}
		}
		if (!innerOr.isEmpty()){
			int inner_or_number = createUniqueID();
			writer.write(inner_or_number + " = " + writeOr(innerOr));
			outerAnd.add(inner_or_number);
		}
		int outer_and_number = createUniqueID();
		writer.write(outer_and_number + " = " + writeAnd(outerAnd));
		return outer_and_number;
		
		
		
	}
	
	public int getOnlyOneTransition(Transition t, int i) throws IOException {

		writer.write("# get only one transition\n");
		int number = createUniqueID();
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
			rest = new HashSet<>(pg.getGame().getPlaces());
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
		writeTerminatingSubFormulas(1, pg.getN());
		String[] terminating = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			if (pg.getGame().getTransitions().size() >= 1) {
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
				key = createUniqueID();
				pre = t.getPreset();
				or.clear();
				for (Place p : pre) {
					or.add(-getVarNr(p.getId() + "." + i, true));
				}
				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[pn.getTransitions().size() * (i - 1) + j] = key;
			}
		}
	}

	public String[] getDeterministic() throws IOException { // faster than naive
															// implementation
		List<Set<Integer>> and = new ArrayList<>(pg.getN() + 1);
		and.add(null); // first element is never accessed
		for (int i = 1; i <= pg.getN(); ++i) {
			and.add(new HashSet<Integer>());
		}
		Transition t1, t2;
		Transition[] sys_transitions;
		for (Place sys : pn.getPlaces()) {
			// Additional system places are not forced to behave
			// deterministically, this is the faster variant (especially the
			// larger the PG becomes)
			if (!pg.getGame().getEnvPlaces().contains(sys) && !sys.getId().startsWith(QBFConSolver.additionalSystemName)) {
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

	public String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 2; j <= pg.getN(); ++j) {
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
			if (i == pg.getN() - 1) {
				int j = pg.getN();
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

	public int addSysStrategy(Place p, Transition t) {
		if (!pg.getGame().getEnvPlaces().contains(p)) {
			if (p.getId().startsWith(QBFConSolver.additionalSystemName)) {
				return getVarNr(p.getId() + ".." + t.getId(), true);
			} else {
				return getVarNr(p.getId() + ".." + getTruncatedId(t.getId()), true);
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
		sb.append(")" + QBFConSolver.linebreak);
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