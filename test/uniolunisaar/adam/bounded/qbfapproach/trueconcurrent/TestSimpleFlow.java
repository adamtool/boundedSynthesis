package uniolunisaar.adam.bounded.qbfapproach.trueconcurrent;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.synthesis.ManufactorySystem;

@Test
public class TestSimpleFlow extends EmptyTestEnvDec {
	
	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	/*
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testNiklas() throws Exception {
		oneTest("nm/multipleEnvDecision",4,2,false);
		oneTest("nm/minimal", 5, 2	, false);
		oneTest("nm/trueconcurrent",3,0,true);
		oneTest("nm/nounfolding",5,2,true);
		oneTest("nm/oneunfolding",5,2,true);
		oneTest("nm/BurglarApt",7,2,true);
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
		testPath(path, n, b, result);
	}*/
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testManufactorSystem() throws Exception {
		oneTest(2, 6, 3, true); // -> FAST
		//oneTest(3, 4, 1, false);
		//oneTest(4, 4, 1, false);
		//oneTest(2, 9, 6, true);
		//oneTest(2, 5, 9, false);
		// oneTest (3, 9, 6); // -> timeout
		// oneTest (3, 7/8/9/10, 3); // -> UNSAT
	}

	private void oneTest(int a, int n, int b, boolean result) throws Exception {
		PetriGame pg = ManufactorySystem.generate(a, true, true, true);
		testGame(pg, n, b, result);
	}
}
