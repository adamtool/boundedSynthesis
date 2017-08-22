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

public class ForNonDeterministicUnfolder extends WhileNonDeterministicUnfolder {
	
	public Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = new HashMap<>(); // Map for QCIRbuilder to include additional information
	Set<String> closed = new HashSet<>();

	public ForNonDeterministicUnfolder(QBFPetriGame QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
		this.max = max;
		this.pn = QBFPetriGame.getNet();
	}

	@Override
	public void createUnfolding(Map<String, Integer> b) throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		// Initialize counter for unfolding
		Set<Place> places = new HashSet<>(pn.getPlaces());
		for (Place p : places) {
			current.put(p.getId(), 1);
		}
		limit = b;
		
		Map<Place, LinkedList<Integer>> orderOfUnfolding = calculateOrderOfUnfolding();
		for (int i = 2; i <= pg.getN(); ++i) {
			for (Place p : places) {				// TODO only here a hashCode is important anymore
				LinkedList<Integer> list = orderOfUnfolding.get(p);
				if (list.size() > 0 && list.getFirst() == i) {
					checkPlaceForUnfolding(p, false);
					while (list.size() > 0 && list.getFirst() == i) {
						list.removeFirst();
					}
				}
			}
		}
		
		// add additional system places to unfolded env transitions
		addAdditionalSystemPlaces();
	}

	private Map<Place, LinkedList<Integer>> calculateOrderOfUnfolding() {
		Map<Place, LinkedList<Integer>> orderOfUnfolding = new HashMap<>();
		Marking initial = pn.getInitialMarking();
		
		for (Place p : pn.getPlaces()) {
			LinkedList<Integer> list = new LinkedList<>();
			orderOfUnfolding.put(p, list);
		}
		Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
		queue.add(new Pair<>(initial, 1));
		Set<Marking> closed = new HashSet<>();
		Pair<Marking, Integer> p;
		while ((p = queue.poll()) != null) {
			Marking m = p.getFirst();
			int i = p.getSecond();
			closed.add(m);
			for (Transition t : pn.getTransitions()) {
				if (t.isFireable(m)) {
					Marking next = t.fire(m);
					if (!closed.contains(next)) {
						for (Place place : t.getPostset()) {
							orderOfUnfolding.get(place).add(i + 1);
						}
						if (i + 1 < pg.getN()) {
							queue.add(new Pair<>(next, i + 1));
						}
					}
				}
			}
		}
		return orderOfUnfolding;
	}

}
