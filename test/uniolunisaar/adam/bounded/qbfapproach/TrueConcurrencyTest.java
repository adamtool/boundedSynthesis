package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import java.io.File;

/**
 * 
 * @author Jesko Hecking-Harbusch and Niklas Metzger
 *
 */

@Test
public class TrueConcurrencyTest extends EmptyTest {
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testTrueConcurrent() throws Exception {
			oneTest("independentNets", 10, 0, true);
			oneTest("minimal", 5, 2, false);
			oneTest("minimalNonCP", 5, 2, false);
			oneTest("minimalNotFinishingEnv", 5, 2, false);
			oneTest("minimalOnlySys", 5, 2, false);
			oneTest("nounfolding", 5, 0, true);
			oneTest("oneunfolding", 5, 0, true);
		if (QbfControl.trueConcurrent) {
			oneTest("sendingprotocol", 6, 2, true);
			oneTest("sendingprotocolTwo", 11, 2, true);
			oneTest("trueconcurrent", 3, 0, true);
		} else {
			oneTest("sendingprotocol", 6, 2, true);
			oneTest("sendingprotocolTwo", 12, 2, true);
			oneTest("trueconcurrent", 5, 0, true);
		}
	}

	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + File.separator + "forallsafety" + File.separator + "nm" + File.separator + str + ".apt";
		testPath(path, n, b, result);
	}
}
