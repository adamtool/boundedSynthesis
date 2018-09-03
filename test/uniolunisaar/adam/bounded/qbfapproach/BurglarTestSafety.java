package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.generators.SecuritySystem;

@Test
public class BurglarTestSafety {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSecSys() throws Exception {
		oneTest(2, 7, 2, true);
		oneTest(2, 6, 2, false);
		oneTest(2, 7, 1, false);
	}
	
	private void oneTest(int problemSize, int n, int b, boolean result) throws Exception {
		PetriGame pn = SecuritySystem.createSafetyVersion(problemSize, false);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(n, b));
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
	
}
