package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

/**
 * quick and dirty
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class DeterministicUnfolder extends Unfolder {

	Set<Place> consideredPlaces;

	public DeterministicUnfolder(QBFPetriGame QBFPetriGame, Map<String, Integer> max) {
		super(QBFPetriGame, max);
		QBFPetriGame pgCopy = pg.copy("temp");
		PGSimplifier.simplifyPG(pgCopy, false, false);
		consideredPlaces = pgCopy.getNet().getPlaces();
	}

	@Override
	public void createUnfolding(Map<String, Integer> b) throws NetNotSafeException, NoSuitableDistributionFoundException {
		// Initialize bounds
		limit = b;
		for (String s : b.keySet())
			current.put(s, 0);

		// Start search
		Place nextToUnfold;
		while ((nextToUnfold = findNextPlaceToUnfold()) != null) {
			unfoldPlace(nextToUnfold);
		}
	}

	private boolean unfoldCondition(Place p) {
		return getCurrentValue(p) < getLimitValue(p) && (p.getPreset().size() >= 2 || (p.getPreset().size() == 1 && p.getInitialToken().getValue() == 1));
	}

	public Place findNextPlaceToUnfold() {
		// first try environment places
		for (Place env : pg.getEnvPlaces()) {
			if (unfoldCondition(env))
				return env;
		}
		// next try system places
		for (Place sys : pn.getPlaces()) {
			if (unfoldCondition(sys))
				return sys;
		}
		return null;
	}

	public void unfoldPlace(Place unfold) {
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
				reduceCurrentValue(unfold);
			}
		}
		
		for (Transition t : otherTransitions) {
			if (bound-- > 0) {
				Place newP = copyPlace(unfold);
				pg.getNet().removeFlow(t, unfold);
				pg.getNet().createFlow(t, newP);
				reduceCurrentValue(unfold);
			}
		}
	}

	public void unfoldPlaceOLD(Place unfold) {
		if (getCurrentValue(unfold) <= getLimitValue(unfold)) {
			int iterations = unfold.getPreset().size() - 1;
			for (int i = 0; i < iterations; ++i) {
				Place newP = copyPlace(unfold);
				// change one existing transition i.e. unfold
				// again choose first, maybe one can do better
				for (Transition t : unfold.getPreset()) {
					pg.getNet().removeFlow(t, unfold);
					pg.getNet().createFlow(t, newP);
					break;
				}
				unfoldPlaceRecursion(newP, unfold);
			}
		}
	}

	// TODO detect loops early before limit is reached

	public void unfoldPlaceRecursion(Place newP, Place oldP) {
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

	public Place copyPlace(Place p) {
		String id = truncateUnderscores(p.getId());
		Place ret = pg.getNet().createPlace(id + "__" + current.get(id));
		current.put(id, current.get(id) + 1);
		copyBadAndEnv(ret, p);
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

	public Transition copyTransition(Transition t) {
		String id = truncateUnderscores(t.getId());
		Transition ret = pg.getNet().createTransition(id + "__" + getCopyCounter(id));
		return ret;

	}

	public String truncateUnderscores(String id) {
		int index = id.indexOf("__");
		if (index != -1)
			return id.substring(0, index);
		else
			return id;
	}
	
	public void reduceCurrentValue(Place p) {
		String id = truncateUnderscores(p.getId());
		current.put(id, current.get(id) + 1);
	}

	public int getCurrentValue(Place p) {
		String id = truncateUnderscores(p.getId());
		return current.get(id);
	}

	public int getLimitValue(Place p) {
		String id = truncateUnderscores(p.getId());
		return limit.get(id);
	}
}
