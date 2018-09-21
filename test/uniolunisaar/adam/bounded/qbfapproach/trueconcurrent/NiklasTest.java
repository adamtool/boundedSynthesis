package uniolunisaar.adam.bounded.qbfapproach.trueconcurrent;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class NiklasTest extends EmptyTestEnvDec {
	
	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testNiklas() throws Exception {
		oneTest("jhh/myexample000", 5, 0, false);
		//oneTest("jhh/myexample00", 5, 0, false);
		//oneTest("jhh/myexample0", 5, 0, false);
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
