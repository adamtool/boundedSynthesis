package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

/**
 * 
 * @author Jesko Hecking-Harbusch and Niklas Metzger
 *
 */

@Test
public class TrueConcurrencyTest extends EmptyTest {
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testTrueConcurrent() throws Exception {
			oneTest("nm/independentNets", 10, 0, true);
			oneTest("nm/minimal", 5, 2, false);
			oneTest("nm/minimalNonCP", 5, 2, false);
			oneTest("nm/minimalNotFinishingEnv", 5, 2, false);
			oneTest("nm/minimalOnlySys", 5, 2, false);
			oneTest("nm/nounfolding", 5, 0, true);
			oneTest("nm/oneunfolding", 5, 0, true);
		if (QbfControl.trueConcurrent) {
			oneTest("nm/sendingprotocol", 6, 2, true);
			oneTest("nm/sendingprotocolTwo", 11, 2, true);
			oneTest("nm/trueconcurrent", 3, 0, true);
		} else {
			oneTest("nm/sendingprotocol", 6, 2, true);
			oneTest("nm/sendingprotocolTwo", 12, 2, true);
			oneTest("nm/trueconcurrent", 5, 0, true);
		}
	}

	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallsafety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
