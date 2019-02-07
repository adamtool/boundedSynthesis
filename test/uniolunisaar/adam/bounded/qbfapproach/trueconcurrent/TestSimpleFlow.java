package uniolunisaar.adam.bounded.qbfapproach.trueconcurrent;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.pg.ManufactorySystem;

/**
 * 
 * @author Niklas Metzger
 *
 */

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
	
	@Test(timeOut = 1800 * 1000)
	private void oneTest2(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
		testPath(path, n, b, result);
	}*/
	
	
	
	@Test(timeOut = 1800 * 1000)
	private void oneTest() throws Exception {
		testPath("examples/safety/nm/testStepNet.apt", 4, 1, true);
		testPath("examples/safety/nm/trueconcurrent.apt",10,1,true);
		//PetriGame pg = ManufactorySystem.generate(a, true, true, true);
		//testGame(pg, n, b, result);
	}
}
