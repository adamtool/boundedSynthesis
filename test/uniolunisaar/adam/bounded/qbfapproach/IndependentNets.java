package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.pg.OnceOfAll;
import uniolunisaar.adam.generators.pg.SCC;
import uniolunisaar.adam.generators.pg.SecuritySystem;
import uniolunisaar.adam.util.PNWTTools;

/**
 * 
 * @author Niklas Metzger
 *
 */

@Test
public class IndependentNets extends EmptyTest {

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testIndependentNets() throws Exception {
		for (int i = 2; i <= 4; i++) {
			PetriGame pg = SCC.generatePetriNet(i);
			//PetriGame pg = OnceOfAll.generate(2, true);
			//PNWTTools.saveAPT("examples/safety/nm/IndependentNets_" + i, pg, true);
			testGame(pg,i+2,0, false); // i+3 for tc, seq: 2 -> 7  maybe bound is (n+1)! where ! is the faculty operator
		}
	}

	
}
