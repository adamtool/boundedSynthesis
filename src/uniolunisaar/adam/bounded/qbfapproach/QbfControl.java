package uniolunisaar.adam.bounded.qbfapproach;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.Unfolder;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.util.PNWTTools;

public class QbfControl {

	public static String linebreak = System.lineSeparator(); // Controller
	public static String additionalSystemName = "AS___"; // Controller
	public static String additionalSystemUniqueDivider = "_0_"; // Controller
	public static String solver = "quabs"; // Controller
	public static String replaceAfterWardsSpaces = "          "; // Controller
	public static boolean deterministicStrat = true; // Controller
	public static boolean debug = false;
	public static boolean edacc = false;
	public static boolean rebuildingUnfolder = false; // in contrast to extending unfolder
	
	// Check winning strategy for validity
	public static boolean checkStrategy(PetriGame origNet, PetriGame strat) {
		// some preparation
		for (Place p : origNet.getPlaces()) {
			origNet.setOrigID(p, Unfolder.getTruncatedId(p.getId()));
		}
		for (Place p : strat.getPlaces()) {
			strat.setOrigID(p, Unfolder.getTruncatedId(p.getId()));
		}
		for (Transition t : strat.getTransitions()) {
			t.setLabel(Unfolder.getTruncatedId(t.getId()));
		}
		return PNWTTools.checkStrategy(origNet, strat);
	}
}
