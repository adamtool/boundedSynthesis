package uniolunisaar.adam.bounded.qbfapproach;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFExistsSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.petrigame.TokenFlow;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.PetriGameAnnotator;
import uniolunisaar.adam.tools.Tools;

@Test
public class ExistsSafetyTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void test() throws Exception {
		oneTest("jhh/oneTransitionEnv1", 3, 0, false);
		oneTest("jhh/oneTransitionEnv1", 10, 0, false);
		oneTest("jhh/oneTransitionEnv2", 3, 0, true);
		oneTest("jhh/oneTransitionEnv2", 10, 0, true);
		oneTest("jhh/oneTransitionEnv3", 3, 0, false);
		oneTest("jhh/oneTransitionEnv3", 10, 0, false);
		oneTest("jhh/oneTransitionSys1", 3, 0, false);
		oneTest("jhh/oneTransitionSys1", 10, 0, false);
		oneTest("jhh/oneTransitionSys2", 3, 0, true);
		oneTest("jhh/oneTransitionSys2", 10, 0, true);
		oneTest("jhh/oneTransitionBoth1", 3, 0, true);
		oneTest("jhh/oneTransitionBoth1", 10, 0, true);
		oneTest("jhh/oneTransitionBoth2", 3, 0, false);
		oneTest("jhh/oneTransitionBoth2", 10, 0, false);
		oneTest("jhh/oneTransitionBoth3", 3, 0, true);
		oneTest("jhh/oneTransitionBoth3", 10, 0, true);
		oneTest("jhh/oneTransitionBoth4", 3, 0, true);
		oneTest("jhh/oneTransitionBoth4", 10, 0, true);
		oneTest("jhh/oneTransitionBoth5", 3, 0, true);
		oneTest("jhh/oneTransitionBoth5", 10, 0, true);
		oneTest("jhh/decision1", 3, 0, false);
		oneTest("jhh/decision1", 10, 0, false);
		oneTest("jhh/decision2", 3, 0, true);
		oneTest("jhh/decision2", 10, 0, true);
		oneTest("jhh/twoDecisions1", 3, 0, false);
		oneTest("jhh/twoDecisions1", 5, 0, false);
		oneTest("jhh/twoDecisions1", 10, 0, false);
		oneTest("jhh/twoDecisions2", 3, 0, false);
		oneTest("jhh/twoDecisions2", 5, 0, true);
		oneTest("jhh/twoDecisions2", 10, 0, true);
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
        final String path = System.getProperty("examplesfolder") + "/existssafety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        PetriGameAnnotator.parseNetOptionsAndAnnotate(pn);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        for (Transition t : pn.getTransitions()) {
            List<TokenFlow> list = AdamExtensions.getTokenFlow(t);
            Set<Pair<Place, Place>> set = new HashSet<>();
            for (TokenFlow tf : list) {
            	for (Place pre : tf.getPreset()) {
            		for (Place post : tf.getPostset()) {
            			set.add(new Pair<>(pre, post));
            		}
            	}
            }
        	fl.put(t, set);
        }
        QBFExistsSafetySolver sol = new QBFExistsSafetySolver(new QBFPetriGame(pn), new Safety(), new QBFSolverOptions(n, b));
        sol.pg.setFl(fl);
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("originalGame", sol.game.getNet(), false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding.getNet(), false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
