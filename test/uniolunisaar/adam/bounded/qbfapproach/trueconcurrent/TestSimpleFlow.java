package uniolunisaar.adam.bounded.qbfapproach.trueconcurrent;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.pg.ManufactorySystem;
import uniolunisaar.adam.generators.pg.SCC;
import uniolunisaar.adam.util.PNWTTools;

/**
 * 
 * @author Niklas Metzger
 *
 */

@Test
public class TestSimpleFlow extends EmptyTestEnvDec {
	
	
	//@Test(timeOut = 1800 * 1000) // 30 min
	public void testNiklas() throws Exception {
		/*oneTest("nm/multipleEnvDecision",4,2,false);
		oneTest("nm/minimal", 5, 2	, false);
		oneTest("nm/trueconcurrent",3,0,true);
		oneTest("nm/nounfolding",5,2,true);
		oneTest("nm/oneunfolding",5,2,true);
		oneTest("nm/BurglarApt",7,2,true);*/
		//oneTest2("nm/oneenv",10,0,true);
	}
	
	@Test(timeOut = 1800 * 1000)
	private void oneTest() throws Exception {
		//testPath("examples/safety/nm/testStepNet.apt", 4, 1, true);
		//testPath("examples/safety/nm/trueconcurrent.apt",10,1,true);
		//testPath("examples/safety/nm/sccbenchmark.apt",20,0,true);
		testPath("examples/safety/boundedunfolding/causalmemory.apt", 10, 2, false);
		//PetriGame pg = ManufactorySystem.generate(a, true, true, true);
		//testGame(pg, n, b, result);
	}
}
