package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.analysis.exception.UnboundedException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public class ForNonDeterministicUnfolder extends NonDeterministicUnfolder {

	Set<String> closed = new HashSet<>();

	public ForNonDeterministicUnfolder(QBFSolvingObject<? extends WinningCondition> QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
	}

	@Override
	protected void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		Set<Place> places = new HashSet<>(pn.getPlaces());
		Map<String, LinkedList<Integer>> orderOfUnfolding = calculateOrderOfUnfoldingBasedOnGameSimulation();
		for (int i = 2; i <= pg.getN(); ++i) {
			for (Place p : places) { // TODO hashCode determines order; order according to size of preset or postset
				LinkedList<Integer> list = orderOfUnfolding.get(p.getId());
				if (list.size() > 0 && list.getFirst() == i) {
					if (unfoldConditionSatisfied(p)) {
						unfoldPlace(p); // ignore returned places as no queue is used
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
