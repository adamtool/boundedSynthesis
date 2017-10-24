package uniolunisaar.adam.bounded.qbfapproach;


import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFForallReachabilitySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.tools.Tools;

@Test
public class ForallReachabilityTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void test() throws Exception {
		oneTest("jhh/myexample11", 3, 0, false);
		oneTest("jhh/myexample11", 10, 0, false);
		oneTest("jhh/myexample12", 3, 0, true);
		oneTest("jhh/myexample12", 10, 0, true);
		oneTest("jhh/myexample21", 3, 0, false);
		oneTest("jhh/myexample21", 10, 0, false);
		oneTest("jhh/myexample22", 20, 0, true);
		oneTest("burglar/burglar", 10, 0, false);
		oneTest("burglar/burglar", 10, 2, true);
		oneTest("burglar/burglar1", 10, 0, false);
		oneTest("burglar/burglar1", 10, 2, true);
		oneTest("burglar/burglar2", 10, 0, false);
		oneTest("burglar/burglar2", 10, 2, false);
		oneTest("burglar/burglarDirectlyWon", 10, 0, true);
		oneTest("toyexamples/infiniteFlowChains", 20, 0, false);		// TODO add check for simultaneous
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
		PetriNet pn = Tools.getPetriNet(path);
        QBFForallReachabilitySolver sol = new QBFForallReachabilitySolver(new QBFPetriGame(pn), new Reachability(), new QBFSolverOptions(n, b));
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("originalGame", sol.game.getNet(), false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding.getNet(), false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
