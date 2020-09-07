package uniolunisaar.adam.bounded.qbfapproach.tokenflows;

import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.EmptyTest;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.CarRouting;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class CarRoutingTest extends EmptyTest {
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCR() throws Exception {
		oneTest(2, 1, 15, 3, true);			// TODO does not work... Why should this work?
	}
	
	private void oneTest(int nb_routes, int nb_cars, int n, int b, boolean result) throws Exception {
		PetriGameWithTransits pg = CarRouting.createAReachabilityVersion(nb_routes, nb_cars, false);
		testGame(pg, n, b, result);
	}
}
