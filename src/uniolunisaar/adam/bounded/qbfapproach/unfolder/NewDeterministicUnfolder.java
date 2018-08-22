package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolver;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

public class NewDeterministicUnfolder extends Unfolder {

	public NewDeterministicUnfolder(QBFSolvingObject petriGame, Map<String, Integer> max) {
		super(petriGame, max);
	}

	@Override
	public void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		Map<String, LinkedList<Integer>> orderOfUnfolding = calculateOrderOfUnfoldingBasedOnGameSimulation();
		Map<Place, Set<Transition>> originalPresetMap = new HashMap<>();
		for (int i = 2; i <= pg.getN(); ++i) {
			// update originalPresetMap
			for (Place p : pn.getPlaces()) {
				originalPresetMap.put(p, p.getPreset());	// TODO decide on using all or only additionalTest
				/*Set<Transition> originalPreset = getOriginalPreset(p);
				addTransitionsToOriginalPreset(p, originalPreset);
				originalPresetMap.put(p, originalPreset);*/
			}
			Set<Place> places = new HashSet<>(pn.getPlaces());
			for (Place p : places) {						// TODO only here a hashCode is important anymore
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					LinkedList<Integer> list = orderOfUnfolding.get(getTruncatedId(p.getId()));
					if (list.size() > 0 && list.getFirst() == i) {
						if (unfoldConditionSatisfied(p) && !closed.contains(p.getId())) {	// TODO I removed additional weird !noUnfold() here
							unfoldPlaceDeterministically(p, originalPresetMap.get(p));	// ignore returned places as no queue is used
							//closed.add(p.getId());	// TODO it might be useful to not have this?
						}
						while (list.size() > 0 && list.getFirst() == i) {
							list.removeFirst();
						}
					}
				}
			}
		}
	}
	
	private void unfoldPlaceDeterministically(Place p, Set<Transition> p_originalPreset) {
		int limit = getLimitValue(p) - getCurrentValue(p);
		int required = (int)(p_originalPreset.size() + p.getInitialToken().getValue() - 1);
		int maxNumberOfUnfoldings = Math.min(limit, required); 
		int numberOfUnfoldings = 0;
		for (Transition pre : p_originalPreset) { // TODO use ordering of transitions based on simulation
			if (numberOfUnfoldings < maxNumberOfUnfoldings) {
				Place newP = copyPlace(p);
				pn.removeFlow(pre, p);
				pn.createFlow(pre, newP);
				increaseCurrentValue(p);
				numberOfUnfoldings++;
			} else if (numberOfUnfoldings == maxNumberOfUnfoldings) {
				// skip the else for cases where the original places suffices for unfolding
				numberOfUnfoldings++;
			}
			else {
				// upon reaching the bound we have to have the possibility to return to all unfolded copies
				Set<Transition> copies = new HashSet<>();
				copies.add(pre);
				for (Place place : pn.getPlaces()) {
					if (getTruncatedId(p.getId()).equals(getTruncatedId(place.getId())) && !p.equals(place)) { // place is unfolded copy of p
						Transition newT = copyTransition(pre);
						for (Place pre_pre : pre.getPreset()) {
							pn.createFlow(pre_pre, newT);
						}
						for (Place post_pre : pre.getPostset()) {
							if (getTruncatedId(post_pre.getId()).equals(getTruncatedId(place.getId()))) {
								pn.createFlow(newT, place);
							} else {
								pn.createFlow(newT, post_pre);
							}
						}
						copies.add(newT);
					}
				}
				if (copies.size() > 0) {
					// create additional system place and place token
					Place newSysPlace = pg.getGame().createPlace(QBFSolver.additionalSystemName + pre.getId() + QBFSolver.additionalSystemUniqueDivider + p.getId());
					newSysPlace.setInitialToken(1);
					systemHasToDecideForAtLeastOne.put(newSysPlace, copies);
					// add arrows between tokens and transitions
					for (Transition trans : copies) {
						pn.createFlow(newSysPlace, trans);
						pn.createFlow(trans, newSysPlace);
					}
				}
			}
		}
	}
}
