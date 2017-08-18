package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Token;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolver;

/**
 * This class removes transitions and following places according to the decisions of the (winning) strategy. 
 * Unreachable places in the original Petri game can be removed. 
 * Additional places of the NonDeterministicUnfolder can be removed. 
 * TODO performance evaluation BEFORE solving pending, performance
 * boost would indicate bad tests
 * 
 * @author Jesko Hecking-Harbusch
 */

public class PGSimplifier {

	public static void simplifyPG(QBFPetriGame pg, boolean removeAdditionalPlaces, boolean removeUnreachablePlaces) {
		PetriNet pn = pg.getNet();
		int n = pg.getN();

		Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
		queue.add(new Pair<>(pn.getInitialMarking(), 0));

		Set<Pair<Marking, Integer>> closed = new HashSet<>();
		Set<Transition> firedTransitions = new HashSet<>();
		Set<Place> reachedPlaces = new HashSet<>();
		if (removeUnreachablePlaces) {
			addMarking(pg, reachedPlaces, pn.getInitialMarking());
		}

		Pair<Marking, Integer> p;
		while ((p = queue.poll()) != null) {
			Marking m = p.getFirst();
			int i = p.getSecond();
			for (Transition t : pn.getTransitions()) {
				if (t.isFireable(m)) {
					Marking next = t.fire(m);
					firedTransitions.add(t);
					if (i <= n && !closed.contains(next)) {
						queue.add(new Pair<>(next, i + 1));
					}
					if (removeUnreachablePlaces) {
						addMarking(pg, reachedPlaces, next);
					}
				}
			}
			closed.add(p); // add place to closed this late to allow self-loops for reaching transitions
		}
		Set<Transition> transitions = new HashSet<>(pn.getTransitions());
		for (Transition t : transitions)
			if (!firedTransitions.contains(t))
				pg.removeTransitionRecursively(t);
		if (removeAdditionalPlaces) {
			removeAS(pg);
		}
		if (removeUnreachablePlaces) {
			for (Place place : pn.getPlaces()) {
				if (!reachedPlaces.contains(place)) {
					pg.removePlaceRecursively(place);
				}
			}
		}
	}

	private static void removeAS(QBFPetriGame pg) {
		PetriNet pn = pg.getNet();
		Set<Place> places = new HashSet<>(pn.getPlaces());
		for (Place p : places) {
			if (p.getId().startsWith(QBFSolver.additionalSystemName)) {
				Set<Transition> transitions = new HashSet<>(p.getPreset());
				for (Transition pre : transitions) {
					pn.removeFlow(p, pre);
					pn.removeFlow(pre, p);
				}
				pn.removePlace(p);
			}
		}
	}

	private static void addMarking(QBFPetriGame pg, Set<Place> places, Marking marking) {
		for (Place place : pg.getNet().getPlaces()) {
			Token token = marking.getToken(place);
			if (token.getValue() > 0) {
				places.add(place);
			}
		}
	}
}
