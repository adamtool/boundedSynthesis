package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

public class ForNonDeterministicUnfolder extends NonDeterministicUnfolder {
	
	Set<String> closed = new HashSet<>();

	public ForNonDeterministicUnfolder(QBFPetriGame QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
	}

	@Override
	protected void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		Set<Place> places = new HashSet<>(pn.getPlaces());
		Map<String, LinkedList<Integer>> orderOfUnfolding = calculateOrderOfUnfoldingBasedOnGameSimulation();
		for (int i = 2; i <= pg.getN(); ++i) {
			for (Place p : places) {						// TODO only here a hashCode is important anymore
				LinkedList<Integer> list = orderOfUnfolding.get(p.getId());
				if (list.size() > 0 && list.getFirst() == i) {
					if (unfoldCondition(p)) {				// TODO I removed !unfoldCondition() here
						checkPlaceForUnfolding(p);			// ignore returned places as no queue is used
					}
					while (list.size() > 0 && list.getFirst() == i) {
						list.removeFirst();
					}
				}
			}
		}
		
		// add additional system places to unfolded env transitions
		addAdditionalSystemPlaces();
	}

}
