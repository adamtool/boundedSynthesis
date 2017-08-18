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
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolver;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class WhileNonDeterministicUnfolder extends Unfolder {

	Set<Place> placesWithCopiedTransitions = new HashSet<>(); // Maintained during unfolding in order to afterwards add additional places
	public Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = new HashMap<>(); // Map for QCIRbuilder to include additional information
	Set<String> closed = new HashSet<>();
	Queue<String> placesToUnfold;

	public WhileNonDeterministicUnfolder(QBFPetriGame QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame);
		this.max = max;
		this.pn = QBFPetriGame.getNet();
	}

	@Override
	public void createUnfolding(Map<String, Integer> b) throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		// Initialize counter for unfolding
		for (Place p : pg.getNet().getPlaces()) {
			current.put(p.getId(), 1);
		}
		limit = b;

		// Initialize queue
		placesToUnfold = initializeQueue();
		// begin unfolding
		while (!placesToUnfold.isEmpty()) {
			String id = placesToUnfold.poll();
			Place p = pn.getPlace(id);
			unfoldPlace(p);
		}

		// add additional system places to unfolded env transitions
		addAdditionalSystemPlaces();
	}

	protected void unfoldPlace(Place p) {
		String p_id = getTruncatedId(p.getId());
		if (closed.contains(p.getId()))
			return;
		closed.add(p.getId());

		// store information for non-deterministic unfold of self-loops
		Set<Place> copies = new HashSet<>();
		Set<Transition> selfloops = new HashSet<>(); // transitions go and return to place p but pre- and postSet are NOT the same: interconnection via selfloops necessary
		Set<Transition> totalSelfLoops = new HashSet<>(); // pre- and postSet are the same: interconnection via selfloops NOT necessary

		// only unfold transitions which were originally present (i.e. only transitions with
		// same ID as truncated ID) plus some additional test satisfier
		// in originalPREset
		Set<Transition> p_originalPreset = new HashSet<>(); 
		// originalPreset/Postset makes CM only solvable in the first place
		// getPreset makes test.apt winning with wrong strategy
		for (Transition t : p.getPreset()) {
			if (t.getId().equals(getTruncatedId(t.getId()))) {
				p_originalPreset.add(t);
			}
		}

		// **** ADDITIONAL ADD TEST 1 ****
		// erkennt wenn lokale Transitionen Historie transportieren da sie unfoldet wurden:
		// wenn die selbe LOKALE transition (gleiche truncated ID) von 2 unterschiedlichen
		// UNFOLDETEN plätzen kommt (gleiche truncated ID), dann doch unfold, wenn
		// partner noch nicht geaddet (EGAL ob env oder sys)
		// second iteration says that this works as intended
		for (Transition t1 : p.getPreset()) {
			if (!t1.getId().equals(getTruncatedId(t1.getId()))) { // unfoldete Transition t1
				for (Transition t2 : p.getPreset()) { // search for partner
					if (getTruncatedId(t1.getId()).equals(getTruncatedId(t2.getId()))) // LOKALER partner
						if (t1.getPreset().size() == 1 && t2.getPreset().size() == 1 && // local transition
								t2.getPostset().size() == 1 && t2.getPostset().size() == 1)
							if (!t1.getPreset().toArray()[0].equals(t2.getPreset().toArray()[0])) {// from DIFFERENT places
								p_originalPreset.add(t1);
								break;
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

		// every outgoing transition is necessary
		// originalPOSTset
		Set<Transition> p_originalPostset = new HashSet<>();
		for (Transition t : p.getPostset()) {
			p_originalPostset.add(t);
		}

		// how often can p be unfolded? as often as needed (first part of min) or not as often as needed (second part of min)
		int max;
		Marking in = pn.getInitialMarking();
		if (isInitialPlace(p, in)) {
			max = Math.min(p_originalPreset.size(), limit.get(p_id) - current.get(p_id));
		} else {
			max = Math.min(p_originalPreset.size() - 1, limit.get(p_id) - current.get(p_id));
		}

		// find self-loops
		for (Transition pre : p_originalPreset) {
			// we can neglect unfolding of transitions consisting only of selfloops (second part) BUT every copied place needs that selfloop
			if (p_originalPostset.contains(pre)) {
				if (!pre.getPreset().equals(pre.getPostset())) {
					selfloops.add(pre);
				} else {
					totalSelfLoops.add(pre);
				}
			}
		}
		
		// actual unfolding
		for (int i = 0; i < max; ++i) {
			// create new place
			Place newP = pg.getNet().createPlace(p_id + "__" + current.get(p_id));
			copies.add(newP);
			current.put(p_id, current.get(p_id) + 1);
			copyBadAndEnv(newP, p);
			//copyTotalSelfLoops(newP, totalSelfLoops);
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
						placesToUnfold.add(postPost.getId()); // TODO this only makes sense for one type of unfolder, namely the while thing
					}
				}
			}
		}

		// interconnect via self-loops
		if (!selfloops.isEmpty()) {
			// from p to all unfolded places via all selfloops
			for (Place p2 : copies) {
				for (Transition loop : selfloops) {
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
			// TODO outcommented following part
			/*for (Place p1 : copies) {
				// add self-loop to each unfolded place; we need these self-loops b/c the winning strategy 
				// might depend on infinite loop in self-loop 
				// we can ignore self-loops which do not produce token but require more than one token 
				// these self-loops can only be fireable infinitely often if the game is not safe 
				if (!p1.equals(p)) { 
					for (Transition self : selfloops) { 
						if (!(self.getPreset().size() >= 2 && self.getPostset().size() == 1)) { 
							String self_id = getTruncatedId(self.getId()); 
							Transition newT = pg.getNet().createTransition(self_id + "__" + getCopyCounter(self_id));
							for (Place postPre : self.getPreset()) { 
								if (postPre.equals(p)) { 
									pg.getNet().createFlow(p1, newT); 
								} else { 
									pg.getNet().createFlow(postPre, newT); 
								} 
							}
							for (Place prePost : self.getPostset()) { 
								if (prePost.equals(p)) { 
									pg.getNet().createFlow(newT, p1); 
								} else { 
									pg.getNet().createFlow(newT, prePost); 
								} 
							} 
						} 
					} 
				}
			  
				// from each unfolded place to each unfolded place AND p (p to all unfolded places already done before) 
				for (Place p2 : copies) { 
					if (!p1.equals(p2)) { 
						for (Transition loop : selfloops) { 
							String loop_id = getTruncatedId(loop.getId()); 
							Transition newT = pg.getNet().createTransition(loop_id + "__" + getCopyCounter(loop_id));
							for (Place postPre : loop.getPreset()) { 
								if (postPre.equals(p)) { 
									pg.getNet().createFlow(p1, newT); 
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
			  }*/
			 
		}
		// find transitions consisting only of self-loops 
		/*Set<Transition> onlyLoops = new HashSet<>(); 
		for (Transition pre : p_originalPreset) { 
			System.out.println(pre.getPreset() + " - " + pre.getPostset()); 
			if (pre.getPreset().equals(pre.getPostset())) { // only selfloops
				onlyLoops.add(pre); 
			} 
		} 
		for (Transition t : onlyLoops) { 
			for (Place copy : copies) { 
				String t_id = getTruncatedId(t.getId()); 
				Transition newT = pg.getNet().createTransition(t_id + "__" + getCopyCounter(t_id)); 
				if (getTruncatedId(copy.getId()).equals(getTruncatedId(p.getId()))) { // add transition t with only loops to unfolded place copy 
					for (Place pre : t.getPreset()) { 
						if (pre.equals(p)) { 
							pn.createFlow(copy, newT); 
							pn.createFlow(newT, copy); 
						} else { 
							pn.createFlow(pre, newT); 
							pn.createFlow(newT, pre); 
						} 
					} 
				} 
			} 
		}*/
	}

	protected void addAdditionalSystemPlaces() {
		for (Place p : placesWithCopiedTransitions) {
			// with (S3) encoded in exists necessary for all places
			Transition[] transitions = p.getPostset().toArray(new Transition[0]);

			Set<Pair<String,Set<Place>>> truncatedIDsAndPreset = new HashSet<>(); // truncated IDs of transitions
			// outgoing transitions for match according to truncated id
			for (int i = 0; i < transitions.length; ++i) {
				Transition t = transitions[i];
				String trunc_id = getTruncatedId(t.getId());
				if (!truncatedIDsAndPreset.contains(new Pair<>(trunc_id, t.getPreset())) /*&& !reachesBadPlace(t)*/ && !containsAdditionalSystemPlace(t.getPreset())) {				// TODO does reachesBadPlace change anything?
					Set<Transition> otherTransitions = new HashSet<>();
					for (int j = i + 1; j < transitions.length; ++j) {
						if (getTruncatedId(transitions[j].getId()).equals(trunc_id) && transitions[j].getPreset().equals(t.getPreset()) /*&& !reachesBadPlace(transitions[j])*/) {	// TODO does reachesBadPlace change anything?
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

	// TODO this is specific to safety, should unfolder be specific to something
	/*private boolean reachesBadPlace(Transition t) {
		for (Place post : t.getPostset()) {
			if (pg.getBadPlaces().contains(post)) {
				return true;
			}
		}
		return false;
	}*/

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

	private Queue<String> initializeQueue() throws UnboundedException, NetNotSafeException {
		// USING PGsimplifier here takes WAY to long TODO PGsimplifier performance issues?!
		// THEORY: 2 transitions from only (sich gegenseitig ausschließen) sys places to the same place do not require unfolding
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
}
