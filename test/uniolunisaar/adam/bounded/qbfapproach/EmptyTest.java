package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;

import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach.QbfSolverFactory;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolverOptions;
import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfconcurrent.QbfConSolverFactory;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfconcurrent.QbfConSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.synthesis.solver.Solver;
import uniolunisaar.adam.util.PGTools;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public abstract class EmptyTest {

	protected void testPath (String path, int n, int b, boolean result) throws Exception {
		if (QbfControl.trueConcurrent) {
			testSolver(QbfConSolverFactory.getInstance().getSolver(path, new QbfConSolverOptions(n, b)), n, b, result);
		} else {
			testSolver(QbfSolverFactory.getInstance().getSolver(path, new QbfSolverOptions(n, b)), n, b, result);
		}
	}
	
	protected void testGame (PetriGame pg, int n, int b, boolean result) throws Exception {
		if (QbfControl.trueConcurrent) {
                        QbfConSolverOptions opts = new QbfConSolverOptions(n,b, false);
			testSolver(QbfConSolverFactory.getInstance().getSolver(pg, opts), n, b, result);
		} else { 
                        QbfSolverOptions opts = new QbfSolverOptions(n,b, false);
			testSolver(QbfSolverFactory.getInstance().getSolver(pg, opts), n, b, result);
		}
	}
 
	protected void testSolver (Solver<PetriGame,?,?,?> sol, int n, int b, boolean result) throws Exception {
		PGTools.savePG2PDF("originalGame", sol.getGame(), false);
		PetriGame originalGame = new PetriGame(sol.getGame());		// true copy of original game to check strategy for correctness later
        sol.existsWinningStrategy();								// solve game
        PGTools.savePG2PDF("unfolding", sol.getGame(), false);
		if (sol.existsWinningStrategy()) {
			PGTools.savePG2PDF("strategy", sol.getStrategy(), false);
			// check correctness of strategy:
			Assert.assertTrue(QbfControl.checkStrategy(originalGame, sol.getStrategy()));
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
