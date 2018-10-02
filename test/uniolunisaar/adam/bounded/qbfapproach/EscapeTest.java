package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.games.Escape;

@Test
public class EscapeTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCR() throws Exception {
		oneTest(1, 0, 10, 0, true);
		oneTest(1, 1, 10, 0, false);
		oneTest(2, 1, 10, 0, true);
		oneTest(2, 2, 10, 0, false);
		oneTest(2, 3, 10, 0, false);
		oneTest(2, 4, 10, 0, false);
		oneTest(3, 2, 10, 0, true);
		oneTest(4, 2, 10, 0, true);
	}
	
	private void oneTest(int nb_sys, int nb_env, int n, int b, boolean result) throws Exception {
		PetriGame pg = Escape.createESafetyVersion(nb_sys, nb_env, false);
		testGame(pg, n, b, result);
	}
}
