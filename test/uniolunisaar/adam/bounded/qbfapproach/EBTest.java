package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.EmergencyBreakdown;
import uniolunisaar.adam.tools.Tools;

@Test
public class EBTest {

	// 1 1 10 2 timeout
	// 1 1 11 2 timeout

	// 1 1 14 24 DETERMINISTIC unfolder 75 Sekunden
	// 2 0 14 24 DETERMINISTIC unfolder 50 Sekunden
	// 0 2 14 24 DETERMINISTIC unfolder 75 Sekunden
	// 1 1 14 2 NONDETERMINISTIC unfolder 2600, 4500 Sekunden, 2 stds...

	@Test(timeOut = 18000 * 1000) // 30 min
	public void testEB() throws Exception {
		oneTest(1, 1, 20, 2); // TODO go back to 14 and evaluate
	}

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriNet pn = EmergencyBreakdown.createSafetyVersion(ps1, ps2, false);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
		Tools.savePN2PDF("unfolding", sol.unfolding.getNet(), false);
		Tools.savePN2PDF("strategy", sol.getStrategy(), false);
	}
}
