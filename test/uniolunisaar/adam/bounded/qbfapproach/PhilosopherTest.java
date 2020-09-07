package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.Philosopher;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class PhilosopherTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testPhilosopher() throws Exception {
		int max = 12;
		if (QbfControl.fastTests) {max = 8;}
		for (int i = 2; i <= max; ++i) {
			PetriGameWithTransits pg = Philosopher.generateGuided(i, true, true);
			if (QbfControl.trueConcurrent) {
				testGame(pg, 2, 0, false);
				testGame(pg, 3, 0, true);
			} else {
				testGame(pg, i + 1, 0, false);
				testGame(pg, i + 2, 0, true);
			}
		}
	}

}