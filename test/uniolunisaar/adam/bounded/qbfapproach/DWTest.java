package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.synthesis.Clerks;

/*
 * NO MEMORY REQUIRED TO SOLVE
 */
@Test
public class DWTest extends EmptyTest { // Document Workflow / DW

    @Test(timeOut = 1800 * 1000) // 30 min
    public void testClerks() throws Exception {
    	oneTestTrue(3,15,0);
    	/*
        int j = 8; // j = 7 -> UNSAT; j = 8 -> SAT
        for (int i = 1; i <= 5; ++i) {
            oneTestTrue(i, j-1, 0); //tc: j-1 seq: j
            oneTestFalse(i, j - 2, 0); //tc: j-2 seq: j-1
            j += 2;
        }*/
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
