package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.Clerks;
import uniolunisaar.adam.tools.Tools;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;

/*
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class DWTest { // Document Workflow / DW

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testClerks() throws Exception {

		int j = 8; // j = 7 -> UNSAT; j = 8 -> SAT
		for (int i = 1; i <= 5; ++i) {
			oneTestTrue(i, j, 0);
			oneTestFalse(i, j - 1, 0);
			j += 2;
		}
	}

	private void oneTestTrue(int problemSize, int n, int b) throws Exception {
		PetriNet pn = Clerks.generateNonCP(problemSize, true, true);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
	}
	
	private void oneTestFalse(int problemSize, int n, int b) throws Exception {
		PetriNet pn = Clerks.generateNonCP(problemSize, true, true);
		Tools.savePN2PDF("before", pn, true);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		boolean bool = sol.existsWinningStrategy();
		if (bool) Tools.savePN2PDF("strategy", sol.getStrategy(), true);
		Assert.assertFalse(sol.existsWinningStrategy());
	}
}
