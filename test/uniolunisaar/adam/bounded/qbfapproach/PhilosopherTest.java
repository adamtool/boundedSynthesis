package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.generators.Philosopher;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;

@Test
public class PhilosopherTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testPhilosopher() throws Exception {

		// no unfold: 2->4, 3->5, 4->6, 5->7 in 12,5s with 5->8 in Timeout

		PetriGame pn = Philosopher.generateGuided(2, true, true);
		QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(4, 0));
		Assert.assertTrue(sol.existsWinningStrategy());

		pn = Philosopher.generateGuided(3, true, true);
		sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(5, 0));
		Assert.assertTrue(sol.existsWinningStrategy());

		pn = Philosopher.generateGuided(4, true, true);
		sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(6, 0));
		Assert.assertTrue(sol.existsWinningStrategy());

		pn = Philosopher.generateGuided(5, true, true);
		sol = new QbfASafetySolver(pn,new Safety(),  new QbfSolverOptions(7, 0));
		Assert.assertTrue(sol.existsWinningStrategy());

		pn = Philosopher.generateGuided(6, true, true);
		sol = new QbfASafetySolver(pn, new Safety(), new QbfSolverOptions(8, 0));
		Assert.assertTrue(sol.existsWinningStrategy());
	}

}