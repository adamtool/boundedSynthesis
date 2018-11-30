package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.synthesis.ManufactorySystem;

@Test
public class JPTest extends EmptyTest { // Job Processing

	// non-deterministic 2, 7, 3
	// deterministic 2, 7, 2

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testManufactorSystem() throws Exception {
		oneTest(2, 7, 3, true); // -> FAST
		/*oneTest(3, 4, 1, false);
		oneTest(4, 4, 1, false);
		oneTest(2, 9, 6, true);
		oneTest(2, 5, 9, false);*/
		// oneTest (3, 9, 6); // -> timeout
		// oneTest (3, 7/8/9/10, 3); // -> UNSAT
	}

	private void oneTest(int a, int n, int b, boolean result) throws Exception {
		PetriGame pg = ManufactorySystem.generate(a, true, true, true);
		testGame(pg, n, b, result);
	}
}
