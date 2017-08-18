package uniolunisaar.adam.bounded.qbfapproach;

import java.util.HashMap;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
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
		/*oneTest("jhh/myexample2", 10, 2, true);
		oneTest("ndet/nondet", 5, 2, false);
		oneTest("burglar/burglar", 7, 2, true);
		oneTest("burglar/burglar", 6, 2, false);*/
		oneTest("jhh/robots", 20, 0, true);
		//oneTest("constructedExample/constructedExample", 4, 0, true);
		//oneTest("constructedExample/constructedExample", 3, 0, false);
		//oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 4, 0, true);
		//oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 3, 0, false);
		//oneTest("container/container", 10, 2, true);	// TODO search for bound
		//oneTest("container/container", 3, 0, false);
		/*oneTest("deadlock/missDeadlock", 4, 0, true);
		oneTest("deadlock/missDeadlock", 3, 0, false);
		//oneTest("firstExamplePaper/firstExamplePaper_extended", 6, 3, true);  // TODO did this ever work?!
		oneTest("firstExamplePaper/firstExamplePaper_extended", 5, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper", 5, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper", 4, 3, false);
		oneTest("jhh/myexample7", 4, 0, true);
		oneTest("jhh/myexample7", 3, 0, false);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 3, 0, true);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 2, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 2, false);
		oneTest("jhh/myexample1", 10, 0, false);
		oneTest("jhh/myexample1", 10, 2, false);*/
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        QBFPetriGame pg = new QBFPetriGame(pn);
		pg.setN(n);
		pg.setB(b);
		ForNonDeterministicUnfolder u = new ForNonDeterministicUnfolder(pg, new HashMap<>());
		u.createUnfolding();
       /* QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		Assert.assertEquals(sol.existsWinningStrategy(), result);
		
		// TODO put this to an appropriate place in code
		Tools.savePN2PDF("originalGame", sol.game.getNet(), true);
		Tools.savePN2PDF("unfolding", sol.unfolding.getNet(), true);
		if (sol.existsWinningStrategy()) {
			Tools.savePN2PDF("strategy", sol.getStrategy(), true);
		}*/
		
	}
}
