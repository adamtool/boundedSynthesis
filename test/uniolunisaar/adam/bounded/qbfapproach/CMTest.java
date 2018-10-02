package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.games.Workflow;

@Test
public class CMTest extends EmptyTest { // Concurrent Machines / WF

	// i-1-6-3
	// i-2-6-3
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCM() throws Exception {
		for (int i = 2; i <= 2; ++i) {
			oneTest(i, 1, 6, 3, true);
		}
		//oneTest(3, 2, 6, 4, true);	// very long but possible
	}

	private void oneTest(int ps1, int ps2, int n, int b, boolean result) throws Exception {
		PetriGame pg = Workflow.generate(ps1, ps2, true, true);
		testGame(pg, n, b, result);
	}
}
