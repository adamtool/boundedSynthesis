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
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class WhileNonDeterministicUnfolder extends NonDeterministicUnfolder {
	private Queue<String> placesToUnfold;

	public WhileNonDeterministicUnfolder(QBFPetriGame QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
	}

	@Override
	public void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		// Initialize counter for unfolding
		for (Place p : pg.getNet().getPlaces()) {
			current.put(p.getId(), 1);
		}

		// Initialize queue
		placesToUnfold = initializeQueue();
		// begin unfolding
		while (!placesToUnfold.isEmpty()) {
			String id = placesToUnfold.poll();
			Place p = pn.getPlace(id);
			//placesToUnfold.addAll(checkPlaceForUnfolding(p));
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
					if (p.getPostset().size() >= 1 && !additionalCheck(p)) {
						result.add(p.getId());
					}
				}
			}
		}

		// add initial places which can be re-reached via loop
		for (Place p : pn.getPlaces()) {
			if (in.getToken(p).getValue() >= 1) {
				if (p.getPreset().size() >= 1) {
					if (p.getPostset().size() >= 1 && !additionalCheck(p)) {
						result.add(p.getId());
					}
				}
			}
		}
		return result;
	}
	
	private boolean additionalCheck(Place p) {
		boolean first = true;
		Set<Place> cup = new HashSet<>();
		for (Transition pre : p.getPreset()) {
			Set<Place> preset = new HashSet<>(pre.getPreset());
			preset.retainAll(pre.getPostset());
			if (!preset.isEmpty()) {
				return false;
			}
			if (first) {
				cup = new HashSet<>(pre.getPreset());
				first = false;
			} else {
				cup.retainAll(pre.getPreset());
			}
			if (cup.isEmpty()) {
				return false;
			}
			for (Place prePre : pre.getPreset()) {
				if (pg.getEnvPlaces().contains(prePre)) {
					return false;
				}
			}
		}
		return true;
	}
}
