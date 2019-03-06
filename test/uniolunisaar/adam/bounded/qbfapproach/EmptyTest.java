package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QbfConSolverFactory;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QbfConSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.util.PNWTTools;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public abstract class EmptyTest {

	protected boolean trueconcurrent = false;

	protected void testPath (String path, int n, int b, boolean result) throws Exception {
		if (trueconcurrent) {
			testSolver(QbfConSolverFactory.getInstance().getSolver(path, new QbfConSolverOptions(n, b)), n, b, result);
		} else {
			testSolver(QbfSolverFactory.getInstance().getSolver(path, new QbfSolverOptions(n, b)), n, b, result);
		}
	}
	
	protected void testGame (PetriGame pg, int n, int b, boolean result) throws Exception {
		if (trueconcurrent) {
			testSolver(QbfConSolverFactory.getInstance().getSolver(pg, false, new QbfConSolverOptions(n, b)), n, b, result);
		} else {
			testSolver(QbfSolverFactory.getInstance().getSolver(pg, false, new QbfSolverOptions(n, b)), n, b, result);
		}
	}
 
	protected void testSolver (Solver<?,?> sol, int n, int b, boolean result) throws Exception {
		PNWTTools.savePnwt2PDF("originalGame", sol.getGame(), false);
		PetriGame originalGame = new PetriGame(sol.getGame());		// true copy of original game to check strategy for correctness later
        sol.existsWinningStrategy();								// solve game
		PNWTTools.savePnwt2PDF("unfolding", sol.getGame(), false);
		if (sol.existsWinningStrategy()) {
			PNWTTools.savePnwt2PDF("strategy", sol.getStrategy(), false);
			Assert.assertEquals(QbfControl.checkStrategy(originalGame, sol.getStrategy()), true);
		}
		//Assert.assertEquals(sol.existsWinningStrategy(), result);  // TODO fix recognition of finite strategies
	}
}
