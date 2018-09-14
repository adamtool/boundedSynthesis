package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.SelfOrganizingRobots;

@Test
public class SRTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSR() throws Exception { // only with simplifier no timeout
		oneTest(2, 1, 6, 1, false);
		oneTest(2, 1, 5, 2, false);
		oneTest(2, 1, 6, 2, true);
		//oneTest(3, 1, 7, 2, true);
		//oneTest(4, 1, 8, 2, true);
	}

	private void oneTest(int robot1, int robot2, int n, int b, boolean result) throws Exception {
		PetriGame pg = SelfOrganizingRobots.generate(robot1, robot2, true, true);
		testGame(pg, n, b, result);
	}
}
