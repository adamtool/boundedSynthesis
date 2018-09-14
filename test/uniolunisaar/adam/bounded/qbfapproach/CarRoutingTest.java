package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.CarRouting;

@Test
public class CarRoutingTest extends EmptyTest {
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCR() throws Exception {
		oneTest(2, 1, 15, 3, true);			// TODO does not work...
	}
	
	private void oneTest(int nb_routes, int nb_cars, int n, int b, boolean result) throws Exception {
		PetriGame pg = CarRouting.createAReachabilityVersion(nb_routes, nb_cars, false);
		testGame(pg, n, b, result);
	}
}
