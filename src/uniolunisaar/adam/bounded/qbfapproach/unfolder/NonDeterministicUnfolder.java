package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public abstract class NonDeterministicUnfolder extends Unfolder {

	protected Set<Place> placesWithCopiedTransitions = new HashSet<>(); // Maintained during unfolding in order to afterwards add additional places
	
	public NonDeterministicUnfolder(QBFSolvingObject<? extends WinningCondition> petriGame, Map<String, Integer> max) {
		super(petriGame, max);
	}
	
	protected Set<String> unfoldPlace(Place p) {
		String p_id = getTruncatedId(p.getId());
		if (closed.contains(p.getId())) {
			return new HashSet<>();
		}
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
		return unfoldPlace(p, p_id, p_originalPreset, p_originalPostset);
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
						Place newSysPlace = pg.getGame().createPlace(QbfControl.additionalSystemName + t.getId() + QbfControl.additionalSystemUniqueDivider + p.getId());
						newSysPlace.setInitialToken(1);
						// add transition the unfolding is based on BEFORE requiring system to decide for at least one
						otherTransitions.add(t);
						systemHasToDecideForAtLeastOne.put(newSysPlace, otherTransitions);
						// add arrows between tokens and transitions
						for (Transition trans : otherTransitions) {
							pn.createFlow(newSysPlace, trans);
							pn.createFlow(trans, newSysPlace);
                                                        Map<Transition, Set<Pair<Place, Place>>> map = pg.getFl();
							map.get(trans).add(new Pair<> (newSysPlace, newSysPlace));
						}
					}
				}
				truncatedIDsAndPreset.add(new Pair<>(getTruncatedId(t.getId()), t.getPreset()));
			}
		}
	}

	private boolean containsAdditionalSystemPlace(Set<Place> preset) {
		for (Place p : preset) {
			if (p.getId().startsWith(QbfControl.additionalSystemName)) {
				return true;
			}
		}
		return false;
	}

	protected Set<String> unfoldPlace(Place p, String p_id, Set<Transition> p_originalPreset, Set<Transition> p_originalPostset) {
		Set<String> placesToUnfold = new HashSet<>();
		// how often can p be unfolded? as often as needed (first part of min) or not as often as needed (second part of min)
		int limit = getLimitValue(p) - getCurrentValue(p);
		int required = (int)(p_originalPreset.size() + p.getInitialToken().getValue() - 1);
		int maxNumberOfUnfoldings = Math.min(limit, required);

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
			Place newP = pg.getGame().createPlace(p_id + "__" + current.get(p_id));
			copies.add(newP);
			current.put(p_id, current.get(p_id) + 1);
			for (Pair<String, Object> pair : p.getExtensions()) {
				newP.putExtension(pair.getFirst(), pair.getSecond());
			}
			copyEnv(newP, p);
			// copyTotalSelfLoops(newP, totalSelfLoops);
			// copy incoming transitions (except self-loops)
			HashSet<Transition> preSet = new HashSet<>(p_originalPreset);
			preSet.removeAll(p_originalPostset);
			for (Transition pre_transition : preSet) {
				Transition newT = copyTransition(pre_transition);
				for (Place prePre : pre_transition.getPreset()) {
					pg.getGame().createFlow(prePre, newT);
					placesWithCopiedTransitions.add(prePre);
				}

				for (Place prePost : pre_transition.getPostset()) {
					if (prePost.equals(p)) {
						pg.getGame().createFlow(newT, newP);
					} else {
						pg.getGame().createFlow(newT, prePost);
					}
				}
				
                Map<Transition, Set<Pair<Place, Place>>> map = pg.getFl();
				for (Pair<Place, Place> fl : map.get(pre_transition)) {
					Place first = fl.getFirst();
					Place second = fl.getSecond();
					if (second.equals(p)) {
						map.get(newT).add(new Pair<>(first, newP));
					} else {
						map.get(newT).add(new Pair<>(first, second));
					}
				}
			}

			// copy outgoing transitions (except self-loops)
			HashSet<Transition> postSet = new HashSet<>(p_originalPostset);
			// postSet.removeAll(p_originalPreset);
			for (Transition post_transition : postSet) {
				Transition newT = copyTransition(post_transition);
				for (Place postPre : post_transition.getPreset()) {
					if (postPre.equals(p)) {
						pg.getGame().createFlow(newP, newT);
					} else {
						pg.getGame().createFlow(postPre, newT);
					}
				}

				for (Place postPost : post_transition.getPostset()) {
					pg.getGame().createFlow(newT, postPost);
				}
				
                Map<Transition, Set<Pair<Place, Place>>> map = pg.getFl();
				for (Pair<Place, Place> fl : map.get(post_transition)) {
					Place first = fl.getFirst();
					Place second = fl.getSecond();
					if (first.equals(p)) {
						map.get(newT).add(new Pair<>(newP, second));
					} else {
						map.get(newT).add(new Pair<>(first, second));
					}
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
			// TODO was hat es mit p2 und p weiter unten auf sich?
			for (Place p2 : copies) {
				for (Transition loop : selfLoops) {
					Transition newT = copyTransition(loop);

					for (Place postPre : loop.getPreset()) {
						placesWithCopiedTransitions.add(postPre);
						pg.getGame().createFlow(postPre, newT);
					}

					for (Place prePost : loop.getPostset()) {
						if (prePost.equals(p)) {
							pg.getGame().createFlow(newT, p2);
						} else {
							pg.getGame().createFlow(newT, prePost);
						}
					}
					
                    Map<Transition, Set<Pair<Place, Place>>> map = pg.getFl();
					for (Pair<Place, Place> fl : map.get(loop)) {
						Place first = fl.getFirst();
						Place second = fl.getSecond();
						if (first.equals(p)) {
							map.get(newT).add(new Pair<>(first, p2));
						} else {
							map.get(newT).add(new Pair<>(first, second));
						}
					}
				}
			}

			copies.add(p);
		}
		return placesToUnfold;
	}
}
