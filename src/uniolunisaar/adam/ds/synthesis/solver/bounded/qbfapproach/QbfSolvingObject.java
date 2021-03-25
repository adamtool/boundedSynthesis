package uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.SolvingObject;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.util.AdamExtensions;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.synthesis.solver.bounded.BoundedExtensionHandler;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;

/**
 * Parameters for bounded synthesis added. Possibilities to remove
 * places/transitions according to (winning) strategy implemented. Petri game
 * can be copied.
 *
 * @author Jesko Hecking-Harbusch
 * @param <W>
 */
public class QbfSolvingObject<W extends Condition<W>> extends SolvingObject<PetriGameWithTransits, W, QbfSolvingObject<W>> {

    private int n; // length of the simulation, i.e., for n there are n - 1 transitions simulated
    private int b; // number of unfoldings per place in the bounded unfolding
    private Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>(); // tokenflow

    public QbfSolvingObject(PetriGameWithTransits game, W winCon, boolean skipChecks) throws SolvingException {
        super(game, winCon);
        for (Transition t : game.getTransitions()) {	// TODO necessary!
            fl.put(t, new HashSet<>());
        }

        if (!skipChecks) {
        	if (!game.getBounded().isSafe()) {
        		throw new SolvingException(new NetNotSafeException(game.getBounded().unboundedPlace.toString(), game.getBounded().sequence.toString()));
        	}
        }
        // TODO JESKO: add how the flows should be copied or if you still need them after the restructuring.
    }

    // Before removing a transition, always check that it has not already been removed, because a single missing place suffices.
    public void removeTransitionRecursively(Transition t) {
        if (getGame().getTransitions().contains(t)) {
            Set<Place> followingPlaces = new HashSet<>(t.getPostset());
            getGame().removeTransition(t);
            Marking initialMarking = getGame().getInitialMarking();
            for (Place p : followingPlaces) {
                // remove place p if all transition leading to it are removed or all incoming transitions are also outgoing from p but don't remove place if part of initial marking
                if (getGame().getPlaces().contains(p) && initialMarking.getToken(p).getValue() == 0 && (p.getPreset().isEmpty() || p.getPostset().containsAll(p.getPreset()))) {
                    removePlaceRecursively(p);
                }
            }
        }
    }

    public void removePlaceRecursively(Place p) {
        Set<Transition> followingTransitions = new HashSet<>(p.getPostset());
        getGame().removePlace(p);
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

    public int getBoundedParameterNFromExtension() {
        return BoundedExtensionHandler.getBoundN(getGame());
    }

    public boolean hasBoundedParameterNinExtension() {
        return BoundedExtensionHandler.hasBoundN(getGame());
    }

    public int getBoundedParameterBFromExtension() {
        return BoundedExtensionHandler.getBoundB(getGame());
    }

    public boolean hasBoundedParameterBinExtension() {
        return BoundedExtensionHandler.hasBoundB(getGame());
    }

	@Override
    public QbfSolvingObject<W> getCopy() {
    	QbfSolvingObject<W> result = null;
		try {
			result = new QbfSolvingObject<>(new PetriGameWithTransits(this.getGame()), this.getWinCon().getCopy(), true);
		} catch (SolvingException e) {
			// cannot be thrown as it was tested for safe on initialisation
		}
        result.setN(this.n);
        result.setB(this.b);
        return result;
    }
}
