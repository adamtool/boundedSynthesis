package uniolunisaar.adam.bounded.qbfapproach;


import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.tools.Tools;

@Test
public class SafetyTest {
	
	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void test() throws Exception {
		//oneTest("tests/watchdog5", 15, 3, true);		// TODO search for bounds
		//oneTest("container/container", 10, 2, true);	// TODO search for bounds
		oneTest("nm/sendingprotocolTwo", 11, 2, true);
		/*oneTest("jhh/myexample1", 10, 0, false);
		oneTest("jhh/myexample1", 10, 2, false);
		oneTest("jhh/myexample2", 10, 2, true);
		oneTest("jhh/myexample2", 10, 0, true);
		oneTest("jhh/myexample7", 4, 0, true);
		oneTest("jhh/myexample7", 3, 0, false);
		oneTest("ndet/nondet", 5, 2, false); //TRUE
		oneTest("burglar/burglar", 7, 3, true);
		oneTest("burglar/burglar", 6, 2, false);
		oneTest("jhh/robots_true", 20, 0, true);
		oneTest("jhh/robots_false", 20, 0, false);
		oneTest("constructedExample/constructedExample", 4, 0, true);
		oneTest("constructedExample/constructedExample", 3, 0, false);
		oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 4, 0, true);
		oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 3, 0, false);
		oneTest("container/container", 20, 0, false);
		oneTest("deadlock/missDeadlock", 4, 0, true);
		oneTest("deadlock/missDeadlock", 3, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 10, true);
		oneTest("firstExamplePaper/firstExamplePaper", 4, 3, false);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 3, 0, true);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 2, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 2, false);
		oneTest("ndet/nondet_s3", 10, 0, false);
		oneTest("ndet/nondet_s3", 10, 2, true);
		oneTest("ndet/nondet_s3_noStrat", 15, 2, false);
		oneTest("ndet/nondet_unnecessarily_noStrat", 15, 3, false);
		oneTest("testingNets/envSkipsSys", 15, 3, false);*/
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new Safety(), new QBFSolverOptions(n, b));
		sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		// TODO put this to an appropriate place in code
		Tools.savePN2PDF("originalGame", sol.game.getNet(), false);
		Tools.savePN2PDF("unfolding", sol.unfolding.getNet(), false);
		if (sol.existsWinningStrategy()) {
			Tools.savePN2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
