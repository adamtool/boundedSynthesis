package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 * TODO categorize what is tested, look at ALL existing example files
 */

@Test
public class SafetyTest extends EmptyTest {
	
	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
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
		//PNWTTools.saveAPT("deploy/accesscontrolToy", OfficeBenchmark.generateOfficeToy(), false);
		//PNWTTools.saveAPT("deploy/accesscontrolSmall", OfficeBenchmark.generateOfficeSmall(), false);	
		//oneTest("tests/watchdog5", 15, 3, true);		// TODO search for bounds
		//oneTest("container/container", 10, 2, true);	// TODO search for bounds
		//oneTest("notConcurrencyPreservingTests/toMakeCP", 20, 2, false);	// TODO nets are unsafe, making them safe defeats their purpose
		//oneTest("notConcurrencyPreservingTests/madeCP", 20, 2, true);		// TODO nets are unsafe, making them safe defeats their purpose
		//oneTest("boundedunfolding/counterexample", 10, 0, true);
		//oneTest("boundedunfolding/firstTry", 15, 0, true);
		//oneTest("boundedunfolding/secondTry", 15, 0, true);
		oneTest("ndet/nondet_motivationForSchedulingChange", 20, 0, false);
		//oneTest("boundedunfolding/finiteWithBad", 10, 2, true);
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
		oneTest("ndet/nondet", 5, 2, false);
		//oneTest("burglar/burglar", 7, 3, true);
		//oneTest("burglar/burglar", 6, 2, false);
		//oneTest("container/container", 20, 0, false);
		int bound = 4;
		//if (trueconcurrent) bound = 3; // TODO why did i think an error is here?
		oneTest("firstExamplePaper/firstExamplePaper", bound, 3, false);
		//oneTest("firstExamplePaper/firstExamplePaper", bound + 1, 3, true);
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
		/*oneTest("nm/sendingprotocol", 6, 2, true);
		oneTest("nm/sendingprotocol", 5, 2, false);
		oneTest("nm/sendingprotocolTwo", 12, 2, true);
		oneTest("nm/sendingprotocolTwo", 11, 2, false);*/
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
