package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QbfConSolverFactory;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QbfConSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.util.PGTools;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public abstract class EmptyTest {

	protected boolean trueconcurrent = false;
	protected static boolean fast = true;

	protected void testPath (String path, int n, int b, boolean result) throws Exception {
		if (trueconcurrent) {
			testSolver(QbfConSolverFactory.getInstance().getSolver(path, new QbfConSolverOptions(n, b)), n, b, result);
		} else {
			testSolver(QbfSolverFactory.getInstance().getSolver(path, new QbfSolverOptions(n, b)), n, b, result);
		}
	}
	
	protected void testGame (PetriGame pg, int n, int b, boolean result) throws Exception {            
               
		if (trueconcurrent) { 
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
			Assert.assertEquals(QbfControl.checkStrategy(originalGame, sol.getStrategy()), true);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
