package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.generators.Clerks;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;

/*
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class DWsTest { // Document Workflow / DW

	// j = 4 -> UNSAT; j = 5 -> SAT
	// ADAM bis 20
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testDWs() throws Exception {
		runTest(true, 5, 5);
		runTest(false, 5, 4);
	}

	private void runTest (boolean tf, int max, int n) throws Exception {
		int j = n; 
		for (int i = 1; i <= max; ++i) {
			if (tf)
				oneTestTrue(i, j, 0);
			else
				oneTestFalse(i, j, 0);
			j += 2;
		}
	}
	
	private void oneTestTrue(int problemSize, int n, int b) throws Exception {
		PetriGame pn = Clerks.generateCP(problemSize, true, true);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
	}
	
	private void oneTestFalse(int problemSize, int n, int b) throws Exception {
		PetriGame pn = Clerks.generateCP(problemSize, true, true);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(n, b));
		Assert.assertFalse(sol.existsWinningStrategy());
	}
}
