package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.exception.TransitionFireException;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;

public class PGSimplifier {

	public static void simplifyPG(QBFPetriGame pg, boolean removeAdditionalPlace) {			// TODO performance evaluate PGsimplify

		PetriNet pn = pg.getNet();
		int n = pg.getN();
		Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
		Set<Pair<Marking, Integer>> closed = new HashSet<>();
		queue.add(new Pair<>(pn.getInitialMarking(), 0));
		Pair<Marking, Integer> p;
		Set<Transition> visited = new HashSet<>();
		while ((p = queue.poll()) != null) {
			Marking m = p.getFirst();
			int i = p.getSecond();
			//System.out.println("ITERATION " + queue.size() + " : " + m + " : " + i);
			for (Transition t : pn.getTransitions()) {
				Marking next = new Marking(m);
				try {
					//next.fire(t); <- deprecated
					next = t.fire(next);
				} catch (TransitionFireException e) {
					continue;
				}
				visited.add(t);
				if (i <= n && ! closed.contains(next)) {
					queue.add(new Pair<>(next, i + 1));
				}
			}
			closed.add(p);			// add closed this late to allow selfloops to reach transitions
		}
		Set<Transition> transitions = new HashSet<>(pn.getTransitions());
		for (Transition t : transitions)
			if (!visited.contains(t))
				pg.removeTransitionRecursively(t);
		if (removeAdditionalPlace) {
			removeAS(pg);
		}
	}
	
	private static void removeAS(QBFPetriGame pg) {
		PetriNet pn = pg.getNet();
		Set<Place> places = new HashSet<>(pn.getPlaces());
		for (Place p : places) {
			if (p.getId().startsWith(pg.additionalSystemName)) {
				Set<Transition> transitions = new HashSet<>(p.getPreset());
				for (Transition pre : transitions) {
					pn.removeFlow(p, pre);
					pn.removeFlow(pre, p);
				}
				pn.removePlace(p);
			}
		}
	}
}
