package uniolunisaar.adam.tests.synthesis.bounded.qbfapproach;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class SafetyTest extends EmptyTest {
	
	@BeforeClass
    public void setProperties() {
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
			if (QbfControl.trueConcurrent) bound = 2;
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
		if (QbfControl.trueConcurrent) bound = 2;
		oneTest("deadlock/missDeadlock", bound, 0, false);
		oneTest("deadlock/missDeadlock", bound + 1, 0, true);
	}
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testGeneralExamples() throws Exception {
		//oneTest("boundedunfolding/txt2", 25, 3, true);
		//oneTest("boundedunfolding/txt", 25, 3, true); //
		//oneTest("boundedunfolding/secondTry", 15, 3, true);
		//TODO continue including more examples
		oneTest("tests/watchdog5", 15, 3, true);
		//oneTest("container/container", 10, 2, true);	// TODO search for bounds
		//oneTest("2env/paul", 10, 2, true); // TODO find corresponding unfolding to find winning strategy?
		oneTest("boundedunfolding/causalmemory", 15, 2, false);
		oneTest("notConcurrencyPreservingTests/madeCP", 6, 0, false);
		//oneTest("notConcurrencyPreservingTests/madeCP", 15, 2, false); // TODO search for bug of new unfolder here
		oneTest("boundedunfolding/finite1", 15, 2, true); // OLD cutoff is too early in McMillianUnfolder
		oneTest("boundedunfolding/finite2", 10, 2, true);
		oneTest("boundedunfolding/finiteWithBad", 10, 2, true);
		oneTest("boundedunfolding/finite3", 10, 2, true); 
		oneTest("boundedunfolding/counterexample", 10, 0, true);
		if (!QbfControl.fastTests) {
			// TODO debug these four for unfolder? take long and use NondeterministicUnfolder?
			//oneTest("boundedunfolding/unfolding1", 15, 2, true);
			//oneTest("boundedunfolding/unfolding2", 20, 2, true); // 20 is necessary for current unfolder; strategy NOT accepted
			//oneTest("boundedunfolding/firstTry", 15, 3, true); // TODO why not working for SEQ? TC strategy rejected because of env transition
			//oneTest("boundedunfolding/secondTry", 15, 3, true);
		}
		oneTest("ndet/nondet_motivationForSchedulingChange", 20, 0, false);
		oneTest("jhh/myexample1", 10, 0, false);
		oneTest("jhh/onlySysCPandSliceable", 10, 0, true);		// TODO used for testing the new binary encoding
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
		oneTest("cornercases/unreachableEnvTransition", 10, 0, true);
		oneTest("cornercases/unreachableEnvTransition2", 10, 0, true);
		oneTest("ndet/nondet", 5, 2, false);
		int bound = 7;
		if (QbfControl.trueConcurrent) bound = 6;
		oneTest("burglar/burglar", bound, 2, true);
		oneTest("burglar/burglar", bound, 1, false);
		oneTest("container/container", 20, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper", 4, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper", 5, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 10, false);
		oneTest("ndet/nondet_s3_noStrat", 15, 2, false);
		oneTest("ndet/nondet_unnecessarily_noStrat", 15, 3, false);
		oneTest("ndet/nondet_withBad", 12, 2, false);
		oneTest("ndet/nondet_jhh1", 20, 0, false);
		oneTest("ndet/nondet_jhh2", 20, 0, true);
		oneTest("ndet/nondet_jhh3", 20, 0, false);
		oneTest("testingNets/envSkipsSys", 15, 3, false);
		oneTest("nm/sendingprotocol", 25, 2, true); // TODO strategy not accepted
		oneTest("nm/sendingprotocol", 5, 2, false);
		if (!QbfControl.fastTests) {
			bound = 12;
			if (QbfControl.trueConcurrent) bound = 10;
			//oneTest("nm/sendingprotocolTwo", bound, 2, true);	// TODO debug: BOTH encodings are not deadlock-avoiding as there is a loop which is stopped; ForNonDeterministicUnfolder problem?
			oneTest("nm/sendingprotocolTwo", bound - 1, 2, false);
		}
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallsafety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
