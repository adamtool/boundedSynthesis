package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.Clerks;

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
		for (int i = 1; i <= 1; ++i) {
			oneTest(i, j, 0);
			j += 2;
		}
	}

	private void oneTest(int problemSize, int n, int b) throws Exception {
		PetriNet pn = Clerks.generateCP(problemSize, true, true);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}