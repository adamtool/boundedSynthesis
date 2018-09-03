package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;

public abstract class EmptyTest {

	protected void nextTest (QbfSolver<?> sol, int n, int b, boolean result) throws Exception {
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
        
        // TODO cannot print nets without tokenflow anymore?
		/*AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding, false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}*/
		
		Assert.assertEquals(sol.existsWinningStrategy(), result);

		if (sol.existsWinningStrategy()) {
			sol.getStrategy(); // TODO remove when printing is possible again.. 
			//assertTrue(QbfSolver.checkStrategy(sol.originalGame, sol.strategy));	// TODO cannot generate strategy... ORIGINAL: check validity of strategy if existent
		}
	}
}
