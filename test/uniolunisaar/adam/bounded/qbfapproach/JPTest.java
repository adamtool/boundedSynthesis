package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.generators.ManufactorySystem;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.logic.util.AdamTools;

@Test
public class JPTest { // Job Processing

	// non-deterministic 2, 7, 3
	// deterministic 2, 7, 2

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testManufactorSystem() throws Exception {
		oneTest(2, 7, 3); // -> FAST
		// oneTest(3, 4, 1);
		// oneTest(4, 4, 1);
		// oneTest(2, 9, 6);
		// oneTest(2, 5, 9);
		// oneTest (3, 9, 6); // -> timeout
		// oneTest (3, 7/8/9/10, 3); // -> UNSAT
		// oneTest(3, 8, 4);
	}

	private void oneTest(int a, int n, int b) throws Exception {
		PetriGame pn = ManufactorySystem.generate(a, true, true, true);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(n, b));
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
