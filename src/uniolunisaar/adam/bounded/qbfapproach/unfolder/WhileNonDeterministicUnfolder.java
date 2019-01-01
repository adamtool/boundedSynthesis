package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.exceptions.pg.NetNotSafeException;
import uniolunisaar.adam.exceptions.pg.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.objectives.Condition;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

// TODO this approach continuously unfolds places when bound is too high, e.g. b=10, this gives poor performance

public class WhileNonDeterministicUnfolder extends NonDeterministicUnfolder {
	private Queue<String> placesToUnfold;

	public WhileNonDeterministicUnfolder(QbfSolvingObject<? extends Condition> QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
	}

	@Override
	protected void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		// Initialize queue
		placesToUnfold = initializeQueue();
		// begin unfolding
		while (!placesToUnfold.isEmpty()) {
			String id = placesToUnfold.poll();
			Place p = pn.getPlace(id);
			placesToUnfold.addAll(unfoldPlace(p));
		}

		// add additional system places to unfolded env transitions
		addAdditionalSystemPlaces();
	}

	private Queue<String> initializeQueue() throws UnboundedException, NetNotSafeException {
		// THEORY: 2 transitions from only (sich gegenseitig ausschließenden) sys places to the same place do not require unfolding TODO das hier prüfen
		Queue<String> result = new LinkedList<>(); // fancy
		Marking in = pn.getInitialMarking();
		// add non-initial places which can have different history
		for (Place p : pn.getPlaces()) {
			if (in.getToken(p).getValue() == 0) {
				if (p.getPreset().size() >= 2) {
					if (p.getPostset().size() >= 1 && unfolding(p)) {
						result.add(p.getId());
					}
				}
			}
		}

		// add initial places which can be re-reached via loop
		for (Place p : pn.getPlaces()) {
			if (in.getToken(p).getValue() >= 1) {
				if (p.getPreset().size() >= 1) {
					if (p.getPostset().size() >= 1 && unfolding(p)) {
						result.add(p.getId());
					}
				}
			}
		}
		return result;
	}
	
	// TODO I have no clue what is happening here
	private boolean unfolding(Place p) {
		boolean first = true;
		Set<Place> cap = null;
		for (Transition pre : p.getPreset()) {
			Set<Place> preset = new HashSet<>(pre.getPreset());
			preset.retainAll(pre.getPostset());
			if (!preset.isEmpty()) {
				return true;
			}
			if (first) {
				cap = new HashSet<>(pre.getPreset());
				first = true;
			} else {
				cap.retainAll(pre.getPreset());
			}
			if (cap.isEmpty()) {
				return true;
			}
			for (Place prePre : pre.getPreset()) {
				if (pg.getGame().getEnvPlaces().contains(prePre)) {
					return true;
				}
			}
		}
		return false;
	}
}
