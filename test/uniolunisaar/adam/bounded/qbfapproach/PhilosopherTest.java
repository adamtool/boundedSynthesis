package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.synthesis.Philosopher;

/*
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class PhilosopherTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testPhilosopher() throws Exception {

		for (int i = 2; i <= 8; ++i) {
			PetriGame pg = Philosopher.generateGuided(i, true, true);
			testGame(pg, i + 2, 0, true);
		}
	}

}