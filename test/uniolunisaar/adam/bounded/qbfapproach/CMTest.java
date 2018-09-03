package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.generators.Workflow;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.logic.util.AdamTools;

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
		PetriGame pn = Workflow.generate(ps1, ps2, true, true);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(n, b));
		sol.existsWinningStrategy(); // calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding, false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}
