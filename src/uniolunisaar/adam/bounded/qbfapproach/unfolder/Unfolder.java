package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

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

	// global counter used to make copied places and transitions due to unfolding unique (increased on each access)
	protected int copycounter = 0;
	public Map<String, Integer> copycounter_map = new HashMap<>();

	// different limits for places
	protected Map<String, Integer> max = null;

	// how much unfolding was done and how much can still be done
	public Map<String, Integer> current = new HashMap<>();
	protected Map<String, Integer> limit = null;

	public Unfolder(QBFPetriGame petriGame) {
		pg = petriGame;
		pn = pg.getNet();
	}

	public void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		if (max != null) {
			for (Place p : pg.getNet().getPlaces()) {
				if (max.get(p.getId()) == null) {
					max.put(p.getId(), 0);
				}
			}
			createUnfolding(max);
		} else {
			Map<String, Integer> bvalues = new HashMap<>();
			for (Place p : pg.getNet().getPlaces()) {
				bvalues.put(p.getId(), pg.getB());
			}
			createUnfolding(bvalues);
		}
	}

	public abstract void createUnfolding(Map<String, Integer> b) throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException;

	protected void copyBadAndEnv(Place newP, Place p) {
		for (Pair<String, Object> pair : p.getExtensions()) {
			newP.putExtension(pair.getFirst(), pair.getSecond());
		}
		if (pg.getEnvPlaces().contains(p)) {
			pg.getEnvPlaces().add(newP);
		}
		// winning conditions are addeed after unfolding TODO unfolding uses reachbad?
	}

	protected int getCopyCounter() {
		return copycounter++;
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
