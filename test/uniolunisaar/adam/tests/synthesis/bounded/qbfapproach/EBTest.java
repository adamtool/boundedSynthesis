package uniolunisaar.adam.tests.synthesis.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.EmergencyBreakdown;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class EBTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testEB() throws Exception {
		if (QbfControl.trueConcurrent) {
			// TODO strategies are not accepted, same problem as in safetyTest with DLA?
			//oneTest(1, 1, 9, 2);
			if (!QbfControl.fastTests) {
				//oneTest(0, 2, 9, 2);
				//oneTest(2, 0, 9, 2);
			}
		} else {
			// TODO not working with the current unfolders?
			//oneTest(1, 1, 13, 2);
			if (!QbfControl.fastTests) {
				//oneTest(0, 2, 13, 2);
				//oneTest(2, 0, 13, 2);
			}
		}
	}

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriGameWithTransits pg = EmergencyBreakdown.createSafetyVersion(ps1, ps2, false);
		testGame(pg, n, b, true);
	}
}
