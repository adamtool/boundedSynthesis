package uniolunisaar.adam.bounded.qbfapproach.trueconcurrent;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestSimpleFlow extends EmptyTestEnvDec {
	
	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testNiklas() throws Exception {
		oneTest("nm/minimal", 3, 0, false);
		oneTest("nm/trueconcurrent",3,0,true);
		oneTest("nm/nounfolding",5,2,true);
		//oneTest("nm/oneunfolding",5,2,true);
		}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
