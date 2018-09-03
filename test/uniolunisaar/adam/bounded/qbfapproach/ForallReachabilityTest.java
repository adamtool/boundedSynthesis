package uniolunisaar.adam.bounded.qbfapproach;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

@Test
public class ForallReachabilityTest extends EmptyTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        		System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testForallReachability() throws Exception {
		oneTest("toyexamples/twoDecisions1", 6, 0, false);
		oneTest("toyexamples/twoDecisions1", 10, 0, false);
		oneTest("toyexamples/chains", 6, 0, true);
		oneTest("toyexamples/chains", 10, 0, true);
		oneTest("toyexamples/chains0", 6, 0, false);
		oneTest("toyexamples/chains0", 10, 0, false);
		oneTest("toyexamples/chains1", 6, 0, false);
		oneTest("toyexamples/chains1", 10, 0, false);
		oneTest("toyexamples/infiniteFlowChains", 13, 0, true);
		oneTest("toyexamples/infiniteFlowChains", 20, 0, true);
		oneTest("toyexamples/infiniteFlowChains2", 13, 0, false);
		oneTest("toyexamples/infiniteFlowChains2", 20, 0, false);
		oneTest("toyexamples/newLateChain", 6, 0, false);
		oneTest("toyexamples/newLateChain", 10, 0, false);
		oneTest("toyexamples/newLateToken1", 6, 0, false);
		oneTest("toyexamples/newLateToken1", 10, 0, false);
		oneTest("toyexamples/newLateToken2", 5, 0, true);
		oneTest("toyexamples/newLateToken2", 10, 0, true);
		oneTest("toyexamples/oneTokenMultiChains0", 6, 0, false);
		oneTest("toyexamples/oneTokenMultiChains0", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains1", 6, 0, false);
		oneTest("toyexamples/oneTokenMultiChains1", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains2", 6, 0, false);
		oneTest("toyexamples/oneTokenMultiChains2", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains3", 6, 0, true);
		oneTest("toyexamples/oneTokenMultiChains3", 10, 0, true);
		oneTest("toyexamples/oneTokenMultiChains4", 6, 0, false);
		oneTest("toyexamples/oneTokenMultiChains4", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains5", 6, 0, false);
		oneTest("toyexamples/oneTokenMultiChains5", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains6", 6, 0, false);
		oneTest("toyexamples/oneTokenMultiChains6", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains7", 6, 0, true);
		oneTest("toyexamples/oneTokenMultiChains7", 10, 0, true);
		oneTest("toyexamples/oneTokenMultiChains8", 6, 0, false);			// ONLY because I don't allow deadlocks after reach
		oneTest("toyexamples/oneTokenMultiChains8", 10, 0, false);		// ONLY because I don't allow deadlocks after reach
		oneTest("toyexamples/overallBad0", 6, 0, false);
		oneTest("toyexamples/overallBad0", 10, 0, false);
		oneTest("toyexamples/type2", 6, 0, true);
		oneTest("toyexamples/type2", 10, 0, true);
		oneTest("toyexamples/winInit", 4, 0, true);
		oneTest("toyexamples/winInit", 10, 0, true);
		oneTest("jhh/myexampleWithSysNoStrat", 3, 0, false);
		oneTest("jhh/myexampleWithSysNoStrat", 10, 0, false);
		oneTest("jhh/myexampleWithSys", 4, 0, true);
		oneTest("jhh/myexampleWithSys", 10, 0, true);
		oneTest("jhh/myexample2WithEnvNoStrat", 3, 0, false);
		oneTest("jhh/myexample2WithEnvNoStrat", 10, 0, false);
		oneTest("jhh/myexample2WithEnv", 3, 0, true);
		oneTest("jhh/myexample2WithEnv", 10, 0, true);
		oneTest("jhh/myexample2WithEnvNoStrat", 3, 0, false);
		oneTest("jhh/myexample2WithEnvNoStrat", 10, 0, false);
		oneTest("jhh/myexampleStrat", 3, 0, true);
		oneTest("jhh/myexampleStrat", 10, 0, true);
		oneTest("jhh/myexampleNoStrat", 3, 0, false);
		oneTest("jhh/myexampleNoStrat", 10, 0, false);
		oneTest("jhh/unfair", 10, 0, false);
		oneTest("jhh/unfair", 20, 0, false);
		oneTest("burglar/burglar", 10, 0, false);
		oneTest("burglar/burglar", 10, 2, true);
		oneTest("burglar/burglar1", 10, 0, false);
		oneTest("burglar/burglar1", 10, 2, true);
		oneTest("burglar/burglar2", 10, 0, false);
		oneTest("burglar/burglar2", 10, 2, false);
		oneTest("burglar/burglarDirectlyWon", 10, 0, true);
		oneTest("toyexamples/lateUnsafeChain", 10, 0, false);
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
//		PetriNet pn = Tools.getPetriNet(path);
//		QBFFlowChainSolver<?> sol = new QBFForallReachabilitySolver(new QBFSolvingObject(pn), new Reachability(), new QBFSolverOptions(n, b));
		QbfSolver<? extends WinningCondition> sol = QbfSolverFactory.getInstance().getSolver(path, new QbfSolverOptions(n, b)); //todo MG: warum nicht so?
        nextTest(sol, n, b, result);
	}
}
