package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.bounded.qbfapproach.solver.SolverQbfAndQbfCon;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Condition;

/**
 * 
 * @author Niklas Metzger
 *
 */

public abstract class QbfConSolver<W extends Condition> extends SolverQbfAndQbfCon<W, QbfConSolverOptions> {

	protected List<Map<Integer,Integer>> enabledlist; // First setindex, then iteration index
	protected Map<Transition, Integer> transitionmap; 
	protected List<Transition> setlist;
	protected Set<Set<Place>> strongComponents;
	protected int loopBound;


	
	protected QbfConSolver(PetriGame game, W winCon, QbfConSolverOptions so) throws SolvingException {
		super(new QbfSolvingObject<>(game, winCon, false), so);
		
		// initializing bounded parameters n and b
		initializeNandB(so.getN(), so.getB());
		
		// initializing arrays for storing variable IDs
		initializeBeforeUnfolding(getSolvingObject().getN());
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
		Set<String> post_transitions = new HashSet<>();
		Set<String> truncatedPostSet = new HashSet<>();
		Set<Integer> inner_or = new HashSet<>();
		Set<Integer> inner_and = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getEnvPlaces()) {			
			if (!p.getPostset().isEmpty()) {	
				// only iterate over truncated ID once
				for (Transition t : p.getPostset()) {
					truncatedPostSet.add(getTruncatedId(t.getId()));
				}
				for (int i = 1; i <= loopBound; i++) {//getSolvingObject().getN() //TODO
					for (String t : truncatedPostSet) {
						post_transitions.addAll(truncatedPostSet);
						post_transitions.remove(t);
						int check_truncated = addEnvStrategy(p, t, i);
						for (String t_prime : post_transitions) {
							//if (addEnvStrategy(p, t_prime, i) != check_truncated) // TODO unnecessary because now only iterate over truncated IDs?
								inner_and.add(-addEnvStrategy(p, t_prime, i));
						}
						inner_and.add(check_truncated);
						int inner_and_var = createUniqueID();
						writer.write(inner_and_var + " = " + writeAnd(inner_and));
						inner_or.add(inner_and_var);
						inner_and.clear();
						post_transitions.clear();
					}
					int inner_or_var = createUniqueID();
					writer.write(inner_or_var + " = " + writeOr(inner_or));
					outer_and.add(inner_or_var);
					inner_or.clear();
				}
				truncatedPostSet.clear();
			}
		}
		return writeAnd(outer_and);
	}
	
	public String getDetAdditionalSys() throws IOException {
		//No check for single post transition, bc additional system place always has at least 2 post transition
		Set<Integer> outer_and = new HashSet<>();
		Set<Integer> inner_and = new HashSet<>();
		Set<Integer> inner_or = new HashSet<>();
		for (Place addsys : getSolvingObject().getGame().getPlaces()){
			inner_or.clear();
			if (!getSolvingObject().getGame().getEnvPlaces().contains(addsys)
				&& addsys.getId().startsWith(QbfControl.additionalSystemName)) {
				for (Transition t : addsys.getPostset()){
					inner_and.clear();
					inner_and.add(addSysStrategy(addsys, t));
					for (Transition t_prime : addsys.getPostset()){
						if (!t.getId().equals(t_prime.getId()))
							inner_and.add(-addSysStrategy(addsys,t_prime));
					}
					int inner_and_id = createUniqueID();
					writer.write(inner_and_id + " = " + writeAnd(inner_and));
					inner_or.add(inner_and_id);
					}
				int inner_or_id = createUniqueID();
				writer.write(inner_or_id + " = " + writeOr(inner_or));
				outer_and.add(inner_or_id);
			
				}
			}
		return writeAnd(outer_and);
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
			strat = addEnvStrategy(p, t.getId(), 1); //TODO i
			if (strat != 0)
				outerAnd.add(strat);
		}
		//Only transitions with sys place in preset can be stalled by env
		Set<Place> sysPrePlaces = new HashSet<Place>(t.getPreset());
		sysPrePlaces.removeAll(getSolvingObject().getGame().getEnvPlaces());
		if (!sysPrePlaces.isEmpty()) {
			outerAnd.add(addEnvStall(t));
		}
		writer.write(outer_and_number + " = " + writeAnd(outerAnd));
		
		return outer_and_number;
	}

	protected int addEnvStrategy(Place p, String t, int i) {
		if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
			return getVarNr(p.getId() + "**" + getTruncatedId(t) + "**" + i, true);
		} else {
			return 0;
		}
	}
	
	public int addEnvStall(Transition t){
		return getVarNr((getTruncatedId(t.getId()) + "**" + "stall"), true);
	}
	
	protected String getLoopIJ() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (Set<Place> currentSet: strongComponents) {
			Set<Integer> or = new HashSet<>();
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				for (int j = i + 1; j <= getSolvingObject().getN(); ++j) {
					Set<Integer> and2 = new HashSet<>();
					for (Place p : currentSet) {
						// additional system places cannot leave their places, they always loop
						if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
							int p_i = getVarNr(p.getId() + "." + i, true);
							int p_j = getVarNr(p.getId() + "." + j, true);
							and2.add(writeImplication(p_i, p_j));
							and2.add(writeImplication(p_j, p_i));
						}
					}
					int andNumber = createUniqueID();
					writer.write(andNumber + " = " + writeAnd(and2));
					or.add(andNumber);
				}
			}
			int orNumber = createUniqueID();
			writer.write(orNumber + " = " + writeOr(or));
			and.add(orNumber);
		}
		return writeAnd(and);
	}
	
	
}