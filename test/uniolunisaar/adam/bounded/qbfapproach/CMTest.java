package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.Workflow;
import uniolunisaar.adam.tools.Tools;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.winningconditions.Safety;

@Test
public class CMTest { // Concurrent Machines / WF

	// i-1-6-3
	// i-2-6-3
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCM() throws Exception {
		for (int i = 2; i <= 2; ++i) {
			oneTest(i, 1, 6, 3);
		}
		//oneTest(3, 2, 6, 4);	// very long but possible
	}

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriNet pn = Workflow.generate(ps1, ps2, true, true);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new Safety(), new QBFSolverOptions(n, b));
		sol.existsWinningStrategy(); // calculate first, then output games, and then check for correctness
		// TODO put this to an appropriate place in code
		Tools.savePN2PDF("originalGame", sol.game.getNet(), false);
		Tools.savePN2PDF("unfolding", sol.unfolding.getNet(), false);
		if (sol.existsWinningStrategy()) {
			Tools.savePN2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}
