package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public abstract class Unfolder {

	// PetriGame which will be unfolded
	protected QBFPetriGame pg;
	protected PetriNet pn;

	// how much unfolding of places was done and how much can still be done
	protected Map<String, Integer> current = new HashMap<>();
	protected Map<String, Integer> limit = null;
	
	// NewDeterministicUnfolder also uses this
	public Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = new HashMap<>(); // Map for QCIRbuilder to include additional information
	
	// Counter to make copied transitions unique, places use numbers from current
	protected Map<String, Integer> copycounter_map = new HashMap<>();
	
	protected Set<String> closed = new HashSet<>();
	
	public Unfolder(QBFPetriGame petriGame, Map<String, Integer> max) {
		this.pg = petriGame;
		this.pn = pg.getNet();
		this.limit = max;
	}

	public void prepareUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		for (Place p : pn.getPlaces()) {
			current.put(p.getId(), 1);
		}
		
		if (limit != null) {
			for (Place p : pn.getPlaces()) {
				if (limit.get(p.getId()) == null) {
					limit.put(p.getId(), 0);
				}
			}
		} else {
			limit = new HashMap<>();
			for (Place p : pn.getPlaces()) {
				limit.put(p.getId(), pg.getB());
			}
		}
		createUnfolding();
	}

	protected abstract void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException;

	protected Map<String, LinkedList<Integer>> calculateOrderOfUnfoldingBasedOnGameSimulation() {
		Map<String, LinkedList<Integer>> orderOfUnfolding = new HashMap<>();
		Marking initial = pn.getInitialMarking();
		
		for (Place p : pn.getPlaces()) {
			LinkedList<Integer> list = new LinkedList<>();
			orderOfUnfolding.put(p.getId(), list);
		}
		Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
		queue.add(new Pair<>(initial, 1));
		Set<Marking> closed = new HashSet<>();
		Pair<Marking, Integer> p;
		int i;
		while ((p = queue.poll()) != null) {
			Marking m = p.getFirst();
			i = p.getSecond();
			closed.add(m);
			for (Transition t : pn.getTransitions()) {
				if (t.isFireable(m)) {
					Marking next = t.fire(m);
					if (!closed.contains(next)) {
						// local SYS transition (i.e. pre- and postset <= 1) cannot produce history, but they CAN TRANSPORT history
						if (t.getPreset().size() > 1 || pg.getEnvTransitions().contains(t) || transportHistory(t, orderOfUnfolding)) {
							for (Place place : t.getPostset()) {
								// only unfold places with outgoing transitions
								if (place.getPostset().size() > 0) {
									orderOfUnfolding.get(place.getId()).add(i + 1);
								}
							}
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
	
	private boolean transportHistory(Transition t, Map<String, LinkedList<Integer>> orderOfUnfolding) {
		if (t.getPreset().size() == 1) {
			Place p = t.getPreset().toArray(new Place[0])[0];
			if (orderOfUnfolding.get(p.getId()).size() > 1) {
				return true;
			}
		}
		return false;
	}
	
	protected Set<Transition> getOriginalPreset (Place p) {
		Set<Transition> originalPreset = new HashSet<>();
		for (Transition t : p.getPreset()) {
			if (t.getId().equals(getTruncatedId(t.getId()))) {
				originalPreset.add(t);
			}
		}
		return originalPreset;
	}
	
	protected void addTransitionsToOriginalPreset(Place p, Set<Transition> p_originalPreset) {
		// TODO additional test 1 & 2 look similar and probably can be combined

		// **** ADDITIONAL ADD TEST 1 ****
		// erkennt wenn lokale Transitionen Historie transportieren da sie unfoldet wurden:
		// wenn die selbe LOKALE transition (gleiche truncated ID) von 2 unterschiedlichen
		// UNFOLDETEN plätzen kommt (gleiche truncated ID), dann doch unfold, wenn
		// partner noch nicht geaddet (EGAL ob env oder sys)
		// second iteration says that this works as intended
		for (Transition t1 : p.getPreset()) {
			if (!t1.getId().equals(getTruncatedId(t1.getId()))) { // unfoldete Transition t1
				for (Transition t2 : p.getPreset()) { // search for partner
					if (getTruncatedId(t1.getId()).equals(getTruncatedId(t2.getId()))) { // LOKALER partner
						if (t1.getPreset().size() == 1 && t2.getPreset().size() == 1 && // local transition
								t2.getPostset().size() == 1 && t2.getPostset().size() == 1) {
							if (!t1.getPreset().toArray()[0].equals(t2.getPreset().toArray()[0])) {// from DIFFERENT places
								p_originalPreset.add(t1);
								break;
							}
						}
					}
				}
			}
		}

		// **** ADDITIONAL ADD TEST 2 ****
		// soll erkennen wenn von lokaler transition transportierte Historie von system
		// genutzt wird
		Set<Transition> already_added = new HashSet<>();
		for (Transition t1 : p.getPreset()) {
			if (!t1.getId().equals(getTruncatedId(t1.getId()))) { // unfoldete Transition
				for (Transition t2 : p.getPreset()) {
					if (getTruncatedId(t1.getId()).equals(getTruncatedId(t2.getId()))) { // original partner
						if (!already_added.contains(t1) && !already_added.contains(t2)) {
							if (t1.getPreset().size() == 2 && t2.getPreset().size() == 2 && // local transition
									t2.getPostset().size() <= 2 && t2.getPostset().size() <= 2) {
								if (checkTransitionPair(t1, t2)) {
									p_originalPreset.add(t1);
									already_added.add(t1);
									break;
								}
							}
						}
					}
				}
			}
		}

		// **** ADDITIONAL ADD TEST 3 ****
		// soll erkennen wenn sync-Transitionen (mit Einschränkung zum nicht unendlich often Feuern)
		// Historie transportieren, test basiert auf original transition
		// scheint evtl den Sinn von originalPreset zu neglecten
		for (Transition t : p.getPreset()) {
			if (!t.getId().equals(getTruncatedId(t.getId()))) { // unfoldete Transition
				if (t.getPreset().size() == t.getPostset().size() + 1) { // TODO 1 hardcoden oder mehr erlauben
					String tTruncID = getTruncatedId(t.getId());
					Transition truncT = pn.getTransition(tTruncID);
					boolean check = true;
					for (Place post : truncT.getPostset()) {
						boolean equal = false;
						for (Place pre : truncT.getPreset()) {
							if (pre.equals(post)) {
								equal = true;
								break;
							}
						}
						if (!equal) {
							check = false;
							break;
						}
					}
					if (check) {
						p_originalPreset.add(t);
					}
				}
			}
		}
	}
	
	private boolean checkTransitionPair(Transition t1, Transition t2) {
		// both transition originate from same system place and another env place is part, respectively
		for (Place p1 : t1.getPreset()) {
			if (!pg.getEnvPlaces().contains(p1)) {
				// system
				boolean sysMatch = false;
				for (Place p2 : t2.getPreset()) {
					if (p1.equals(p2))
						sysMatch = true;
				}
				if (!sysMatch) {
					return false;
				}
			} else {
				// environment
				boolean envTruncatedMatch = false;
				for (Place p2 : t2.getPreset()) {
					if (getTruncatedId(p1.getId()).equals(getTruncatedId(p2.getId()))) {
						envTruncatedMatch = true;
					}
				}
				if (!envTruncatedMatch) {
					return false;
				}
			}
		}
		return true;
	}
	
	protected boolean unfoldConditionSatisfied(Place p) {
		boolean boundNotReached = getCurrentValue(p) < getLimitValue(p);
		boolean preset = p.getPreset().size() >= 2 || (p.getPreset().size() == 1 && p.getInitialToken().getValue() == 1);
		boolean postset = p.getPostset().size() >= 1;
		return boundNotReached && postset && preset;
	}
	
	protected void increaseCurrentValue(Place p) {
		String id = getTruncatedId(p.getId());
		current.put(id, current.get(id) + 1);
	}

	protected int getCurrentValue(Place p) {
		String id = getTruncatedId(p.getId());
		return current.get(id);
	}

	protected int getLimitValue(Place p) {
		String id = getTruncatedId(p.getId());
		return limit.get(id);
	}

	protected Transition copyTransition(Transition t) {
		String id = getTruncatedId(t.getId());
		Transition newT = pg.getNet().createTransition(id + "__" + getCopyCounter(id));
		pg.getFl().put(newT, new HashSet<>());
		for (Pair<String, Object> pair : t.getExtensions()) {
			newT.putExtension(pair.getFirst(), pair.getSecond());
		}
		return newT;
	}
	
	// only used in deterministic unfolder, don't care about flow chains until now
	protected Place copyPlace(Place p) {
		String id = getTruncatedId(p.getId());
		Place ret = pg.getNet().createPlace(id + "__" + current.get(id));
		current.put(id, current.get(id) + 1);
		copyEnv(ret, p);
		for (Transition trans : p.getPostset()) {
			Transition copy = copyTransition(trans);
			for (Place pre : trans.getPreset()) {
				if (pre.equals(p)) {
					pn.createFlow(ret, copy);
				} else {
					pn.createFlow(pre, copy);
				}
			}
			for (Place post : trans.getPostset()) {
				if (post.equals(p)) {
					pn.createFlow(copy, ret);
				} else {
					pn.createFlow(copy, post);
				}
			}
		}
		return ret;
	}
	
	protected void copyEnv(Place newP, Place p) {
		for (Pair<String, Object> pair : p.getExtensions()) {
			newP.putExtension(pair.getFirst(), pair.getSecond());
		}
		if (pg.getEnvPlaces().contains(p)) {
			pg.getEnvPlaces().add(newP);
		}
		// winning conditions are added after unfolding TODO unfolding uses reachbad?
	}

	protected int getCopyCounter(String id) {
		Integer i = copycounter_map.get(id);
		int return_value;
		if (i == null) {
			return_value = 0;
			copycounter_map.put(id, return_value + 1);
		} else {
			return_value = i;
			copycounter_map.put(id, i + 1);
		}
		return return_value;
	}

	public static String getTruncatedId(String id) {
		int index = id.indexOf("__");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}

	protected boolean isInitialPlace(Place p, Marking in) {
		if (in.getToken(p).getValue() >= 1) {
			return true;
		} else {
			return false;
		}
	}

	protected boolean isEnvTransition(Transition t) {
		boolean result = true;
		for (Place p : t.getPreset()) {
			if (!pg.getEnvPlaces().contains(p))
				return false;
		}
		return result;
	}
}
