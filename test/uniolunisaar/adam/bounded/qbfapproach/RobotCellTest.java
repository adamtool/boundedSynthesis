package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.RobotCell;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class RobotCellTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testRobotCell() throws Exception {// constant larger +2
		// 2/3/4/5/6/7/8/9 0 3 0, 2/3/4/5/6/7 1 5 0, 2/3/4/5 2 7 0, 2/3 3 9 0, 2 4 11 0, 2 5 13 0
		int max = 6;
		if (QbfControl.fastTests) {max = 4;}
		int n = 5;		// 5 7 9
		int increase = 2;
		if (QbfControl.trueConcurrent) {
			n = 4;
			increase = 2;
		} // 3 4 5
		for (int i = 2; i < max + 2; ++i) {
			oneTest(i, 1, n, 0);
		}
		for (int i = 2; i < max + 1; ++i) {
			oneTest(i, 2, n + increase, 0);
		}
		for (int i = 2; i < max; ++i) {
			oneTest(i, 3, n + (2 * increase), 0);
		}
	}

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriGameWithTransits pg = RobotCell.generate(ps1, ps2, true);
		testGame(pg, n, b, true);
	}
}
