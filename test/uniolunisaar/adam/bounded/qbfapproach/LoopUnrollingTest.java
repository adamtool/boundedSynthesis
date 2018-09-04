package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.generators.LoopUnrolling;

@Test
public class LoopUnrollingTest extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testLoopUnrolling() throws Exception {
		oneTest(1, true, 10, 0, false);
		oneTest(1, false, 10, 0, true);
		oneTest(2, true, 25, 0, false);
		oneTest(2, false, 10, 0, true);
	}
	
	private void oneTest(int problemSize, boolean newChain, int n, int b, boolean result) throws Exception {
		PetriNet pn = LoopUnrolling.createESafetyVersion(problemSize, newChain, false);
		QbfSolver<? extends WinningCondition> sol = QbfSolverFactory.getInstance().getSolver(new PetriGame(pn), false, new QbfSolverOptions(n, b));
		nextTest(sol, n, b, result);
	}
}
