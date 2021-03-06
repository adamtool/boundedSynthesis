package uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.unfolder;

import java.util.*;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolvingObject;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;

/**
 * 
 * 27.11.2018 ONLY WORKS FOR FINITE NETS AND NEEDS QBFCONTROL rebuildingUnfolder ENABLED
 * copy original net and then build net in place new
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class FiniteDeterministicUnfolder extends Unfolder {

	private final QbfSolvingObject<? extends Condition<?>> originalSolvingObj;
	private final PetriGameWithTransits originalGame;
	private final Map<Place, Set<Place>> pred = new HashMap<>();

	public Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
	public int counter = 0;
	
	public FiniteDeterministicUnfolder(QbfSolvingObject<? extends Condition<?>> petriGame, Map<String, Integer> max) {
		super(petriGame, max);

		originalSolvingObj = petriGame.getCopy();
		originalGame = originalSolvingObj.getGame();
		
		for (Transition t : new HashSet<>(pn.getTransitions())) {
			pn.removeTransition(t);
		}
		
		for (Place p : new HashSet<>(pn.getPlaces())) {
			pn.removePlace(p);
		}
		
		Marking initial = originalGame.getInitialMarking();
		Marking initial_unfolding = new Marking(pn);
		for (Place p : originalGame.getPlaces()) {
			if (initial.getToken(p).getValue() == 1) {
				Place newP = pg.getGame().createPlace(p.getId() + "__" + counter++);
				newP.setInitialToken(1);
				if (originalGame.getEnvPlaces().contains(p)) {
					pg.getGame().getEnvPlaces().add(newP);
				}
				for (Pair<String, Object> pair : p.getExtensions()) {
					newP.putExtension(pair.getFirst(), pair.getSecond());
				}
				initial_unfolding.addToken(newP, 1);
				pred.put(newP, new HashSet<>());
			}
		}
		queue.add(new Pair<>(initial_unfolding, 1));
	}

	@Override
	protected void createUnfolding() {
		Pair<Marking, Integer> currentPair;
		while ((currentPair = queue.poll()) != null){
			Marking marking = currentPair.getFirst();
			int i = currentPair.getSecond();
			Set<String> ids = new HashSet<>();
			for (Place p : pn.getPlaces()) {
				if (marking.getToken(p).getValue() > 0) {
					ids.add(getTruncatedId(p.getId()));
				}
			}
			
			for (Transition t : originalGame.getTransitions()) {
				Set<String> preset_ids = new HashSet<>();
				for (Place pre : t.getPreset()) {
					preset_ids.add(pre.getId());
				}
				if (ids.containsAll(preset_ids)) { // preset subseteq marking -> transition is enabled
					// fire existing transition
					boolean fired = false;
					for (Transition post : pn.getTransitions()) {
						if (getTruncatedId(post.getId()).equals(t.getId()) && post.isFireable(marking)) {
							Marking nextMarking = new Marking(marking);
							nextMarking.fire(post);
							if (i + 1 <= pg.getN()) {
								queue.add(new Pair<>(nextMarking, i + 1));
							}
							fired = true;
						}
					}
					if (!fired) {
						// add copy of transition if not yet existent
						Set<Place> preset = new HashSet<>();
						for (Place pre : pn.getPlaces()) {
							if (marking.getToken(pre).getValue() > 0) {
								if (preset_ids.contains(getTruncatedId(pre.getId()))) {
									preset.add(pre);
								}
							}
						}
						
						Transition newT = pg.getGame().createTransition(t.getId() + "__" + counter++);
						for (Place pre : preset) {
							pn.createFlow(pre, newT);
						}
						for (Place post : t.getPostset()) {
							Place newPost = pg.getGame().createPlace(post.getId() + "__" + counter++);
							if (originalGame.getEnvPlaces().contains(post)) {
								pg.getGame().getEnvPlaces().add(newPost);
							}
							for (Pair<String, Object> pair : post.getExtensions()) {
								newPost.putExtension(pair.getFirst(), pair.getSecond());
							}
							pn.createFlow(newT, newPost);
							pred.put(newPost, predecessors(newPost, newPost));
						}
						
						Marking nextMarking = new Marking(marking);
						nextMarking.fire(newT);
						if (i + 1 <= pg.getN()) {
							queue.add(new Pair<>(nextMarking, i + 1));
						}
					}
				}
			}
		}
	}

	/** Given a place from the unfolding, this function returns the set of places in its causal past that are based
	 * on the same place in the underlying Petri net. It performs DFS and therefore needs current and goal. Thus,
	 * initial call with current == goal.
	 *
	 * @param current the current place to check in the unfolding
	 * @param goal the place for which is searched in the causal past
	 * @return the possibly empty set of places in the causal past of current that are based on goal
	 */

	private Set<Place> predecessors(Place current, Place goal) {
		for (Transition t : current.getPreset()) {
			for (Place p : t.getPreset()) {
				if (getTruncatedId(p.getId()).equals(getTruncatedId(goal.getId()))) {
					// copy found
					Set<Place> result = new HashSet<>(pred.get(p));
					result.add(p);
					return result;
				} else {
					// no copy
					Set<Place> result = predecessors(p, goal);
					if (!result.isEmpty()) {
						return result;
					}
				}
			}
		}
		return new HashSet<>();
	}
}
