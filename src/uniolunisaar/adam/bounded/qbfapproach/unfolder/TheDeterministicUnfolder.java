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

public class TheDeterministicUnfolder extends Unfolder {

	public TheDeterministicUnfolder(QBFPetriGame petriGame, Map<String, Integer> max) {
		super(petriGame, max);
	}

	@Override
	protected void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		Marking initial = pn.getInitialMarking();
		Set<Pair<Transition, Place>> sameHistory = new HashSet<>();
		Set<Place> intialPlaces = new HashSet<>();
		for (Place p : pn.getPlaces()) {
			if (initial.getToken(p).getValue() > 0) {
				sameHistory.add(new Pair<Transition, Place>(null, p));
				intialPlaces.add(p);
			}
		}
		Set<Set<Pair<Transition, Place>>> extendedMarking = new HashSet<>();
		extendedMarking.add(sameHistory);
		Pair<Marking, Set<Set<Pair<Transition, Place>>>> markingPair = new Pair<>(initial, extendedMarking);

		Queue<Pair<Pair<Marking, Set<Set<Pair<Transition, Place>>>>, Integer>> queue = new LinkedList<>();
		queue.add(new Pair<>(markingPair, 1));
		
		// all the histories each original place can be reached with
		Map<Place, Set<Set<Place>>> histories = new HashMap<>();
		for (Place p : pn.getPlaces()) {
			Set<Set<Place>> history = new HashSet<>();
			history.add(intialPlaces);
			histories.put(p, history);
		}
		// each original place and the history it can be reached with point to the (possible unfolded) place to represent it
		Map<Pair<Place, Set<Place>>, Place> unfoldedPlaceForHistory = new HashMap<>();
		// all reached history markings to make decision on unfolding
		Set<Set<Set<Pair<Transition, Place>>>> allHistoryMarkings = new HashSet<>();
		
		Set<Marking> closed = new HashSet<>();
		Pair<Pair<Marking, Set<Set<Pair<Transition, Place>>>>, Integer> element;
		int i;
		Marking marking;
		Set<Set<Pair<Transition, Place>>> historyMarking;
		while ((element = queue.poll()) != null) {
			i = element.getSecond();
			marking = element.getFirst().getFirst();
			historyMarking = element.getFirst().getSecond();
			closed.add(marking);
			for (Transition t : pn.getTransitions()) {
				if (t.isFireable(marking)) {
					Marking nextMarking = t.fire(marking);
					// update history marking
					Set<Set<Pair<Transition, Place>>> nextHistoryMarking = new HashSet<>(historyMarking);
					// remove preset
					for (Place pre : t.getPreset()) {
						for (Set<Pair<Transition, Place>> set : historyMarking) {
							for (Pair<Transition, Place> pair : set) {
								if (pair.getSecond().equals(pre)) {
									nextHistoryMarking.remove(pair);
								}
							}
						}
					}
					// add postset
					Set<Pair<Transition, Place>> postSet = new HashSet<>();
					for (Place post : t.getPostset()) {
						postSet.add(new Pair<> (t, post));
					}
					nextHistoryMarking.add(postSet);
					
					// TODO decide unfolding
					for (Place post : t.getPostset()) {
						Place post_original = pn.getPlace(getTruncatedId(post.getId()));
						Set<Set<Place>> history = histories.get(post_original);
						Set<Place> repeatedHistory = null;
						for (Set<Place> places : history) {
							if (places.containsAll(t.getPostset()) || t.getPostset().containsAll(places)) {
								repeatedHistory = places;
								break;
							}
						}
						if (repeatedHistory != null) {
							// already existing place
							
						} else {
							// new place
							
						}
					}
					
					// 
					if (i + 1 < pg.getN()) {
						queue.add(new Pair<>(new Pair<>(nextMarking, nextHistoryMarking), i + 1));
					}
					// TODO i + 1 = n, looping backwards with additional system places??? 
				}
			}
		}
		
		// TODO Auto-generated method stub
		
	}

}
