package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfAReachabilitySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.generators.SecuritySystem;

@Test
public class BurglarTestReachability {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSecSys() throws Exception {		// TODO work on scalability, thought reach isntead of safety might help, but so far it does not
		//oneTest(2, 8, 2, true);
		//oneTest(3, 10, 2, true); // not working
		oneTest(2, 6, 2, false);
		//oneTest(2, 7, 1, false);
	}
	
	private void oneTest(int problemSize, int n, int b, boolean result) throws Exception {
		PetriGame pn = SecuritySystem.createReachabilityVersion(problemSize, false);
		QbfAReachabilitySolver sol = new QbfAReachabilitySolver(pn,  new Reachability(), new QBFSolverOptions(n, b));
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
	
}
