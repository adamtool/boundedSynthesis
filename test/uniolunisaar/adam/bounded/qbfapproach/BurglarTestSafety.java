package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.pg.SecuritySystem;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class BurglarTestSafety extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testSecSys() throws Exception {
		oneTest(2, 6, 2, true);
		//oneTest(2, 5, 2, false);
		//oneTest(2, 5, 2, false);
		//oneTest(2, 7, 1, false);
	}
	
	private void oneTest(int problemSize, int n, int b, boolean result) throws Exception {
		PetriGame pg = SecuritySystem.createSafetyVersion(problemSize, false);
		testGame(pg, n, b, result);
		
	}
	
}
