package uniolunisaar.adam.bounded.qbfapproach.tokenflows;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.bounded.qbfapproach.EmptyTest;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

@Test
public class ExistsSafetyTest extends EmptyTest {

	@BeforeClass
	public void setProperties() {
		if (System.getProperty("examplesfolder") == null) {
			System.setProperty("examplesfolder", "examples");
		}
	}

	@Test(timeOut = 1800 * 1000) // 30 min
	public void testExistsSafety() throws Exception {
		oneTest("toyexamples/multipleFlowChains1", 3, 0, true);
		oneTest("toyexamples/multipleFlowChains1", 10, 0, true);
		oneTest("toyexamples/multipleFlowChains2", 3, 0, false);
		oneTest("toyexamples/multipleFlowChains2", 10, 0, false);
		oneTest("toyexamples/multipleFlowChains3", 3, 0, true);
		oneTest("toyexamples/multipleFlowChains3", 10, 0, true);
		oneTest("toyexamples/multipleFlowChains4", 3, 0, true);
		oneTest("toyexamples/multipleFlowChains4", 10, 0, true);
		oneTest("toyexamples/multipleFlowChains5", 3, 0, true);
		oneTest("toyexamples/multipleFlowChains5", 10, 0, true);
		oneTest("toyexamples/multipleFlowChains6", 3, 0, true);
		oneTest("toyexamples/multipleFlowChains6", 10, 0, true);
		oneTest("toyexamples/multipleFlowChains7", 3, 0, false);
		oneTest("toyexamples/multipleFlowChains7", 10, 0, false);
		oneTest("toyexamples/unfair1", 6, 0, true);
		oneTest("toyexamples/unfair1", 10, 0, true);
		oneTest("toyexamples/unfair2", 6, 0, true);
		oneTest("toyexamples/unfair2", 10, 0, true);
		oneTest("toyexamples/unfair3", 6, 0, true);
		oneTest("toyexamples/unfair3", 10, 0, true);
		oneTest("toyexamples/unfair4", 4, 0, true);
		oneTest("toyexamples/unfair4", 10, 0, true);
		oneTest("toyexamples/unfair5", 4, 0, true);
		oneTest("toyexamples/unfair5", 10, 0, true);
		oneTest("toyexamples/unfair6", 4, 0, false);
		oneTest("toyexamples/unfair6", 10, 0, false);
		oneTest("toyexamples/unfair7", 4, 0, false);
		oneTest("toyexamples/unfair7", 10, 0, false);
		oneTest("toyexamples/unfair8", 8, 0, true);
		oneTest("toyexamples/unfair8", 10, 0, true);
		oneTest("toyexamples/unfair9", 10, 0, false);
		oneTest("toyexamples/unfair9", 12, 0, true);
		oneTest("toyexamples/unfair10", 10, 0, false);
		oneTest("toyexamples/unfair10", 12, 0, true);
		oneTest("toyexamples/oneTransitionEnv1", 3, 0, false);
		oneTest("toyexamples/oneTransitionEnv1", 10, 0, false);
		oneTest("toyexamples/oneTransitionEnv2", 3, 0, true);
		oneTest("toyexamples/oneTransitionEnv2", 10, 0, true);
		oneTest("toyexamples/oneTransitionEnv3", 3, 0, false);
		oneTest("toyexamples/oneTransitionEnv3", 10, 0, false);
		oneTest("toyexamples/oneTransitionSys1", 3, 0, false);
		oneTest("toyexamples/oneTransitionSys1", 10, 0, false);
		oneTest("toyexamples/oneTransitionSys2", 3, 0, true);
		oneTest("toyexamples/oneTransitionSys2", 10, 0, true);
		oneTest("toyexamples/oneTransitionSys3", 3, 0, true);
		oneTest("toyexamples/oneTransitionSys3", 10, 0, true);
		oneTest("toyexamples/oneTransitionBoth1", 3, 0, true);
		oneTest("toyexamples/oneTransitionBoth1", 10, 0, true);
		oneTest("toyexamples/oneTransitionBoth2", 3, 0, false);
		oneTest("toyexamples/oneTransitionBoth2", 10, 0, false);
		oneTest("toyexamples/oneTransitionBoth3", 3, 0, true);
		oneTest("toyexamples/oneTransitionBoth3", 10, 0, true);
		oneTest("toyexamples/oneTransitionBoth4", 3, 0, true);
		oneTest("toyexamples/oneTransitionBoth4", 10, 0, true);
		oneTest("toyexamples/oneTransitionBoth5", 3, 0, true);
		oneTest("toyexamples/oneTransitionBoth5", 10, 0, true);
		oneTest("toyexamples/decision1", 3, 0, false);
		oneTest("toyexamples/decision1", 10, 0, false);
		oneTest("toyexamples/decision2", 3, 0, true);
		oneTest("toyexamples/decision2", 10, 0, true);
		oneTest("toyexamples/twoDecisions1", 3, 0, false);
		oneTest("toyexamples/twoDecisions1", 5, 0, false);
		oneTest("toyexamples/twoDecisions1", 10, 0, false);
		oneTest("toyexamples/twoDecisions2", 3, 0, false);
		oneTest("toyexamples/twoDecisions2", 5, 0, true);
		oneTest("toyexamples/twoDecisions2", 10, 0, true);
		oneTest("escape/escape11", 3, 0, false);
		oneTest("escape/escape11", 5, 0, false);
		oneTest("escape/escape11", 10, 0, false);
		oneTest("escape/escape21", 3, 0, false);
		oneTest("escape/escape21", 6, 0, true);
		oneTest("escape/escape21", 10, 0, true);
		oneTest("toyexamples/infiniteBadWithEscape", 6, 0, true);
		oneTest("toyexamples/infiniteBadWithEscape", 10, 0, true);
		oneTest("toyexamples/infiniteBadWithEscape2", 7, 0, true);
		oneTest("toyexamples/infiniteBadWithEscape2", 10, 0, true);
		oneTest("infflowchains/infflowchains_env_0", 10, 0, false);
		oneTest("infflowchains/infflowchains", 20, 0, false);
		oneTest("infflowchains/infflowchains1", 20, 0, true);
		oneTest("infflowchains/infflowchains2", 20, 0, false);
		oneTest("infflowchains/infflowchains2b", 20, 0, true);
		oneTest("infflowchains/infflowchains3", 20, 0, true);
		oneTest("infflowchains/infflowchains4", 10, 0, true);
		oneTest("infflowchains/infflowchains5", 20, 0, true);
		oneTest("infflowchains/infiniteFiniteFlowChains", 20, 0, false);
		oneTest("infflowchains/infiniteFiniteFlowChains2", 20, 0, true);
		//oneTest("infflowchains/infflowchains6", 20, 0, false); // TODO should be false; NEW should solve this
		oneTest("infflowchains/infflowchainsOneGoodOneBad_1", 20, 0, false);
		oneTest("infflowchains/infflowchainsOneGoodOneBad", 20, 0, true);
		oneTest("newchains/newchainForget_1", 20, 0, false);
		oneTest("newchains/newchainForget", 20, 0, true);
		oneTest("newchains/newchainSplitAndMerge", 20, 0, true);
	}

	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/existssafety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
