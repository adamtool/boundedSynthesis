package uniolunisaar.adam.bounded.qbfapproach.trueconcurrent;

import static org.testng.Assert.assertTrue;

import org.testng.Assert;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QBFConSolver;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QBFConSolverFactory;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QBFConSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.logic.util.AdamTools;

public abstract class EmptyTest {
	
	protected void testPath (String path, int n, int b, boolean result) throws Exception {
		QBFConSolver<? extends WinningCondition> sol = QBFConSolverFactory.getInstance().getSolver(path, new QBFConSolverOptions(n, b));
		testSolver(sol, n, b, result);
	}
	
	protected void testGame (PetriGame pg, int n, int b, boolean result) throws Exception {
		QBFConSolver<? extends WinningCondition> sol = QBFConSolverFactory.getInstance().getSolver(pg, false, new QBFConSolverOptions(n, b));
		testSolver(sol, n, b, result);
	}
 
	protected void testSolver (QBFConSolver<?> sol, int n, int b, boolean result) throws Exception {
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