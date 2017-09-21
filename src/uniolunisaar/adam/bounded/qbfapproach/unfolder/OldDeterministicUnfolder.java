package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

/**
 * quick and dirty
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class OldDeterministicUnfolder extends Unfolder {

	public OldDeterministicUnfolder(QBFPetriGame QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
	}

	@Override
	protected void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException {
		// Start search
		Place nextToUnfold;
		while ((nextToUnfold = findNextPlaceToUnfold()) != null) {
			unfoldPlace(nextToUnfold);
		}
	}

	private Place findNextPlaceToUnfold() {
		// first try environment places
		for (Place env : pg.getEnvPlaces()) {
			if (unfoldConditionSatisfied(env))
				return env;
		}
		// next try system places
		for (Place sys : pn.getPlaces()) {
			if (unfoldConditionSatisfied(sys))
				return sys;
		}
		return null;
	}

	private void unfoldPlace(Place unfold) {
		int limit = getLimitValue(unfold) - getCurrentValue(unfold);
		int required = (int) (unfold.getPreset().size() + unfold.getInitialToken().getValue() - 1);
		int bound = Math.min(limit, required);
		Set<Transition> selfLoops = new HashSet<>();
		Set<Transition> otherTransitions = new HashSet<>();
		for (Transition pre : unfold.getPreset()) {
			if (pre.getPreset().contains(unfold))
				selfLoops.add(pre);
			else
				otherTransitions.add(pre);
		}
		for (Transition t : selfLoops) {
			if (bound-- > 0) {
				Place newP = copyPlace(unfold);
				pg.getNet().removeFlow(t, unfold);
				pg.getNet().createFlow(t, newP);
				increaseCurrentValue(unfold);
			}
		}

		for (Transition t : otherTransitions) {
			if (bound-- > 0) {
				Place newP = copyPlace(unfold);
				pg.getNet().removeFlow(t, unfold);
				pg.getNet().createFlow(t, newP);
				increaseCurrentValue(unfold);
			}
		}
	}

	// TODO detect loops early before limit is reached
	private void unfoldPlaceRecursion(Place newP, Place oldP) {
		for (Transition post : oldP.getPostset()) {
			Transition newT = copyTransition(post);
			// copy incoming transitions
			for (Place preOfPost : post.getPreset()) {
				if (preOfPost.equals(oldP))
					pg.getNet().createFlow(newP, newT);
				else
					pg.getNet().createFlow(preOfPost, newT);
			}
			// copy outgoing transitions
			for (Place postOfPost : post.getPostset()) {
				if (postOfPost.getPreset().size() >= 2) {
					// recursive unfold
					if (getCurrentValue(postOfPost) <= getLimitValue(postOfPost)) {
						// we can unfold
						Place newnewP = copyPlace(postOfPost);
						unfoldPlaceRecursion(newnewP, postOfPost);
						pg.getNet().createFlow(newT, postOfPost);
					} else {
						// we cannot unfold
						pg.getNet().createFlow(newT, postOfPost);
					}
				} else {
					// do not unfold
					pg.getNet().createFlow(newT, postOfPost);
				}
			}
		}
	}
}