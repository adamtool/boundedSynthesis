package uniolunisaar.adam.bounded.qbfapproach;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

@Test
public class ForallBuchiTest extends EmptyTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testForallBüchi() throws Exception {
		oneTest("jhh/myexample1", 4, 0, false);
		oneTest("jhh/myexample1", 10, 0, false);
		oneTest("jhh/myexample11", 4, 0, false);
		oneTest("jhh/myexample11", 10, 0, false);
		oneTest("jhh/myexample12", 4, 0, false);
		oneTest("jhh/myexample12", 10, 0, false);
		oneTest("jhh/myexample13", 4, 0, true);
		oneTest("jhh/myexample13", 10, 0, true);
		oneTest("jhh/myexample2", 4, 0, false);
		oneTest("jhh/myexample2", 10, 0, false);
		oneTest("jhh/myexample21", 4, 0, false);
		oneTest("jhh/myexample21", 10, 0, false);
		oneTest("jhh/myexample22", 4, 0, true);
		oneTest("jhh/myexample22", 10, 0, true);
		oneTest("toyexamples/infiniteChains", 7, 0, true);
		oneTest("toyexamples/infiniteChains", 10, 0, true);
		oneTest("toyexamples/infiniteChains1", 7, 0, false);
		oneTest("toyexamples/infiniteChains1", 10, 0, false);
		oneTest("toyexamples/oneTokenMultiChains0", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains0", 15, 0, false);
		oneTest("toyexamples/oneTokenMultiChains1", 22, 0, true);	
		oneTest("toyexamples/oneTokenMultiChains1", 25, 0, true);
		oneTest("toyexamples/oneTokenMultiChains2", 11, 0, true);
		oneTest("toyexamples/oneTokenMultiChains2", 15, 0, true);
		oneTest("toyexamples/oneTokenMultiChains3", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains3", 15, 0, false);
		oneTest("toyexamples/oneTokenMultiChains4", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains4", 15, 0, false);
		oneTest("toyexamples/oneTokenMultiChains5", 13, 0, true);		// infinitely many becoming true
		oneTest("toyexamples/oneTokenMultiChains5", 15, 0, true);		// infinitely many becoming true
		oneTest("toyexamples/oneTokenMultiChains6", 11, 0, false);
		oneTest("toyexamples/oneTokenMultiChains6", 15, 0, false);
		oneTest("toyexamples/type2_0", 25, 0, false);
		oneTest("toyexamples/type2_1", 21, 0, true);
		oneTest("toyexamples/type2_1", 25, 0, true);
		//oneTest("toyexamples/type2_2", 25, 0, true);	// TODO 64 reachable markings --- too much
		oneTest("toyexamples/type2_3", 23, 0, true);
		oneTest("toyexamples/type2_3", 25, 0, true);
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallbuechi/" + str + ".apt";
		QbfSolver<? extends WinningCondition> sol = QbfSolverFactory.getInstance().getSolver(path, new QbfSolverOptions(n, b));
        nextTest(sol, n, b, result);
	}
}
