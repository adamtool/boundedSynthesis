package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Token;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolver;

/**
 * This class removes transitions and following places according to the decisions of the WINNING strategy and the simulation length. 
 * Unreachable places from the original Petri game can be removed. 
 * Additional places of the NonDeterministicUnfolder can be removed. 
 * 
 * @author Jesko Hecking-Harbusch
 */
public class PGSimplifier {

	public static void simplifyPG(QBFSolvingObject pg, boolean removeAdditionalPlaces, boolean removeUnreachablePlaces) {
		// Initialization
		int n = pg.getN();

		Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
		queue.add(new Pair<>(pg.getGame().getInitialMarking(), 1));

		Set<Marking> closed = new HashSet<>();
		Set<Transition> firedTransitions = new HashSet<>();
		Set<Place> reachedPlaces = new HashSet<>();
		
		if (removeUnreachablePlaces) {
			addReachedPlaces(pg, pg.getGame().getInitialMarking(), reachedPlaces);
		}

		// Search loop
		Pair<Marking, Integer> pair;
		while ((pair = queue.poll()) != null) {
			Marking marking = pair.getFirst();
			int i = pair.getSecond();
			closed.add(marking);
			for (Transition transition : pg.getGame().getTransitions()) {
				if (transition.isFireable(marking)) {
					Marking nextMarking = transition.fire(marking);
					firedTransitions.add(transition);
					if (!closed.contains(nextMarking)) {
						if (removeUnreachablePlaces) {
							addReachedPlaces(pg, nextMarking, reachedPlaces);
						}
						if (i + 1 <= n) {
							queue.add(new Pair<>(nextMarking, i + 1));
						}
					}
				}
			}
		}
		
		// Removal
		Set<Transition> transitions = new HashSet<>(pg.getGame().getTransitions());
		for (Transition transition : transitions) {
			if (!firedTransitions.contains(transition)) {
				pg.removeTransitionRecursively(transition);
			}
		}
		if (removeAdditionalPlaces) {
			removeAS(pg);
		}
		if (removeUnreachablePlaces) {
			for (Place place : pg.getGame().getPlaces()) {
				if (!reachedPlaces.contains(place)) {
					pg.removePlaceRecursively(place);
				}
			}
		}
	}

	/**
	 * Additional system places from unfolder are removed including their flow.
	 * @param pg
	 */
	private static void removeAS(QBFSolvingObject pg) {
		Set<Place> places = new HashSet<>(pg.getGame().getPlaces());
		for (Place place : places) {
			if (place.getId().startsWith(QBFSolver.additionalSystemName)) {
				Set<Transition> transitions = new HashSet<>(place.getPreset());
				for (Transition transition : transitions) {
					pg.getGame().removeFlow(place, transition);
					pg.getGame().removeFlow(transition, place);
				}
				pg.getGame().removePlace(place);
			}
		}
	}
	
	/**
	 * For a given Petri game (because class is static) and option that unreachable places are removed, 
	 * places with token from marking are added to the set of reached places.
	 * @param pg
	 * @param marking
	 * @param reachedPlaces
	 */
	private static void addReachedPlaces(QBFSolvingObject pg, Marking marking, Set<Place> reachedPlaces) {
		for (Place place : pg.getGame().getPlaces()) {
			Token token = marking.getToken(place);
			if (token.getValue() > 0) {
				reachedPlaces.add(place);
			}
		}
	}
}
