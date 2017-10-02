package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 * Parameters four bounded synthesis added.
 * Possibilities to remove places/transitions according to (winning) strategy implemented.
 * Petri game can be copied.
 * 
 * @author Jesko Hecking-Harbusch
 */
public class QBFPetriGame extends PetriGame {

	private int n; // length of the simulation, i.e., for n there are n - 1 transitions simulated
	private int b; // number of unfoldings per place in the bounded unfolding
	private Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();

	public QBFPetriGame(PetriNet pn) throws UnboundedPGException {
		super(pn);
		for (Transition t : pn.getTransitions()) {
			fl.put(t, new HashSet<>());
		}
	}
	
	// Before removing a place or transition, always check that it has not already been removed.
	public void removeTransitionRecursively(Transition t) {
		if (getNet().getTransitions().contains(t)) {
			Set<Place> next = new HashSet<>(t.getPostset());
			Marking in = getNet().getInitialMarking();
			getNet().removeTransition(t);
			for (Place p : next) {
				// remove place p if all transition leading to it are removed or all incoming transitions are also outgoing from p
				// don't remove place if part of initial marking
				if (getNet().getPlaces().contains(p) && in.getToken(p).getValue() == 0 && (p.getPreset().isEmpty() || p.getPostset().containsAll(p.getPreset()))) {
					removePlaceRecursively(p);
				}
			}
		}
	}

	public void removePlaceRecursively(Place p) {
		Set<Transition> next = new HashSet<>(p.getPostset());
		getNet().removePlace(p);
		for (Transition t : next) {
			// remove transition t as soon as one place in pre(t) is removed
			removeTransitionRecursively(t);
		}
	}

	public QBFPetriGame copy(String name) {
		PetriNet copy = new PetriNet(getNet().getName() + "_" + name);

		for (Place p : getNet().getPlaces()) {
			Place copyPlace = copy.createPlace(p.getId());
			for (Pair<String, Object> pair : p.getExtensions()) {
				copyPlace.putExtension(pair.getFirst(), pair.getSecond());
			}
		}
		for (Transition t : getNet().getTransitions()) {
			copy.createTransition(t.getId());
		}
		for (Flow f : getNet().getEdges()) {
			copy.createFlow(f.getSource().getId(), f.getTarget().getId());
		}

		Marking in = getNet().getInitialMarking();
		for (Place p : getNet().getPlaces()) {
			if (in.getToken(p).getValue() == 1) {
				copy.getPlace(p.getId()).setInitialToken(1);
			}
		}

		QBFPetriGame newPG = null;
		try {
			newPG = new QBFPetriGame(copy);
		} catch (UnboundedPGException e) {
			System.out.println("Something went wrong when copying a Petri game. The copied game was found unbounded. This should not happen as the original game should not be unbounded.");
			e.printStackTrace();
		}
		for (Place p : getNet().getPlaces()) {
			if (getEnvPlaces().contains(p)) {
				newPG.getEnvPlaces().add(p);
			}
		}
		return newPG;
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
