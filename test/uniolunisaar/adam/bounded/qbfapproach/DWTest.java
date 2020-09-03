package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.pg.Clerks;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class DWTest extends EmptyTest { // Document Workflow / DW

    @Test(timeOut = 1800 * 1000) // 30 min
    public void testDW() throws Exception {
        int j = 8; // j = 7 -> UNSAT; j = 8 -> SAT
        int max = 8;
        if (QbfControl.fastTests) {max = 5;}
        for (int i = 1; i <= max; ++i) {
        	if (QbfControl.trueConcurrent) {
        		oneTestTrue(i, j - 1, 0);
        		oneTestFalse(i, j - 2, 0);
        	} else {
        		oneTestTrue(i, j, 0);
        		oneTestFalse(i, j - 1, 0);
        	}
            j += 2;
        }
    }

    private void oneTestTrue(int problemSize, int n, int b) throws Exception {
        PetriGame pg = Clerks.generateNonCP(problemSize, true, true);
        testGame(pg, n, b, true);
    }

    private void oneTestFalse(int problemSize, int n, int b) throws Exception {
        PetriGame pg = Clerks.generateNonCP(problemSize, true, true);
        testGame(pg, n, b, false);
    }
}
