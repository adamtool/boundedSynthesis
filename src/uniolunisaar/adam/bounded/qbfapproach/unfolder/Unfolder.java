package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;

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

	// Counter to make copied transitions unique, places use numbers from current
	protected Map<String, Integer> copycounter_map = new HashMap<>();
	
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

	public abstract void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException;

	protected boolean unfoldCondition(Place p) {
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
		Transition ret = pg.getNet().createTransition(id + "__" + getCopyCounter(id));
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
