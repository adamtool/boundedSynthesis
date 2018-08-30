package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.SelfOrganizingRobots;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.logic.util.AdamTools;

@Test
public class SRTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSR() throws Exception { // only with simplifier no timeout
		oneTest(2, 1, 6, 2);
		//oneTest(3, 1, 7, 2);
		// oneTest(4, 1, 8, 2);
	}

	private void oneTest(int robot1, int robot2, int n, int b) throws Exception {
		PetriGame pn = SelfOrganizingRobots.generate(robot1, robot2, true, true);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QBFSolverOptions(n, b));
		sol.existsWinningStrategy(); // calculate first, then output games, and then check for correctness
		// TODO put this to an appropriate place in code
		AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding, false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}
