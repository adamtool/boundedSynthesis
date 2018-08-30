package uniolunisaar.adam.bounded.qbfapproach;

import static org.testng.Assert.assertTrue;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.logic.util.AdamTools;

@Test
public class BuechiTest {

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    @Test(timeOut = 1800 * 1000) // 30 min
    public void testExistsBüchi() throws Exception {
        test ("independentloops", true, 10, 0);
		test ("independentloops", true, 10, 2);
		test ("independentloops2", true, 10, 0);
		test ("independentloops2", true, 10, 2);
		test ("finiteA", false, 10, 0);
		test ("infiniteA", true, 10, 0);
		test ("infiniteB", false, 10, 0);
		test ("type2A", true, 10, 0);
		test ("type2B", true, 10, 0);
        test ("decInLoop", false, 10, 0);
        test ("decInLoop", true, 10, 2);
        test ("decInLoop", false, 5, 2);
        test("goodBadLoop0", true, 10, 0);
        test("goodBadLoop1", true, 10, 0);
        test("goodBadLoop2", true, 10, 0);
        test("oneGoodInfEnv", false, 10, 0);
        test("oneGoodInfEnv", false, 10, 2);
        test("nondet", false, 10, 0);
        test("nondet", false, 10, 2);
        test("firstExamplePaperBuchi", false, 10, 2);
        test("firstExamplePaperBuchi", false, 10, 0);
        // test("firstExamplePaperBuchi", true, 10, 3); TODO ForNonDetUnfolder does not work here!!!
    }

    private void test(String name, boolean result, int n, int b) throws Exception {
        final String path = System.getProperty("examplesfolder") + File.separator + "buechi" + File.separator + "toyExamples" + File.separator + name + ".apt";
//        PetriNet pn = Tools.getPetriNet(path);
//        QBFBuchiSolver sol = new QBFBuchiSolver(pn, new Buchi(), new QBFSolverOptions(n, b));
		QBFSolver<? extends WinningCondition> sol = QBFSolverFactory.getInstance().getSolver(path, new QBFSolverOptions(n, b)); //todo MG: warum nicht so?
        sol.existsWinningStrategy();
        AdamTools.savePG2PDF("originalGame", sol.originalGame, false);
        AdamTools.savePG2PDF("unfolding", sol.unfolding, false);
        if (sol.existsWinningStrategy()) {
            AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
        }
        Assert.assertEquals(sol.existsWinningStrategy(), result);
        
        if (sol.existsWinningStrategy()) {
			assertTrue(QBFSolver.checkStrategy(sol.originalGame, sol.strategy));
		}
    }
}
