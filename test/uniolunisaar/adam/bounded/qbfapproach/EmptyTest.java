package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QBFConSolverFactory;
import uniolunisaar.adam.bounded.qbfconcurrent.solver.QBFConSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.logic.util.AdamTools;


public abstract class EmptyTest {

	private boolean trueconcurrent = true;

	protected void testPath (String path, int n, int b, boolean result) throws Exception {
		if (trueconcurrent)
			testSolver(QBFConSolverFactory.getInstance().getSolver(path, new QBFConSolverOptions(n, b)),n,b,result);
		else
			testSolver(QbfSolverFactory.getInstance().getSolver(path, new QbfSolverOptions(n, b)),n,b,result);
	}
	
	protected void testGame (PetriGame pg, int n, int b, boolean result) throws Exception {
		if (trueconcurrent)
			testSolver(QBFConSolverFactory.getInstance().getSolver(pg, false, new QBFConSolverOptions(n, b)),n,b,result);
		else
			testSolver(QbfSolverFactory.getInstance().getSolver(pg, false, new QbfSolverOptions(n, b)),n,b,result);

	}
 
	protected void testSolver (Solver<?,?> sol, int n, int b, boolean result) throws Exception {
		AdamTools.savePG2PDF("originalGame", sol.getGame(), false);
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("unfolding", sol.getGame(), false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		
		Assert.assertEquals(sol.existsWinningStrategy(), result);

		if (sol.existsWinningStrategy()) {
			//assertTrue(QbfControl.checkStrategy(sol.originalGame, sol.strategy));	// ORIGINAL: check validity of strategy if existent
		}
	}
}
