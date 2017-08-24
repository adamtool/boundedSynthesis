package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolver;

public abstract class NonDeterministicUnfolder extends Unfolder {

	protected Set<String> closed = new HashSet<>();
	protected Set<Place> placesWithCopiedTransitions = new HashSet<>(); // Maintained during unfolding in order to afterwards add additional places
	public Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = new HashMap<>(); // Map for QCIRbuilder to include additional information

	public NonDeterministicUnfolder(QBFPetriGame petriGame, Map<String, Integer> max) {
		super(petriGame, max);
	}

	// returns places for further unfolding, may be ignored
	protected void addAdditionalSystemPlaces() {
		for (Place p : placesWithCopiedTransitions) {
			// with (S3) encoded in exists necessary for all places
			Transition[] transitions = p.getPostset().toArray(new Transition[0]);

			Set<Pair<String, Set<Place>>> truncatedIDsAndPreset = new HashSet<>(); // truncated IDs of transitions
			// outgoing transitions for match according to truncated id
			for (int i = 0; i < transitions.length; ++i) {
				Transition t = transitions[i];
				String trunc_id = getTruncatedId(t.getId());
				if (!truncatedIDsAndPreset.contains(new Pair<>(trunc_id, t.getPreset())) && !containsAdditionalSystemPlace(t.getPreset())) {
					Set<Transition> otherTransitions = new HashSet<>();
					for (int j = i + 1; j < transitions.length; ++j) {
						if (getTruncatedId(transitions[j].getId()).equals(trunc_id) && transitions[j].getPreset().equals(t.getPreset())) {
							otherTransitions.add(transitions[j]);
						}
					}
					if (otherTransitions.size() > 0) {
						// create additional system place and place token
						Place newSysPlace = pg.getNet().createPlace(QBFSolver.additionalSystemName + t.getId() + "__" + p.getId());
						newSysPlace.setInitialToken(1);
						// add transition the unfolding is based on BEFORE requiring system to decide for at least one
						otherTransitions.add(t);
						systemHasToDecideForAtLeastOne.put(newSysPlace, otherTransitions);
						// add arrows between tokens and transitions
						for (Transition trans : otherTransitions) {
							pn.createFlow(newSysPlace, trans);
							pn.createFlow(trans, newSysPlace);
						}
					}
				}
				truncatedIDsAndPreset.add(new Pair<>(getTruncatedId(t.getId()), t.getPreset()));
			}
		}
	}

	private boolean containsAdditionalSystemPlace(Set<Place> preset) {
		for (Place p : preset) {
			if (p.getId().startsWith(QBFSolver.additionalSystemName)) {
				return true;
			}
		}
		return false;
	}
	
	protected void /*Set<String>*/ checkPlaceForUnfolding(Place p) {
		String p_id = getTruncatedId(p.getId());
		if (closed.contains(p.getId()))
			return;// new HashSet<>();
		closed.add(p.getId());

		// only unfold transitions which were originally present (i.e. only transitions with same ID as truncated ID) plus some additional test satisfier in originalPREset
		Set<Transition> p_originalPreset = new HashSet<>();
		// originalPreset/Postset makes CM only solvable in the first place
		// getPreset makes test.apt winning with wrong strategy
		for (Transition t : p.getPreset()) {
			if (t.getId().equals(getTruncatedId(t.getId()))) {
				p_originalPreset.add(t);
			}
		}

		// Certain patterns require that SOME unfolded transitions are also part of the "original" preset
		addTransitionsToOriginalPreset(p, p_originalPreset);

		// every outgoing transition is necessary
		// originalPOSTset
		Set<Transition> p_originalPostset = new HashSet<>();
		for (Transition t : p.getPostset()) {
			p_originalPostset.add(t);
		}

		// decide unfolding of place
		/*return*/ unfoldPlace(p, p_id, p_originalPreset, p_originalPostset);
	}

	protected void/*Set<String>*/ unfoldPlace(Place p, String p_id, Set<Transition> p_originalPreset, Set<Transition> p_originalPostset) {
		Set<String> placesToUnfold = new HashSet<>();
		// how often can p be unfolded? as often as needed (first part of min) or not as often as needed (second part of min)
		int maxNumberOfUnfoldings;
		Marking in = pn.getInitialMarking();
		if (isInitialPlace(p, in)) {
			maxNumberOfUnfoldings = Math.min(p_originalPreset.size(), limit.get(p_id) - current.get(p_id));
		} else {
			maxNumberOfUnfoldings = Math.min(p_originalPreset.size() - 1, limit.get(p_id) - current.get(p_id));
		}

		Set<Place> copies = new HashSet<>();
		// store information for non-deterministic unfold of self-loops
		Set<Transition> selfLoops = new HashSet<>(); // transitions go and return to place p but pre- and postSet are NOT the same: interconnection via selfloops necessary
		// find self-loops
		for (Transition pre : p_originalPreset) {
			// we can neglect unfolding of transitions consisting only of selfloops (second part) BUT every copied place needs that selfloop
			if (p_originalPostset.contains(pre)) {
				if (!pre.getPreset().equals(pre.getPostset())) {
					selfLoops.add(pre);
				} // else case: pre- and postSet are the same: interconnection via selfloops NOT necessary BECAUSE infinite looping can always be taken in original places
			}
		}

		// actual unfolding
		for (int i = 0; i < maxNumberOfUnfoldings; ++i) {
			// create new place
			Place newP = pg.getNet().createPlace(p_id + "__" + current.get(p_id));
			copies.add(newP);
			current.put(p_id, current.get(p_id) + 1);
			copyBadAndEnv(newP, p);
			// copyTotalSelfLoops(newP, totalSelfLoops);
			// copy incoming transitions (except self-loops)
			HashSet<Transition> preSet = new HashSet<>(p_originalPreset);
			preSet.removeAll(p_originalPostset);
			for (Transition pre_transition : preSet) {
				String pre_id = getTruncatedId(pre_transition.getId());
				Transition newT = pg.getNet().createTransition(pre_id + "__" + getCopyCounter(pre_id));
				for (Place prePre : pre_transition.getPreset()) {
					pg.getNet().createFlow(prePre, newT);
					placesWithCopiedTransitions.add(prePre);
				}

				for (Place prePost : pre_transition.getPostset()) {
					if (prePost.equals(p)) {
						pg.getNet().createFlow(newT, newP);
					} else {
						pg.getNet().createFlow(newT, prePost);
					}
				}
			}

			// copy outgoing transitions (except self-loops)
			HashSet<Transition> postSet = new HashSet<>(p_originalPostset);
			// postSet.removeAll(p_originalPreset);
			for (Transition post_transition : postSet) {
				String post_id = getTruncatedId(post_transition.getId());
				Transition newT = pg.getNet().createTransition(post_id + "__" + getCopyCounter(post_id));
				for (Place postPre : post_transition.getPreset()) {
					if (postPre.equals(p)) {
						pg.getNet().createFlow(newP, newT);
					} else {
						pg.getNet().createFlow(postPre, newT);
					}
				}

				for (Place postPost : post_transition.getPostset()) {
					pg.getNet().createFlow(newT, postPost);
				}
			}

			for (Transition t : p_originalPostset) {
				for (Place postPost : t.getPostset()) {
					if (postPost.getPreset().size() >= 2 && postPost.getPostset().size() >= 1 && !closed.contains(postPost.getId())) {
						placesToUnfold.add(postPost.getId());
					}
				}
			}
		}

		// interconnect via self-loops
		if (!selfLoops.isEmpty()) {
			// from p to all unfolded places via all selfloops
			for (Place p2 : copies) {
				for (Transition loop : selfLoops) {
					String loop_id = getTruncatedId(loop.getId());
					Transition newT = pg.getNet().createTransition(loop_id + "__" + getCopyCounter(loop_id));

					for (Place postPre : loop.getPreset()) {
						placesWithCopiedTransitions.add(postPre);
						if (postPre.equals(p)) {
							pg.getNet().createFlow(p, newT);
						} else {
							pg.getNet().createFlow(postPre, newT);
						}
					}

					for (Place prePost : loop.getPostset()) {
						if (prePost.equals(p)) {
							pg.getNet().createFlow(newT, p2);
						} else {
							pg.getNet().createFlow(newT, prePost);
						}
					}
				}
			}

			copies.add(p);
		}
		//return placesToUnfold;
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
}
