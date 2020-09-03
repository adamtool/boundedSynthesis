package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.ds.objectives.Condition;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class ForNonDeterministicUnfolder extends NonDeterministicUnfolder {

	Set<String> closed = new HashSet<>();

	public ForNonDeterministicUnfolder(QbfSolvingObject<? extends Condition<?>> QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
	}

	@Override
	protected void createUnfolding() {
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
