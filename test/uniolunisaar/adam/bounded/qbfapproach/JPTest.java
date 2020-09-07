package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.ManufactorySystem;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class JPTest extends EmptyTest { // Job Processing

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testManufactorSystem() throws Exception {
		oneTest(2, 7, 3, true);
		oneTest(3, 8, 3, true);
		oneTest(4, 9, 3, true);
		if (!QbfControl.fastTests) {
			oneTest(5, 10, 3, true);
		}
	}

	private void oneTest(int a, int n, int b, boolean result) throws Exception {
		PetriGameWithTransits pg = ManufactorySystem.generate(a, true, true);
		testGame(pg, n, b, result);
	}
}
