package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class SafetyTest extends EmptyTest {
	
	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
		//Logger.getInstance().setVerbose(true);
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testLoops() throws Exception {
		if (!QbfControl.rebuildingUnfolder) { // simple examples with loops
			oneTest("jhh/robots_true", 19, 0, true);
			oneTest("jhh/robots_false", 20, 0, false);
			oneTest("constructedExample/constructedExample", 3, 0, false);
			oneTest("constructedExample/constructedExample", 4, 0, true);
			oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 3, 0, false);
			oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 4, 0, true);			// TODO loop despite name?
			oneTest("ma_vsp/vsp_1_withBadPlaces", 3, 0, true);
			int bound = 3;
			if (trueconcurrent) bound = 2;
			oneTest("jhh/myexample7", bound, 0, false);
			oneTest("jhh/myexample7", bound + 1, 0, true);
		}
	}
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testUnfoldingBounds() throws Exception {
		if (!QbfControl.rebuildingUnfolder) {
			oneTest("ndet/nondet_s3", 10, 0, false);
		}
		oneTest("ndet/nondet_s3", 10, 2, true);
		if (!QbfControl.rebuildingUnfolder) {
			oneTest("jhh/myexample3", 10, 0, false);
		}
		oneTest("jhh/myexample3", 10, 2, true);
		if (!QbfControl.rebuildingUnfolder) {
			oneTest("ndet/nondet_s3", 10, 0, false);
		}
		oneTest("ndet/nondet_s3", 10, 2, true);
	}
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testTrueConcurrent() throws Exception {
		int bound = 3;
		if (trueconcurrent) bound = 2;
		oneTest("deadlock/missDeadlock", bound, 0, false);
		oneTest("deadlock/missDeadlock", bound + 1, 0, true);
	}
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testGeneralExamples() throws Exception {
		//oneTest("tests/watchdog5", 15, 3, true);		// TODO search for bounds
		//oneTest("container/container", 10, 2, true);	// TODO search for bounds
		oneTest("notConcurrencyPreservingTests/madeCP", 6, 0, false);
		//oneTest("2env/paul", 10, 0, true);	// TODO continue including more examples
		oneTest("boundedunfolding/counterexample", 10, 0, true);
		if (!fast) {
			oneTest("boundedunfolding/firstTry", 10, 3, true); // TODO why not working for SEQ? TC strategy rejected because of env transition
			oneTest("boundedunfolding/secondTry", 15, 2, true);
		}
		oneTest("ndet/nondet_motivationForSchedulingChange", 20, 0, false);
		//oneTest("boundedunfolding/finiteWithBad", 10, 2, true); unreachable env removed?
		oneTest("jhh/myexample1", 10, 0, false);
		oneTest("ndet/nondet_s3_noStrat", 15, 2, false);
		oneTest("ndet/nondet_unnecessarily_noStrat", 15, 3, false);
		oneTest("ndet/nondet_withBad", 12, 2, false);
		oneTest("ndet/nondet_jhh1", 20, 0, false);
		oneTest("ndet/nondet_jhh2", 20, 0, true);
		oneTest("ndet/nondet_jhh3", 20, 0, false);
		oneTest("ndet/nondet_motivationForSchedulingChange", 5, 0, false);
		oneTest("jhh/myexample1", 10, 0, false);
		oneTest("jhh/myexample1", 10, 2, false);
		oneTest("jhh/myexample2", 10, 0, true);
		oneTest("jhh/myexample2", 10, 2, true);
		oneTest("jhh/myexample4", 10, 0, false);
		oneTest("jhh/myexample4", 10, 2, false);
		oneTest("jhh/myexample5", 20, 0, true);
		//oneTest("jhh/myexample8", 10, 0, true);  unreachable env removed?
		oneTest("ndet/nondet", 5, 2, false);
		int bound = 7;
		if (trueconcurrent) bound = 6;
		oneTest("burglar/burglar", bound, 2, true);
		oneTest("burglar/burglar", bound, 1, false);
		oneTest("container/container", 20, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper", 4, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper", 5, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 10, false);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 2, 0, false);
		oneTest("ndet/nondet_s3_noStrat", 15, 2, false);
		oneTest("ndet/nondet_unnecessarily_noStrat", 15, 3, false);
		oneTest("ndet/nondet_withBad", 12, 2, false);
		oneTest("ndet/nondet_jhh1", 20, 0, false);
		oneTest("ndet/nondet_jhh2", 20, 0, true);
		oneTest("ndet/nondet_jhh3", 20, 0, false);
		oneTest("testingNets/envSkipsSys", 15, 3, false);
		oneTest("nm/sendingprotocol", 6, 2, true);
		oneTest("nm/sendingprotocol", 5, 2, false);
		if (!fast) {
			bound = 12;
			if (trueconcurrent) bound = 10;
			oneTest("nm/sendingprotocolTwo", bound, 2, true);	// TODO wrong strategy for TC; strategy is not deadlock-avoiding?
			oneTest("nm/sendingprotocolTwo", bound - 1, 2, false);
		}
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
