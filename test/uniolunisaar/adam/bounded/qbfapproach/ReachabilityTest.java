package uniolunisaar.adam.bounded.qbfapproach;

import java.io.File;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class ReachabilityTest extends EmptyTest {

	@BeforeClass
    public void createFolder() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testExistsReachability() throws Exception {
		// testing unfair loop, i.e., environment decision against reaching the good place
		test ("unfair", "unfair", false, 10, 0);
		test ("unfair", "unfair2", false, 10, 0);
		test ("unfair", "unfair3", true, 10, 0);
		
		// correct because after unfolding all runs can be forced by strategy to reach place-to-reach
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
		
		// Nothing special, problematic for BDDs
		test ("toyExamples", "dontCutEnv_long", true, 10, 0);
		test ("toyExamples", "dontCutEnv_long", true, 5, 0);
		test ("toyExamples", "dontCutEnv_long", false, 4, 0);
		test ("toyExamples", "dontCutEnv", true, 10, 0);
		test ("toyExamples", "dontCutEnv", true, 4, 0);
		test ("toyExamples", "dontCutEnv", false, 3, 0);
		
		// correct because unfair loops should not change the result
		test ("toyExamples", "infiniteA", true, 10, 0);
		test ("toyExamples", "infiniteB", false, 10, 0);
		test ("toyExamples", "infiniteC", false, 10, 0);
		test ("toyExamples", "infiniteD", true, 12, 0);
	
		// correct as for some env behavior the place-to-reach is not reached
		test ("toyExamples", "notReachable", false, 10, 0);
		test ("toyExamples", "notReachable", false, 10, 2);
		test ("toyExamples", "simple", false, 10, 0);
		test ("toyExamples", "question", true, 10, 2);
		test ("toyExamples", "shortestStrategy0", true, 10, 0);
		test ("toyExamples", "shortestStrategy1", true, 10, 0);
		
		// correct because other sys player has to activate transition which leads to loop avoiding place-to-reach
		test ("toyExamples", "type2A", true, 10, 0);
		test ("toyExamples", "type2A", true, 10, 2);
		test ("toyExamples", "type2B", true, 10, 0);
		test ("toyExamples", "type2B", true, 10, 2);
	}

	private void test(String folder, String name, boolean result, int n, int b) throws Exception {
		final String path = System.getProperty("examplesfolder") + File.separator + "reachability" + File.separator + folder + File.separator + name + ".apt";
		testPath(path, n, b, result);
	}
}
