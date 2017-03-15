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
 * Requires the executables "bc2cnf" and "depqbf" from BCPackage and DepQBF as
 * well as the Petri Game in .apt format at the same position as the executable.
 */
public class QBFPetriGame extends PetriGame {

	// TODO folgendes schön aufteilen
	public String linebreak = "\n\n"; // Controller
	public String additionalSystemName = "AS___"; //Controller
	
	private int n = 0;
	private int b = 0;
	
    public QBFPetriGame(PetriNet pn) throws UnboundedPGException {
        super(pn);
    }
    
	public void removeTransitionRecursively(Transition t) {
		if (getNet().getTransitions().contains(t)) {
			Set<Place> next = new HashSet<>(t.getPostset());
			Marking in = getNet().getInitialMarking();
			getNet().removeTransition(t);
			for (Place p : next) {
				// remove place p if all transition leading to it are removed or all
				// incoming transitions are also outgoing from p
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

	public Set<Place> placesDirectlyBefore(Place p) {
		Set<Place> result = new HashSet<>();
		for (Transition t : p.getPreset())
			for (Place pre : t.getPreset())
				result.add(pre);
		return result;
	}

	public Set<Place> placesDirectlyAfter(Place p) {
		Set<Place> result = new HashSet<>();
		for (Transition t : p.getPostset()) {
			for (Place post : t.getPostset()) {
				result.add(post);
			}
		}
		return result;
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
