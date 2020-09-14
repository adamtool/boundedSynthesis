package uniolunisaar.adam.tests.synthesis.bounded.qbfapproach.tokenflows;

import java.io.File;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.tests.synthesis.bounded.qbfapproach.EmptyTest;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class ExistsBuchiTest extends EmptyTest {

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    @Test(timeOut = 1800 * 1000) // 30 min
    public void testExistsBuechi() throws Exception {
        test ("independentloops", true, 10, 0);
		test ("independentloops", true, 10, 2);
		test ("independentloops2", true, 10, 0);
		test ("independentloops2", true, 10, 2);
		test ("finiteA", false, 10, 0);
		test ("infiniteA", true, 10, 0);
		test ("infiniteB", false, 10, 0);
		test ("type2A", true, 10, 0);
		test ("type2B", true, 10, 0);
        test ("decInLoop", false, 10, 0);
        test ("decInLoop", true, 10, 2);
        test ("decInLoop", false, 5, 2);
        test("goodBadLoop0", true, 10, 0);
        test("goodBadLoop1", true, 10, 0);
        test("goodBadLoop2", true, 10, 0);
        test("oneGoodInfEnv", false, 10, 0);
        test("oneGoodInfEnv", false, 10, 2);
        test("nondet", false, 10, 0);
        test("nondet", false, 10, 2);
        test("firstExamplePaperBuchi", false, 10, 2);
        test("firstExamplePaperBuchi", false, 10, 0);
        // test("firstExamplePaperBuchi", true, 10, 3); TODO ForNonDetUnfolder does not work here!!!
    }

    private void test(String name, boolean result, int n, int b) throws Exception {
        final String path = System.getProperty("examplesfolder") + File.separator + "existsbuechi" + File.separator + "toyExamples" + File.separator + name + ".apt";
		testPath(path, n, b, result);
    }
}
