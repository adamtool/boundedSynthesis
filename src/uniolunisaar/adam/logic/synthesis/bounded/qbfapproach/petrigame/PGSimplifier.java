package uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.petrigame;

import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolvingObject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Token;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * TODO removal is based on n, can shorten strategy wrongly for too short n and wrong strategy
 * 
 * This class removes transitions and following places according to the
 * decisions of the WINNING strategy and the simulation length. Unreachable
 * places from the original Petri game can be removed. Additional places of the
 * NonDeterministicUnfolder can be removed.
 * 
 * @author Jesko Hecking-Harbusch
 */
public class PGSimplifier {
	
	private final QbfSolvingObject<? extends Condition<?>> solvingObject;
	private final boolean removeAdditionalPlaces;
	private final boolean removeUnreachablePlaces;
	private final boolean trueConcurrent;
	
	public PGSimplifier(QbfSolvingObject<? extends Condition<?>> solvingObject, boolean removeAdditionalPlaces, boolean removeUnreachablePlaces, boolean trueConcurrent) {
		this.solvingObject = solvingObject;
		this.removeAdditionalPlaces = removeAdditionalPlaces;
		this.removeUnreachablePlaces = removeUnreachablePlaces;
		this.trueConcurrent = trueConcurrent;
	}

	public void simplifyPG() {
		// Initialization
		Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
		queue.add(new Pair<>(solvingObject.getGame().getInitialMarking(), 1));

		Set<Marking> closed = new HashSet<>();
		Set<Transition> firedTransitions = new HashSet<>();
		Set<Place> reachedPlaces = new HashSet<>();

		if (removeUnreachablePlaces) {
			addReachedPlaces(solvingObject.getGame().getInitialMarking(), reachedPlaces);
		}

		// Search loop
		Pair<Marking, Integer> pair;
		while ((pair = queue.poll()) != null) {
			Marking marking = pair.getFirst();
			int i = pair.getSecond();
			closed.add(marking);
			Set<Set<Transition>> tcTransitionsSet = new HashSet<>();
			for (Transition transition : solvingObject.getGame().getTransitions()) {
				if (transition.isFireable(marking)) {
					if (trueConcurrent) {
						// True Concurrent: search for all sets of tc transitions to fire afterwards
						Set<Transition> tctransitions = new HashSet<>();
						tctransitions.add(transition);
						Marking nextMarking = transition.fire(marking);
						Transition t;
						while ((t = findFurtherTransition(marking, nextMarking, tctransitions)) != null) {
							tctransitions.add(t);
							nextMarking = t.fire(nextMarking);
						}
						tcTransitionsSet.add(tctransitions);
					} else {
						// not True Concurrent: fire transition
						Marking nextMarking = transition.fire(marking);
						firedTransitions.add(transition);
						if (!closed.contains(nextMarking)) {
							if (removeUnreachablePlaces) {
								addReachedPlaces(nextMarking, reachedPlaces);
							}
							if (i + 1 <= solvingObject.getN()) {
								queue.add(new Pair<>(nextMarking, i + 1));
							}
						}
					}
				}
			}
			
			// TODO this seems buggy for deadlock-avoidance for TC
			// True Concurrent: fire all set of tc transitions afterwards
			if (trueConcurrent) {
				for (Set<Transition> tctransitions : tcTransitionsSet) {
					if (!tctransitions.isEmpty()) {
						Marking firing = new Marking(marking);
						for (Transition tc : tctransitions) {
							firing = tc.fire(firing);
							firedTransitions.add(tc);
							if (removeUnreachablePlaces) {
								addReachedPlaces(firing, reachedPlaces);
							}
						}
						if (!closed.contains(firing)) {
							if (i + 1 <= solvingObject.getN()) {
								queue.add(new Pair<>(firing, i + 1));
							}
						}
					}
				}
			}
		}
		removal(firedTransitions, reachedPlaces);
	}
	
	/**
	 * returns null if no transition existent, needed to fire all transitions in the true concurrent case
	 * 
	 * @param start
	 * @param current
	 * @param tctransitions
	 * @return
	 */
	
	private Transition findFurtherTransition (Marking start, Marking current, Set<Transition> tctransitions) {
		for (Transition transition : solvingObject.getGame().getTransitions()) {
			// transition is now and initially fireable
			if (!tctransitions.contains(transition) && transition.isFireable(current) && transition.isFireable(start)) {
				return transition;
			}
		}
		return null;
	}
	
	/**
	 * remove unreachable places and transitions as well as additional system places if wanted
	 * 
	 * @param firedTransitions
	 * @param reachedPlaces
	 */
	
	private void removal(Set<Transition> firedTransitions, Set<Place> reachedPlaces) {
		// Removal
		Set<Transition> transitions = new HashSet<>(solvingObject.getGame().getTransitions());
		for (Transition transition : transitions) {
			if (!firedTransitions.contains(transition)) {
				solvingObject.removeTransitionRecursively(transition);
			}
		}
		if (removeAdditionalPlaces) {
			removeAS();
		}
		if (removeUnreachablePlaces) {
			for (Place place : solvingObject.getGame().getPlaces()) {
				if (!reachedPlaces.contains(place)) {
					solvingObject.removePlaceRecursively(place);
				}
			}
		}
	}
	
	/**
	 * Additional system places from unfolder are removed including their flow.
	 */
	private void removeAS() {
		Set<Place> places = new HashSet<>(solvingObject.getGame().getPlaces());
		for (Place place : places) {
			if (place.getId().startsWith(QbfControl.additionalSystemName)) {
				Set<Transition> transitions = new HashSet<>(place.getPreset());
				for (Transition transition : transitions) {
					solvingObject.getGame().removeFlow(place, transition);
					solvingObject.getGame().removeFlow(transition, place);
				}
				solvingObject.getGame().removePlace(place);
			}
		}
	}

	/**
	 * For a given Petri game and option that unreachable
	 * places are removed, places with token from marking are added to the set of
	 * reached places.
	 *
	 * @param marking
	 * @param reachedPlaces
	 */
	private void addReachedPlaces(Marking marking, Set<Place> reachedPlaces) {
		for (Place place : solvingObject.getGame().getPlaces()) {
			Token token = marking.getToken(place);
			if (token.getValue() > 0) {
				reachedPlaces.add(place);
			}
		}
	}
}
