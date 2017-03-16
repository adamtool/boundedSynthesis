package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 *
 * @author Jesko Hecking-Harbusch
 *
 */
public class QBFPetriGame extends PetriGame {
	
	private int n = 0; // length of the simulation, i.e. for n there are n - 1 transitions simulated
	private int b = 0; // number of unfoldings per place in the bounded unfolding
	
    public QBFPetriGame(PetriNet pn) throws UnboundedPGException {
        super(pn);
    }
    
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
}
