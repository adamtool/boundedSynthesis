package uniolunisaar.adam.bounded.qbfapproach.a_reach;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfAReachabilitySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.generators.CarRouting;
import uniolunisaar.adam.logic.util.AdamTools;

@Test
public class CarRoutingTest {
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testCR() throws Exception {
		oneTest(2, 1, 15, 3, true);			// TODO does not work...
	}
	
	private void oneTest(int nb_routes, int nb_cars, int n, int b, boolean result) throws Exception {
		PetriNet pn = CarRouting.createAReachabilityVersion(nb_routes, nb_cars, false);
		QbfAReachabilitySolver sol = new QbfAReachabilitySolver(new PetriGame(pn), new Reachability(), new QbfSolverOptions(n, b));
		sol.existsWinningStrategy(); // calculate first, then output games, and then check for correctness
		AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
		AdamTools.savePG2PDF("unfolding", sol.originalGame, false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
