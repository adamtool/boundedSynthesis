package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.Clerks;

/**
 * 
 * @author Jesko Hecking-Harbusch
 * 
 * NO MEMORY REQUIRED TO SOLVE
 */

@Test
public class DWsTest extends EmptyTest { // Document Workflow / DW

	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testDWs() throws Exception {
		int max = 8;
		if (QbfControl.fastTests) {max = 5;}
		int j = 5; 	// j = 4 -> UNSAT; j = 5 -> SAT
		for (int i = 1; i <= max; ++i) {
			oneTestTrue(i, j, 0);
			oneTestFalse(i, j - 1, 0);
			j += 2;
		}
	}
	
	private void oneTestTrue(int problemSize, int n, int b) throws Exception {
		PetriGameWithTransits pg = Clerks.generateCP(problemSize, true, true);
		testGame(pg, n, b, true);
	}
	
	private void oneTestFalse(int problemSize, int n, int b) throws Exception {
		PetriGameWithTransits pg = Clerks.generateCP(problemSize, true, true);
		testGame(pg, n, b, false);
	}
}
