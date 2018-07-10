package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 * Parameters for bounded synthesis added.
 * Possibilities to remove places/transitions according to (winning) strategy implemented.
 * Petri game can be copied.
 * 
 * @author Jesko Hecking-Harbusch
 */
public class QBFPetriGame extends PetriGame {

	private int n; // length of the simulation, i.e., for n there are n - 1 transitions simulated
	private int b; // number of unfoldings per place in the bounded unfolding
	private Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>(); // tokenflow

	public QBFPetriGame(PetriNet pn) throws NotSupportedGameException {
		super(pn);
		for (Transition t : pn.getTransitions()) {
			fl.put(t, new HashSet<>());
		}
	}
	
	// Before removing a transition, always check that it has not already been removed, because a single missing place suffices.
	public void removeTransitionRecursively(Transition t) {
		if (getNet().getTransitions().contains(t)) {
			Set<Place> followingPlaces = new HashSet<>(t.getPostset());
			getNet().removeTransition(t);
			Marking inintialMarking = getNet().getInitialMarking();
			for (Place p : followingPlaces) {
				// remove place p if all transition leading to it are removed or all incoming transitions are also outgoing from p but don't remove place if part of initial marking
				if (getNet().getPlaces().contains(p) && inintialMarking.getToken(p).getValue() == 0 && (p.getPreset().isEmpty() || p.getPostset().containsAll(p.getPreset()))) {
					removePlaceRecursively(p);
				}
			}
		}
	}

	public void removePlaceRecursively(Place p) {
		Set<Transition> followingTransitions = new HashSet<>(p.getPostset());
		getNet().removePlace(p);
		for (Transition t : followingTransitions) {
			// remove transition t as soon as one place in pre(t) is removed
			removeTransitionRecursively(t);
		}
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getB() {
		return b;
	}

	public void setB(int b) {
		this.b = b;
	}
	
	public Map<Transition, Set<Pair<Place, Place>>> getFl() {
		return fl;
	}
	
	public void setFl(Map<Transition, Set<Pair<Place, Place>>> fl) {
		this.fl = fl;
	}
}
