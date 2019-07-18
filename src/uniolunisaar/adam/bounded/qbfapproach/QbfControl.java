package uniolunisaar.adam.bounded.qbfapproach;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.Unfolder;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.util.PGTools;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class QbfControl {

	public static String linebreak = System.lineSeparator();
	public static String additionalSystemName = "AS___";
	public static String additionalSystemUniqueDivider = "_0_";
	public static String solver = "quabs";
	public static String replaceAfterwardsSpaces = "          ";
	public static boolean deterministicStrategy = true;
	public static boolean debug = false;
	public static boolean edacc = false;
	public static boolean rebuildingUnfolder = false; // in contrast to extending unfolder
	public static boolean binaryPlaceEncoding = false; // based on slice (in contrast to original P x n copies)
	
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
		return PGTools.checkStrategy(origNet, strat, true);
	}
}
