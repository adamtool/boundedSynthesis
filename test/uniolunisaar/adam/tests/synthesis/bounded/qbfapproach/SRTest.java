package uniolunisaar.adam.tests.synthesis.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.SelfOrganizingRobots;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class SRTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSR() throws Exception { // only with simplifier no timeout
		oneTest(2, 1, 5, 2, false);
		oneTest(2, 1, 6, 2, true);
		if (!QbfControl.fastTests) {
			oneTest(3, 1, 6, 2, false);
			oneTest(3, 1, 7, 2, true);
		}
		//oneTest(4, 1, 8, 2, true); // TODO debug unfolding
	}

	private void oneTest(int robot1, int robot2, int n, int b, boolean result) throws Exception {
		PetriGameWithTransits pg = SelfOrganizingRobots.generate(robot1, robot2, true, true);
		testGame(pg, n, b, result);
	}
}
