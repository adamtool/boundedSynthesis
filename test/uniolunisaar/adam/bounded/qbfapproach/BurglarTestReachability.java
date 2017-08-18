package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFReachabilitySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.SecuritySystem;

@Test
public class BurglarTestReachability {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSecSys() throws Exception {
		//oneTest(2, 8, 2, true);
		oneTest(3, 10, 2, true);
		//oneTest(2, 6, 2, false);
		//oneTest(2, 7, 1, false);
	}
	
	private void oneTest(int problemSize, int n, int b, boolean result) throws Exception {
		PetriNet pn = SecuritySystem.createReachabilityVersion(problemSize, false);
		QBFReachabilitySolver sol = new QBFReachabilitySolver(pn, new QBFSolverOptions(n, b));
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
	
}
