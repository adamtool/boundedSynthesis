package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.RobotCell;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;

/*
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class RobotCellTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testRobotCell() throws Exception {// constant larger +2
		// 2/3/4/5/6/7/8/9 0 3 0, 2/3/4/5/6/7 1 5 0, 2/3/4/5 2 7 0, 2/3 3 9 0, 2 4 11 0, 2 5 13 0 <= 215s (bloqqer) (<=185 !bloqqer) 1st 9, 2nd 8, 3rd 6 timeout
		long start = System.currentTimeMillis();

		for (int i = 2; i < 5; ++i) {
			oneTest(i, 1, 5, 0);
		}
		for (int i = 2; i < 4; ++i) {
			oneTest(i, 2, 7, 0);
		}
		for (int i = 2; i < 3; ++i) {
			oneTest(i, 3, 9, 0);
		}

		long end = System.currentTimeMillis();
		long result = end - start;
		System.out.println("Elapsed time for RobotCellTest: " + 1.0 * result / 1000 + "s");
	}

	// no memory
	// non det bis 10/9/8/7/6/5: 872.24sec
	// det bis 10/9/8/7/6/5: 861.268sec

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriGame pn = RobotCell.generate(ps1, ps2, true);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(),new QBFSolverOptions(n, b));
		Assert.assertTrue(sol.existsWinningStrategy());
	}
}
