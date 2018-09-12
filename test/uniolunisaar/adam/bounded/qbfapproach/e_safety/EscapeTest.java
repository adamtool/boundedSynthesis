package uniolunisaar.adam.bounded.qbfapproach.e_safety;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfESafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.generators.Escape;
import uniolunisaar.adam.logic.util.AdamTools;

@Test
public class EscapeTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCR() throws Exception {
		oneTest(1, 0, 10, 0, true);
		oneTest(1, 1, 10, 0, false);
		oneTest(2, 1, 10, 0, true);
		oneTest(2, 2, 10, 0, false);
		oneTest(2, 3, 10, 0, false);
		oneTest(2, 4, 10, 0, false);
		oneTest(3, 2, 10, 0, true);
		oneTest(4, 2, 10, 0, true);
	}
	
	private void oneTest(int nb_sys, int nb_env, int n, int b, boolean result) throws Exception {
		PetriNet pn = Escape.createESafetyVersion(nb_sys, nb_env, false);
		QbfESafetySolver sol = new QbfESafetySolver(new PetriGame(pn), new Safety(), new QbfSolverOptions(n, b));
		sol.existsWinningStrategy(); // calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding, false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
