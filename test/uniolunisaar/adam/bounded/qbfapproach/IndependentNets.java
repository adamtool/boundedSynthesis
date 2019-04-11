package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
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
		for (int i = 3; i <= 3; i++) {
			PetriGame pg = SCC.generatePetriNet(i);
			testGame(pg,25,0, true); // i+3 for tc, seq: 2 -> 7
		}
	}

	
}
