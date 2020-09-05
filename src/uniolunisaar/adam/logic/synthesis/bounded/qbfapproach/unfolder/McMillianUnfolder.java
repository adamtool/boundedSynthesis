package uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.unfolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.antlr.v4.runtime.misc.Triple;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolvingObject;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class McMillianUnfolder extends Unfolder {
	// unfolded result Petri game and Petri net
	private final QbfSolvingObject<? extends Condition<?>> originalSolvingObject;
	private final PetriGameWithTransits originalGame;

	private Set<Pair<Transition, Set<Place>>> closed = new HashSet<>(); //add transition only once
	//private Set<Set<Place>> cutOffOriginal = new HashSet<>(); // cutOff based on markings in the original net
	private Set<Set<Place>> cutOffUnfolding = new HashSet<>(); // cutOff based on markings in the build unfolding

	private int counterPlaces = 0;
	private int counterTransitions = 0;
	
	//Map<Set<Place>, Set<Place>> uniqueCuts = new HashMap<>(); // marking in original game to marking in unfolding
	private Map<Set<Place>, Set<Set<Place>>> uniqueHistory = new HashMap<>(); // unfoldingMarking to set of originalMarkings in its history
	private Map<Set<Place>, Set<Set<Place>>> unfoldingHistory = new HashMap<>(); // unfoldingMarking to set of unfoldingMarkings in its history

	public McMillianUnfolder(QbfSolvingObject<? extends Condition<?>> petriGame, Map<String, Integer> max) {
		super(petriGame, max);

		originalSolvingObject = petriGame.getCopy();
		originalGame = originalSolvingObject.getGame();

		for (Transition t : originalGame.getTransitions()) {
			pn.removeTransition(t.getId());
		}
		
		Marking initial = pn.getInitialMarking();
		for (Place p : originalGame.getPlaces()) {
			if (initial.getToken(p.getId()).getValue() < 1) {
				pn.removePlace(p.getId());
			}
		}
		
		//uniqueCuts.put(originalInitialMarking, pn.getPlaces());
		uniqueHistory.put(new HashSet<>(pn.getPlaces()), new HashSet<>());
		unfoldingHistory.put(new HashSet<>(pn.getPlaces()), new HashSet<>());
	}

	@Override
	public void createUnfolding() {
		// marking ---transition--> extension
		Queue<Triple<Set<Place>, Transition, Set<Place>>> possibleExtensions = possExt(pn.getPlaces());
		while (!possibleExtensions.isEmpty()) {
			
			
			/* // DEBUG ONLY to move step-by-step 
			try {
				System.in.read();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
			
			Triple<Set<Place>, Transition, Set<Place>> extension = possibleExtensions.poll();
			Set<Place> marking  = extension.a;
			Transition t = extension.b;
			Set<Place> preset = extension.c;
			Set<Place> postMarking = new HashSet<>(marking);
			
			//System.out.println(marking + " " + t);
			
			if (closed.contains(new Pair<>(t, preset))) {
				
				//System.out.println("already added");
				
				// do not add transition but continue with updated marking
				Transition alreadyAdded = null;
				for (Transition trans : pn.getTransitions()) {
					if (getOriginalTransitionId(trans.getId()).equals(t.getId()) && trans.getPreset().equals(preset)) {
						alreadyAdded = trans;
						break;		// should be unique and always present when reaching this part of the code
					}
				}
				postMarking.removeAll(alreadyAdded.getPreset());
				postMarking.addAll(alreadyAdded.getPostset());
			}  else {
				
				//System.out.println("new transition added");
				
				// calculate originalPostMarking because each original marking should be present once in the unfolding
				Set<Place> originalPostMarking = getOriginalMarkingCopy(marking);
				originalPostMarking.removeAll(t.getPreset());
				originalPostMarking.addAll(t.getPostset());
				//Set<Place> unfoldingPostMarking = uniqueCuts.get(originalPostMarking);
				//if (unfoldingPostMarking == null) {
					
					// add transition
					Transition newT = pn.createTransition(t.getId() + "__" + counterPlaces++);
					for (Place pre : preset) {
						pn.createFlow(pre, newT);
						postMarking.remove(pre);
					}
					for (Place post : t.getPostset()) {
						Place newPost = pn.createPlace(post.getId() + "__" + counterTransitions++);
						copyEnv(newPost, post);
						pn.createFlow(newT, newPost);
						postMarking.add(newPost);
					}
					closed.add(new Pair<>(t, preset));
					//uniqueCuts.put(getOriginalMarking(postMarking), postMarking);
				/*} else {
					// add transition
					Transition newT = pn.createTransition(t.getId() + "__" + counterPlaces++);
					for (Place pre : preset) {
						pn.createFlow(pre, newT);
						postMarking.remove(pre);
					}
					for (Place post : t.getPostset()) {
						Place newPost = null;
						for (Place unfoldingPlace : unfoldingPostMarking) {
							if (getOriginalPlaceId(unfoldingPlace.getId()).matches(post.getId())) {
								// should be always present and should be unique
								newPost = unfoldingPlace;
								break;
							}
						}
						pn.createFlow(newT, newPost);
					}
					closed.add(new Pair<>(t, preset));
				}*/
			}
			
			// update uniqueHistory and unfoldingHistory
			Set<Set<Place>> markingHistory = new HashSet<>(uniqueHistory.get(marking));
			markingHistory.add(getOriginalMarkingCopy(marking));
			Set<Set<Place>> previousMarkings = uniqueHistory.get(postMarking);
			if (previousMarkings != null) {
				markingHistory.addAll(new HashSet<>(previousMarkings));
			}
			uniqueHistory.put(postMarking, markingHistory);
			//System.out.println("MarkingHistory " + markingHistory);
			
			Set<Set<Place>> unfoldings = new HashSet<>(unfoldingHistory.get(marking));
			unfoldings.add(marking);
			Set<Set<Place>> previousCuts = unfoldingHistory.get(postMarking);
			if (previousCuts != null) {
				unfoldings.addAll(new HashSet<>(previousCuts));
			}
			unfoldingHistory.put(postMarking, unfoldings);
			
			// add ORIGINAL marking to cutOff iff ALL outgoing transitions have been added TOO RESTRICTIVE
			/*boolean allAdded = true;
			Set<Place> originalMarking = getOriginalMarking(marking);
			for (Transition originalTransition : originalGame.getTransitions()) {	// for all enabled original transitions search for copy in branching process
				if (originalMarking.containsAll(originalTransition.getPreset())) {	// check enabledness
					boolean found = false;
					for (Place p : marking) {
						for (Transition trans : p.getPostset()) {
							if (originalTransition.getId().equals(getOriginalTransitionId(trans.getId())) && marking.containsAll(trans.getPreset())) {
								found = true;
								break;
							}
						}
						if (found) break;
					}
					if (!found) {
						allAdded = false;
						break;
					}
				}
			}
			
			if (allAdded) {
				cutOffOriginal.add(originalMarking);
			}*/

			// stop from bad marking onwards
			boolean bad = false;
			for (Place p : postMarking) {
				for (Pair<String, Object> pair : p.getExtensions()) {
					if (pair.getFirst().matches("bad")) {
						bad = true;
						break;
					}
				}
				if (bad) {
					break;
				}
			}
			
			// TODO use new fancy long version only for loops in given PG
			
			// only once calling possExt for each postMarking seems to be possible as possible extensions of postmarking seem independent of currently build unfolding
			Set<Set<Place>> historyInMarkings = uniqueHistory.get(postMarking);
			boolean cont = true;
			Set<Place> originalPostMarking = getOriginalMarkingCopy(postMarking);
			//int i = 0;
			for (Set<Place> unfolding : unfoldingHistory.get(postMarking)) {
				// unfolding repeats same original marking with same history view
				if (originalPostMarking.equals(getOriginalMarkingCopy(unfolding))) {
					//System.out.println(++i);
					if (historyInMarkings.equals(uniqueHistory.get(unfolding))) {
						// TODO use this to build "correct" strategy for loops System.out.println(postMarking + " MATCHES " + unfolding);
						cont = false;
						break;
					} else {
						if (historyInMarkings.size() == uniqueHistory.get(unfolding).size() + 1) {
							Set<Set<Place>> set1 = new HashSet<>(historyInMarkings);
							Set<Set<Place>> set2 = new HashSet<>(uniqueHistory.get(unfolding));
							set1.removeAll(set2);
							Set<Set<Place>> set3 = new HashSet<>();
							set3.add(getOriginalMarkingCopy(unfolding));
							if (set3.equals(set1)) {
								System.out.println("LaCasa");
								cont = false;
								break;
							} else {
								// all sets contain p1 and p23
								System.out.println(set1);
								for (Set<Place> e : set1) {
									String es = e.toString();
									if ( 
										 ( es.contains("p10") && es.contains("p11") && es.contains("p14") && es.contains("p21") ) ||
										 ( es.contains("p10") && es.contains("p11") && es.contains("p16") && es.contains("p20") ) || 
										 ( es.contains("p11") && es.contains("p12") && es.contains("p14") && es.contains("p21") ) ||
										 ( es.contains("p11") && es.contains("p12") && es.contains("p16") && es.contains("p20") )
											) {
										System.out.println("TRUE");
									}
								}
							}
						}
					}
				}
			}
			
			//System.out.println("bad : " + bad + " " + postMarking);
			
			if (/*counterPlaces < 300 &&*/ /*!cutOffUnfolding.contains(postMarking) && !bad && */ cont /*&& !cutOffOriginal.contains(getOriginalMarking(postMarking))*/) {
				possibleExtensions.addAll(possExt(postMarking));
				cutOffUnfolding.add(postMarking);
			} else if (cutOffUnfolding.contains(postMarking)) {
				//System.out.println(cutOffUnfolding.size());
			}
		}
	}
	
	// TODO maybe utilize that when bad place is reached no more unfolding is needed?
	
	// Somewhat clever search strategy:
	// Given a marking in the branching process, 
	// we calculate all newly enabled transitions in the original PG and return them together with the preset in the branching process
	private Queue<Triple<Set<Place>, Transition, Set<Place>>> possExt (Set<Place> marking) {
		Queue<Triple<Set<Place>, Transition, Set<Place>>> possibleExtensions = new LinkedList<>();
		// iterate over ALL original transitions as progress somewhere else in the net may enable other transitions
		for (Transition originalPostTransition : originalGame.getTransitions()) {
			Set<Place> preset = new HashSet<>();
			boolean isEnabled = true;
			// search for fitting places in the branching process for each original place in the preset of the transition
			for (Place originalPresetPlace : originalPostTransition.getPreset()) {
				boolean found = false;
				for (Place place : marking) {
					if (getOriginalPlaceId(originalPresetPlace.getId()).equals(getOriginalPlaceId(place.getId()))) {
						preset.add(place);
						found = true;
					}
				}
				if (!found) {
					isEnabled = false;
					break;
				}
			}
			if (isEnabled) {		// definition of causal past based on places and transitions does not work
				possibleExtensions.add(new Triple<>(new HashSet<>(marking), originalPostTransition, preset));	
			}
		}
		return possibleExtensions;
	}
	
	private Set<Place> getOriginalMarkingCopy(Set<Place> marking) {
		Set<Place> originalMarking = new HashSet<>();
		for (Place p : marking) {
			originalMarking.add(originalGame.getPlace(getOriginalPlaceId(p.getId())));
		}
		return originalMarking;
	}

	public static String getOriginalPlaceId(String id) {
		int index = id.indexOf("__");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}

	public static String getOriginalTransitionId(String id) {
		int index = id.indexOf("__");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}
	
	// TODO go from small to big strategy
}
