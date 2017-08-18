package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.Workflow;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;

@Test
public class CMTest { // Concurrent Machines / WF

	// i-1-6-3
	// i-2-6-3
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCM() throws Exception {
		for (int i = 2; i <= 9; ++i) {
			oneTest(i, 1, 6, 3);
		}
		//oneTest(3, 2, 6, 4);	// very long but possible
	}

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriNet pn = Workflow.generate(ps1, ps2, true, true);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
		System.out.println("PLACES " + ps1 + " " + sol.getStrategy().getPlaces().size());
		System.out.println("TRANSITIONS " + ps1 + " " + sol.getStrategy().getTransitions().size());
	}
}
