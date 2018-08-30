package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.solver.QbfASafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.Clerks;

import org.testng.Assert;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.logic.util.AdamTools;

/*
 * NO MEMORY REQUIRED TO SOLVE
 */
@Test
public class DWTest { // Document Workflow / DW

    @Test(timeOut = 1800 * 1000) // 30 min
    public void testClerks() throws Exception {

        int j = 8; // j = 7 -> UNSAT; j = 8 -> SAT
        for (int i = 1; i <= 5; ++i) {
            oneTestTrue(i, j, 0);
            oneTestFalse(i, j - 1, 0);
            j += 2;
        }
    }

    private void oneTestTrue(int problemSize, int n, int b) throws Exception {
        PetriGame pn = Clerks.generateNonCP(problemSize, true, true);
        QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QBFSolverOptions(n, b));
        Assert.assertTrue(sol.existsWinningStrategy());
    }

    private void oneTestFalse(int problemSize, int n, int b) throws Exception {
        PetriGame pn = Clerks.generateNonCP(problemSize, true, true);
        AdamTools.savePG2PDF("before", pn, true);
        QbfASafetySolver sol = new QbfASafetySolver(pn, new Safety(), new QBFSolverOptions(n, b));
        boolean bool = sol.existsWinningStrategy();
        if (bool) {
            AdamTools.savePG2PDF("strategy", sol.getStrategy(), true);
        }
        Assert.assertFalse(sol.existsWinningStrategy());
    }
}
