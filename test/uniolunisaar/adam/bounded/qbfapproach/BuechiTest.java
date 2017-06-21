package uniolunisaar.adam.bounded.qbfapproach;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFBuchiSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.tools.Tools;

@Test
public class BuechiTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testBüchi() throws Exception {
		test ("finiteA", false, 20, 5);
		test ("infiniteA", true, 10, 0);
		test ("infiniteB", false, 10, 0);
		test ("type2A", true, 10, 0);
		test ("type2B", true, 10, 0);
		test ("decInLoop", false, 10, 0);
		test ("decInLoop", true, 10, 2);
		test ("decInLoop", false, 5, 2);
	}

	private void test(String name, boolean result, int n, int b) throws Exception {
		final String path = System.getProperty("examplesfolder") + File.separator + "buechi" + File.separator + "toyExamples" + File.separator + name + ".apt";
		PetriNet pn = Tools.getPetriNet(path);
		System.out.println("NAME: " + pn.getName());
		QBFBuchiSolver sol = new QBFBuchiSolver(pn, new QBFSolverOptions(n, b));
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
