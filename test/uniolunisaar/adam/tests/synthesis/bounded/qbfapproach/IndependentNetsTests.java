package uniolunisaar.adam.tests.synthesis.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.tests.synthesis.bounded.qbfapproach.EmptyTest;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.generators.pgwt.OnceOfAll;
import uniolunisaar.adam.generators.pgwt.SCC;
import uniolunisaar.adam.generators.pgwt.SecuritySystem;
import uniolunisaar.adam.util.PNWTTools;

/**
 * 
 * @author Niklas Metzger
 *
 */

@Test
public class IndependentNetsTests extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testIndependentNets() throws Exception {
		for (int i = 2; i <= 4; i++) {
			PetriGameWithTransits pg = SCC.generatePetriNet(i);
			//PetriGame pg = OnceOfAll.generate(2, true);
			//PNWTTools.saveAPT("examples/safety/nm/IndependentNets_" + i, pg, true);
			testGame(pg,i+2,0, false); // i+3 for tc, seq: 2 -> 7  maybe bound is (n+1)! where ! is the faculty operator
		}
	}

	
}
