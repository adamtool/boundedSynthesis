package uniolunisaar.adam.bounded.qbfapproach;


import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFForallBuchiSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.winningconditions.Buchi;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.tools.Tools;

@Test
public class ForallBuchiTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void test() throws Exception {
		oneTest("jhh/myexample1", 4, 0, false);
		oneTest("jhh/myexample1", 10, 0, false);
		oneTest("jhh/myexample11", 4, 0, false);
		oneTest("jhh/myexample11", 10, 0, false);
		oneTest("jhh/myexample12", 4, 0, false);
		oneTest("jhh/myexample12", 10, 0, false);
		oneTest("jhh/myexample13", 4, 0, true);
		oneTest("jhh/myexample13", 10, 0, true);
		oneTest("jhh/myexample2", 4, 0, false);
		oneTest("jhh/myexample2", 10, 0, false);
		oneTest("jhh/myexample21", 4, 0, false);
		oneTest("jhh/myexample21", 10, 0, false);
		oneTest("jhh/myexample22", 4, 0, true);
		oneTest("jhh/myexample22", 10, 0, true);
		oneTest("toyexamples/infiniteChains", 7, 0, true);
		oneTest("toyexamples/infiniteChains", 10, 0, true);
		oneTest("toyexamples/infiniteChains1", 7, 0, false);
		oneTest("toyexamples/infiniteChains1", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains0", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains0", 15, 0, false);
		oneTest("toyexamples/oneTokenMultiChains1", 11, 0, true);
		oneTest("toyexamples/oneTokenMultiChains1", 15, 0, true);
		oneTest("toyexamples/oneTokenMultiChains2", 11, 0, true);
		oneTest("toyexamples/oneTokenMultiChains2", 15, 0, true);
		oneTest("toyexamples/oneTokenMultiChains3", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains3", 15, 0, false);
		oneTest("toyexamples/oneTokenMultiChains4", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains4", 15, 0, false);
		oneTest("toyexamples/oneTokenMultiChains5", 13, 0, true);		// infinitely many becoming true
		oneTest("toyexamples/oneTokenMultiChains5", 15, 0, true);		// infinitely many becoming true
		oneTest("toyexamples/oneTokenMultiChains6", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains6", 15, 0, false);
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallbuechi/" + str + ".apt";
		PetriNet pn = Tools.getPetriNet(path);
        QBFForallBuchiSolver sol = new QBFForallBuchiSolver(new QBFPetriGame(pn), new Buchi(), new QBFSolverOptions(n, b));
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("originalGame", sol.game.getNet(), false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding.getNet(), false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
