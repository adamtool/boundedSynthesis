package uniolunisaar.adam.tests.synthesis.bounded.qbfapproach.tokenflows;

import org.testng.annotations.Test;

import uniolunisaar.adam.tests.synthesis.bounded.qbfapproach.EmptyTest;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.SecuritySystem;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class BurglarTestReachability extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSecSys() throws Exception {		// TODO work on scalability, thought reach instead of safety might help, but so far it does not
		//oneTest(2, 8, 2, true);
		//oneTest(3, 10, 2, true); // not working
		oneTest(2, 6, 2, false);
		//oneTest(2, 7, 1, false);
	}
	
	private void oneTest(int problemSize, int n, int b, boolean result) throws Exception {
		PetriGameWithTransits pg = SecuritySystem.createReachabilityVersion(problemSize, false);
		testGame(pg, n, b, result);
	}
	
}
