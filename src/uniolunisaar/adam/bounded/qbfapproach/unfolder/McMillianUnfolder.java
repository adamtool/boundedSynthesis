package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.antlr.v4.runtime.misc.Triple;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.pg.NetNotSafeException;
import uniolunisaar.adam.exceptions.pg.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.util.PGTools;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class McMillianUnfolder extends Unfolder {

	// unfolded result Petri game and Petri net
	public QbfSolvingObject<? extends Condition> unfolding;
	public PetriNet unfoldingNet;

	Set<Pair<Transition, Set<Place>>> closed = new HashSet<>();

	int counterPlaces = 0;
	int counterTransitions = 0;

	public McMillianUnfolder(QbfSolvingObject<? extends Condition> petriGame, Map<String, Integer> max) {
		super(petriGame, max);

		unfoldingNet = new PetriNet(pn.getName() + "_unfolding");
		unfolding = petriGame.getCopy();

		Marking initial = pn.getInitialMarking();
		for (Place p : pn.getPlaces()) {
			if (initial.getToken(p).getValue() == 1) {
				Place newP = unfoldingNet.createPlace(p.getId() + "__" + counterPlaces++);
				newP.setInitialToken(1);
				if (pg.getGame().getEnvPlaces().contains(p)) {
					unfolding.getGame().getEnvPlaces().add(newP);
				}
				for (Pair<String, Object> pair : p.getExtensions()) {
					newP.putExtension(pair.getFirst(), pair.getSecond());
				}
			}
		}
	}

	@Override
	public void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		Queue<Triple<Set<Place>, Transition, Set<Place>>> possibleExtensions = possExt(unfoldingNet.getPlaces(), unfoldingNet.getPlaces());
		while (!possibleExtensions.isEmpty()) {
			Triple<Set<Place>, Transition, Set<Place>> extension = possibleExtensions.poll();
			Set<Place> marking  = extension.a;
			Transition t = extension.b;
			Set<Place> preset = extension.c;
			if (closed.contains(new Pair<>(t, preset))) {
				// do not add transition but continue with updated marking
				Transition alreadyAdded = null;
				for (Transition trans : unfoldingNet.getTransitions()) {
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
				Transition newT = unfoldingNet.createTransition(t.getId() + "__" + counterPlaces++);
				for (Place pre : preset) {
					unfoldingNet.createFlow(pre, newT);
					marking.remove(pre);
				}
				Set<Place> newPostSet = new HashSet<>();
				for (Place post : t.getPostset()) {
					Place newPost = unfoldingNet.createPlace(post.getId() + "__" + counterTransitions++);
					newPostSet.add(newPost);
					marking.add(newPost);
					if (pg.getGame().getEnvPlaces().contains(post)) {
						unfolding.getGame().getEnvPlaces().add(newPost);
					}
					for (Pair<String, Object> pair : post.getExtensions()) {
						newPost.putExtension(pair.getFirst(), pair.getSecond());
					}
					unfoldingNet.createFlow(newT, newPost);
				}
				possibleExtensions.addAll(possExt(marking, newPostSet));
			}
			closed.add(new Pair<>(t, preset));
			
			// TODO stop based on causal past
		}

		// TODO talk to Manuel whether this can be done more cleverly?
		
		try {
			PGTools.savePG2PDF("MCunfolding", new PetriGame(unfoldingNet), false);
		} catch (NotSupportedGameException | IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*// remove original net via transitions and places
		Set<Transition> transitions = new HashSet<>(pn.getTransitions());
		for (Transition t : transitions) {
			pn.removeTransition(t);
		}
		Set<Place> places = new HashSet<>(pn.getPlaces());
		for (Place p : places) {
			pn.removePlace(p);
		}
		
		// add updated net
		
		for (Place p : unfoldingNet.getPlaces()) {
			pn.createPlace(p);
		}
		
		for (Transition t : unfoldingNet.getTransitions()) {
			pn.createTransition(t);
			for (Place pre : t.getPreset()) {
				pn.createFlow(pre, t);
			}
			for (Place post : t.getPostset()) {
				pn.createFlow(t, post);
			}
		}*/
		
	}
	
	// Somewhat clever search strategy:
	// Given a marking (and the postset of the last fired transition) in the branching process, 
	// we calculate all newly enabled transitions in the original PG and return them together with the preset in the branching process
	private Queue<Triple<Set<Place>, Transition, Set<Place>>> possExt (Set<Place> marking, Set<Place> postset) {
		System.out.println(marking);
		Queue<Triple<Set<Place>, Transition, Set<Place>>> possibleExtensions = new LinkedList<>();
		// iterate over newly added places in branching process:
		for (Place postPlace : postset) {
			Place originalPostPlace = pn.getPlace(getOriginalPlaceId(postPlace.getId()));
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
					possibleExtensions.add(new Triple<>(new HashSet<>(marking), originalPostTransition, preset));
				}
			}
		}
		return possibleExtensions;
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
