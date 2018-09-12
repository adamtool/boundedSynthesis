package uniolunisaar.adam.bounded.qbfapproach.e_safety;

import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.EmptyTest;
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
		//oneTest(2, true, 10, 0, false); // TODO understand whether error or algorithm/implementation wrong..
		oneTest(2, false, 10, 0, true);
	}
	
	private void oneTest(int problemSize, boolean newChain, int n, int b, boolean result) throws Exception {
		PetriGame pn = LoopUnrolling.createESafetyVersion(problemSize, newChain, false);
		QbfSolver<? extends WinningCondition> sol = QbfSolverFactory.getInstance().getSolver(pn, false, new QbfSolverOptions(n, b));
		nextTest(sol, n, b, result);
	}
}
