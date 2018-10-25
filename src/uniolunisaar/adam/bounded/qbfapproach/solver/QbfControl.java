package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.Unfolder;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.util.AdamTools;

public class QbfControl {

	// TODO maybe optional arguments to change default values
	public static String linebreak = System.lineSeparator(); // Controller
	public static String additionalSystemName = "AS___"; // Controller
	public static String additionalSystemUniqueDivider = "_0_"; // Controller
	public static String solver = "quabs"; // Controller
	public static String replaceAfterWardsSpaces = "          "; // Controller
	public static boolean deterministicStrat = true; // Controller
	public static boolean debug = true;
	public static boolean edacc = false;
	public static boolean mcmillian = false; // Unfolder
	
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
		return AdamTools.checkStrategy(origNet, strat);
	}
}
