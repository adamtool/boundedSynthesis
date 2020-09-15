package uniolunisaar.adam.tests.synthesis.bounded.qbfapproach.tokenflows;

import org.testng.annotations.Test;

import uniolunisaar.adam.tests.synthesis.bounded.qbfapproach.EmptyTest;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.LoopUnrolling;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class LoopUnrollingTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testLoopUnrolling() throws Exception {
		oneTest(1, true, 10, 0, false);
		oneTest(1, false, 10, 0, true);
		//oneTest(2, true, 10, 0, false); // TODO understand whether error or algorithm/implementation wrong..
		oneTest(2, false, 10, 0, true);
	}
	
	private void oneTest(int problemSize, boolean newChain, int n, int b, boolean result) throws Exception {
		PetriGameWithTransits pg = LoopUnrolling.createESafetyVersion(problemSize, newChain, false);
		testGame(pg, n, b, result);
	}
}
