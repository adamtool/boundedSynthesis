package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.generators.EmergencyBreakdown;
import uniolunisaar.adam.logic.util.AdamTools;

@Test
public class EBTest {

	// 1 1 10 2 timeout
	// 1 1 11 2 timeout

	// 1 1 14 24 DETERMINISTIC unfolder 75 Sekunden
	// 2 0 14 24 DETERMINISTIC unfolder 50 Sekunden
	// 0 2 14 24 DETERMINISTIC unfolder 75 Sekunden
	// 1 1 14 2 NONDETERMINISTIC unfolder 2600, 4500 Sekunden, 2 stds...
	// 1 1 13 2 FORnondeterministicUNFOLDER
	// 2 0 13 2 FORnondeterministicUNFOLDER
	// 0 2 13 2 FORnondeterministicUNFOLDER

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testEB() throws Exception {
		//oneTest(1, 1, 13, 2);
	}

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriGame pn = EmergencyBreakdown.createSafetyVersion(ps1, ps2, false);
		QbfASafetySolver sol = new QbfASafetySolver(pn,new Safety(),  new QbfSolverOptions(n, b));
		sol.existsWinningStrategy(); // calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding, false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}
