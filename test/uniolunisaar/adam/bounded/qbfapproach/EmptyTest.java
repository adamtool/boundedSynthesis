package uniolunisaar.adam.bounded.qbfapproach;

import static org.testng.Assert.assertTrue;

import org.testng.Assert;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.logic.util.AdamTools;

public abstract class EmptyTest {

	protected void nextTest (QbfSolver<?> sol, int n, int b, boolean result) throws Exception {
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		
		AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding, false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		
		Assert.assertEquals(sol.existsWinningStrategy(), result);

		if (sol.existsWinningStrategy()) {
			assertTrue(QbfSolver.checkStrategy(sol.originalGame, sol.strategy));	// ORIGINAL: check validity of strategy if existent
		}
	}
}
