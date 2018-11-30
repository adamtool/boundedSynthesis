package uniolunisaar.adam.bounded.qbfapproach;

import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.synthesis.EmergencyBreakdown;

@Test
public class EBTest extends EmptyTest {

	// 1 1 10 2 timeout
	// 1 1 11 2 timeout

	// 1 1 14 24 DETERMINISTIC unfolder 75 Sekunden
	// 2 0 14 24 DETERMINISTIC unfolder 50 Sekunden
	// 0 2 14 24 DETERMINISTIC unfolder 75 Sekunden
	// 1 1 14 2 NONDETERMINISTIC unfolder 2600, 4500 Sekunden, 2 stds...
	// 1 1 13 2 FORnondeterministicUNFOLDER
	// 2 0 13 2 FORnondeterministicUNFOLDER
	// 0 2 13 2 FORnondeterministicUNFOLDER

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testEB() throws Exception {
		//oneTest(1, 1, 13, 2);
	}

	private void oneTest(int ps1, int ps2, int n, int b) throws Exception {
		PetriGame pg = EmergencyBreakdown.createSafetyVersion(ps1, ps2, false);
		testGame(pg, n, b, true);
	}
}
