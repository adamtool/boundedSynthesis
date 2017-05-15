package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.tools.Tools;

@Test
public class ExamplesTest {
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void test() throws Exception {
		/*oneTest("burglar/burglar", 7, 2, true);
		oneTest("burglar/burglar", 6, 2, false);
		oneTest("constructedExample/constructedExample", 4, 0, true);
		oneTest("constructedExample/constructedExample", 3, 0, false);
		oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 4, 0, true);
		oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 3, 0, false);*/
		//oneTest("container/container", 10, 2, true);	// TODO search for bound
		//oneTest("container/container", 3, 0, false);
		/*oneTest("deadlock/missDeadlock", 4, 0, true);
		oneTest("deadlock/missDeadlock", 3, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 6, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 5, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper", 5, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper", 4, 3, false);
		oneTest("jhh/myexample7", 4, 0, true);
		oneTest("jhh/myexample7", 3, 0, false);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 3, 0, true);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 2, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 2, false);*/
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
