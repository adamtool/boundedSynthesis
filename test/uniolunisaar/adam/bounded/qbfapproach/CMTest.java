package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.Workflow;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class CMTest extends EmptyTest { // Concurrent Machines / WF
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCM() throws Exception {
		oneTest(2, 1, 6, 3, true);
		oneTest(3, 1, 6, 3, true);
		oneTest(4, 1, 6, 3, true);
		if (QbfControl.trueConcurrent) {
			oneTest(3, 2, 7, 3, true);
			if (!QbfControl.fastTests) {
				oneTest(4, 2, 7, 3, true);
			}
		} else {
			oneTest(3, 2, 8, 3, true);
			if (!QbfControl.fastTests) {
				oneTest(4, 2, 8, 3, true);
			}
		}
	}

	private void oneTest(int ps1, int ps2, int n, int b, boolean result) throws Exception {
		PetriGameWithTransits pg = Workflow.generate(ps1, ps2, true, true);
		testGame(pg, n, b, result);
	}
}

