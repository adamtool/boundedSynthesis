package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.ManufactorySystem;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.winningconditions.Safety;

@Test
public class JPTest { // Job Processing

	// non-deterministic 2, 7, 3
	//     deterministic 2, 7, 2
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testManufactorSystem() throws Exception {
		oneTest(2, 7, 3); // -> FAST
		//oneTest(3, 4, 1);
		//oneTest(4, 4, 1);
		//oneTest(2, 9, 6);
		//oneTest(2, 5, 9);
		//oneTest (3, 9, 6); // -> timeout
		//oneTest (3, 7/8/9/10, 3); // -> UNSAT
		//oneTest(3, 8, 4);
	}

	private void oneTest(int a, int n, int b) throws Exception {
		PetriNet pn = ManufactorySystem.generate(a, true, true, true);
		QBFSafetySolver sol = new QBFSafetySolver(pn,new Safety(),  new QBFSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}
