package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.antlr.v4.runtime.misc.Triple;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.ds.petrinet.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.exceptions.pg.NoSuitableDistributionFoundException;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class McMillianUnfolder extends Unfolder {
	// unfolded result Petri game and Petri net
	private QbfSolvingObject<? extends Condition> originalSolvingObject;
	private PetriGame originalGame;

	Set<Pair<Transition, Set<Place>>> closed = new HashSet<>();
	//Set<Set<Place>> cutOffOriginal = new HashSet<>();	// cutOff based on markings in the original net
	Set<Set<Place>> cutOffUnfolding = new HashSet<>(); // cutOff based on markings in the build unfolding

	int counterPlaces = 0;
	int counterTransitions = 0;

	public McMillianUnfolder(QbfSolvingObject<? extends Condition> petriGame, Map<String, Integer> max) {
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
	}

	@Override
	public void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		// marking ---transition--> extension
		Queue<Triple<Set<Place>, Transition, Set<Place>>> possibleExtensions = possExt(pn.getPlaces());
		while (!possibleExtensions.isEmpty()) {
			Triple<Set<Place>, Transition, Set<Place>> extension = possibleExtensions.poll();
			Set<Place> marking  = extension.a;
			Transition t = extension.b;
			Set<Place> preset = extension.c;
			Set<Place> postMarking = new HashSet<>(marking);
			if (closed.contains(new Pair<>(t, preset))) {
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
			} else {
				// add transition
				Transition newT = pn.createTransition(t.getId() + "__" + counterPlaces++);
				for (Place pre : preset) {
					pn.createFlow(pre, newT);
					postMarking.remove(pre);
				}
				for (Place post : t.getPostset()) {
					Place newPost = pn.createPlace(post.getId() + "__" + counterTransitions++);
					postMarking.add(newPost);
					copyEnv(newPost, post);
					pn.createFlow(newT, newPost);
				}
				closed.add(new Pair<>(t, preset));
			}
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

			// only once calling possExt for each postMarking seems to be possible as possible extensions of postmarking seem independent of currently build unfolding
			if (!cutOffUnfolding.contains(postMarking) /*&& !cutOffOriginal.contains(getOriginalMarking(postMarking))*/) {
				possibleExtensions.addAll(possExt(postMarking));
				cutOffUnfolding.add(postMarking);
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
	
	/*private Set<Place> getOriginalMarking(Set<Place> marking) {
		Set<Place> originalMarking = new HashSet<>();
		for (Place p : marking) {
			originalMarking.add(originalGame.getPlace(getOriginalPlaceId(p.getId())));
		}
		return originalMarking;
	}*/

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
