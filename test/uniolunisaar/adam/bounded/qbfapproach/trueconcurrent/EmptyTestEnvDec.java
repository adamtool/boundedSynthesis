package uniolunisaar.adam.bounded.qbfapproach.trueconcurrent;

import org.testng.Assert;

import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QbfConSolver;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QbfConSolverFactory;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QbfConSolverOptions;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.util.PNWTTools;

/**
 * 
 * @author Niklas Metzger
 *
 */

public abstract class EmptyTestEnvDec {
	
	protected void testPath (String path, int n, int b, boolean result) throws Exception {
		QbfConSolver<? extends Condition> sol = QbfConSolverFactory.getInstance().getSolver(path, new QbfConSolverOptions(n, b));
		testSolver(sol, n, b, result);
	}
	
	protected void testGame (PetriGame pg, int n, int b, boolean result) throws Exception {
		QbfConSolver<? extends Condition> sol = QbfConSolverFactory.getInstance().getSolver(pg, false, new QbfConSolverOptions(n, b));
		testSolver(sol, n, b, result);
	}
 
	protected void testSolver (QbfConSolver<? extends Condition> sol, int n, int b, boolean result) throws Exception {
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
        PNWTTools.savePnwt2Dot("originalGame", sol.originalGame, false);
		PNWTTools.savePnwt2Dot("unfolding", sol.unfolding, false);
		if (sol.existsWinningStrategy()) {
			PNWTTools.savePnwt2Dot("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
		
		if (sol.existsWinningStrategy()) {
			assert(QbfControl.checkStrategy(sol.originalGame, sol.strategy));	// ORIGINAL: check validity of strategy if existent
		}
	}
}
