package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.SelfOrganizingRobots;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.winningconditions.Safety;

@Test
public class SRTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSR() throws Exception { // only with simplifier no timeout
		oneTest(2, 1, 6, 2);
		//oneTest(3, 1, 7, 2);
		//oneTest(4, 1, 8, 2);
	}

	private void oneTest(int robot1, int robot2, int n, int b) throws Exception {
		PetriNet pn = SelfOrganizingRobots.generate(robot1, robot2, true, true);
		QBFSafetySolver sol = new QBFSafetySolver(pn, new Safety(),  new QBFSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}
