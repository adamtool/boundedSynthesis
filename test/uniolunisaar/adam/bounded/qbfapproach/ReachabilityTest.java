package uniolunisaar.adam.bounded.qbfapproach;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFReachabilitySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.tools.Tools;

@Test
public class ReachabilityTest {

	@BeforeClass
    public void createFolder() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testReachability() throws Exception {
		//test ("jhh", "myExample1", false, 10, 0);
		test ("toyExamples", "question", false, 10, 2);
		
		/*// correct because after unfolding all runs can be forced by strategy to reach place-to-reach
		test ("burglar", "burglar", false, 10, 0);
		test ("burglar", "burglar", true, 10, 2);
		
		// correct because as soon as sys is unfolded twice it can make the proper decisions
		test ("firstExamplePaper", "firstExamplePaper", false, 10, 0);
		test ("firstExamplePaper", "firstExamplePaper", false, 10, 2);
		test ("firstExamplePaper", "firstExamplePaper", true, 10, 3);
		
		// correct because there is no det strategy regardless the winning condition
		test ("ndet", "nondet", false, 10, 0);
		test ("ndet", "nondet", false, 10, 2);
		test ("ndet", "nondet2", false, 10, 2);
		
		// correct because there is an infinite env-loop
		test ("toyExamples", "infiniteA", false, 10, 0);
		test ("toyExamples", "infiniteB", false, 10, 0);
	
		// correct as for some env behavior the place-to-reach is not reached
		test ("toyExamples", "notReachable", false, 10, 0);
		test ("toyExamples", "notReachable", false, 10, 2);
		
		// correct because other sys player has to activate transition which leads to loop avoiding place-to-reach
		test ("toyExamples", "type2A", false, 10, 0);
		test ("toyExamples", "type2A", false, 10, 2);
		test ("toyExamples", "type2B", false, 10, 0);
		test ("toyExamples", "type2B", false, 10, 2);
		
		// correct because the strategy cannot both force reaching the place and afterwards be deadlock-avoiding
		test ("toyExamples", "question", false, 10, 0)*/
	}

	private void test(String folder, String name, boolean result, int n, int b) throws Exception {
		final String path = System.getProperty("examplesfolder") + File.separator + "reachability" + File.separator + folder + File.separator + name + ".apt";
		PetriNet pn = Tools.getPetriNet(path);
		QBFReachabilitySolver sol = new QBFReachabilitySolver(pn, new Reachability(), new QBFSolverOptions(n, b));
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
