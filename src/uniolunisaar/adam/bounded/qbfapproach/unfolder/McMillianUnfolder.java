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
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.pg.NetNotSafeException;
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
		Queue<Triple<Set<Place>, Transition, Set<Place>>> possibleExtensions = possExt(pn.getPlaces(), pn.getPlaces());
		while (!possibleExtensions.isEmpty()) {
			Triple<Set<Place>, Transition, Set<Place>> extension = possibleExtensions.poll();
			Set<Place> marking  = extension.a;
			Transition t = extension.b;
			Set<Place> preset = extension.c;
			System.out.println(marking);
			if (closed.contains(new Pair<>(t, preset))) {
				// do not add transition but continue with updated marking
				Transition alreadyAdded = null;
				for (Transition trans : originalGame.getTransitions()) {
					if (trans.getPreset().equals(preset)) {
						alreadyAdded = trans;
						break;		// should be unique and always present when reaching this part of the code
					}
				}
				marking = new HashSet<>(marking);
				marking.removeAll(alreadyAdded.getPreset());
				marking.addAll(alreadyAdded.getPostset());
				possibleExtensions.addAll(possExt(marking, alreadyAdded.getPostset()));
			} else {
				marking = new HashSet<>(marking);
				// add transition
				Transition newT = pn.createTransition(t.getId() + "__" + counterPlaces++);
				for (Place pre : preset) {
					pn.createFlow(pre, newT);
					marking.remove(pre);
				}
				Set<Place> newPostSet = new HashSet<>();
				for (Place post : t.getPostset()) {
					Place newPost = pn.createPlace(post.getId() + "__" + counterTransitions++);
					newPostSet.add(newPost);
					marking.add(newPost);
					if (originalGame.getEnvPlaces().contains(post)) {
						pg.getGame().getEnvPlaces().add(newPost);
					}
					for (Pair<String, Object> pair : post.getExtensions()) {
						newPost.putExtension(pair.getFirst(), pair.getSecond());
					}
					pn.createFlow(newT, newPost);
				}
				possibleExtensions.addAll(possExt(marking, newPostSet));
			}
			closed.add(new Pair<>(t, preset));
		}	
	}
	
	// Somewhat clever search strategy:
	// Given a marking (and the postset of the last fired transition) in the branching process, 
	// we calculate all newly enabled transitions in the original PG and return them together with the preset in the branching process
	private Queue<Triple<Set<Place>, Transition, Set<Place>>> possExt (Set<Place> marking, Set<Place> postset) {
		Queue<Triple<Set<Place>, Transition, Set<Place>>> possibleExtensions = new LinkedList<>();
		// iterate over newly added places in branching process:
		for (Place postPlace : postset) {
			Place originalPostPlace = originalGame.getPlace(getOriginalPlaceId(postPlace.getId()));
			// check the following transitions in the original game:
			for (Transition originalPostTransition : originalPostPlace.getPostset()) {
				Set<Place> preset = new HashSet<>();
				boolean isEnabled = true;
				// search for fitting places in the branching process for each original place in the preset of the transition
				for (Place originalPresetPlace : originalPostTransition.getPreset()) {
					boolean found = false;
					for (Place place : marking) {
						if (getOriginalPlaceId(originalPresetPlace.getId()).equals(getOriginalPlaceId(place.getId()))) {
							preset.add(place);
							found = true;
							break;
						}
					}
					if (!found) {
						isEnabled = false;
						break;
					}
				}
				if (isEnabled) {
					// check for causal past AFTER enabledness as it should be cheaper?
					if (!isCausalPast(originalPostTransition.getId(), marking)) {
						possibleExtensions.add(new Triple<>(new HashSet<>(marking), originalPostTransition, preset));
					}
				}
			}
		}
		return possibleExtensions;
	}
	
	private boolean isCausalPast(String transitionId, Set<Place> marking) {
		boolean isCausalPast = false;
		for (Place p : marking) {
			for (Transition pre : p.getPreset()) {
				if (getOriginalTransitionId(pre.getId()).equals(transitionId)) {
					return true;
				} else {
					isCausalPast = isCausalPast(transitionId, pre.getPreset()) || isCausalPast;  
				}
			}
		}
		return isCausalPast;
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
}
