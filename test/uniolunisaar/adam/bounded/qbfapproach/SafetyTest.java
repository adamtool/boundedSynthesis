package uniolunisaar.adam.bounded.qbfapproach;


import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.sdnencoding.SequentialUpdate;
import uniolunisaar.adam.sdnencoding.SwitchUpdate;
import uniolunisaar.adam.sdnencoding.TopologyToPN;
import uniolunisaar.adam.sdnencoding.Update;

@Test
public class SafetyTest extends EmptyTest {
	
	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
        if (System.getProperty("PROPERTY_FILE") == null) {
        	System.setProperty("PROPERTY_FILE", "ADAM.properties");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void testForallSafety() throws Exception {
		Set<String> ingress = new HashSet<>();
		ingress.add("sw0");
		List<Update> updateList = new ArrayList<>();
		updateList.add(new SwitchUpdate("sw8", "sw7", "sw5"));
		updateList.add(new SwitchUpdate("sw9", "sw10", "sw8"));
		updateList.add(new SwitchUpdate("sw0", "sw1", "sw2"));
		SequentialUpdate update = new SequentialUpdate(updateList);
		TopologyToPN ttp = new TopologyToPN(new File("examples/AbilenePN.txt"), ingress, update);
		PetriGame pn = ttp.generatePetriNet();
		//initial configuration for forwarding
		pn.getPlace("sw0fwdTosw1").setInitialToken(1);
		pn.getPlace("sw1fwdTosw10").setInitialToken(1);
		pn.getPlace("sw10fwdTosw7").setInitialToken(1);
		pn.getPlace("sw7fwdTosw6").setInitialToken(1);
		pn.getPlace("sw6fwdTosw4").setInitialToken(1);
		pn.getPlace("sw4fwdTosw5").setInitialToken(1);
		
		//initial configuration for updating
		pn.getPlace("sw8fwdTosw7").setInitialToken(1);
		pn.getPlace("sw9fwdTosw10").setInitialToken(1);
		
		pn.getPlace("sw2fwdTosw9").setInitialToken(1);
		
		AdamTools.savePG2PDF("test", pn, false);
		AdamTools.saveAPT("example", pn, false);
		
		//oneTest("tests/watchdog5", 15, 3, true);		// TODO search for bounds
		//oneTest("container/container", 10, 2, true);	// TODO search for bounds
		//oneTest("notConcurrencyPreservingTests/toMakeCP", 20, 2, false);	// TODO nets are unsafe, making them safe defeats their purpose
		//oneTest("notConcurrencyPreservingTests/madeCP", 20, 2, true);		// TODO nets are unsafe, making them safe defeats their purpose
		oneTest("jhh/twoIndependentProcesses", 7, 0, true);
		//oneTest("ndet/nondet_s3", 10, 0, false);
		/*oneTest("ndet/nondet_s3", 10, 2, true);
		oneTest("ndet/nondet_s3_noStrat", 15, 2, false);
		oneTest("ndet/nondet_unnecessarily_noStrat", 15, 3, false);
		oneTest("ndet/nondet_withBad", 12, 2, false);
		oneTest("ndet/nondet_jhh1", 20, 0, false);
		oneTest("ndet/nondet_jhh2", 20, 0, true);
		oneTest("ndet/nondet_jhh3", 20, 0, false);
		oneTest("ndet/nondet_motivationForSchedulingChange", 5, 0, false);
		oneTest("jhh/myexample1", 10, 0, false);
		oneTest("jhh/myexample1", 10, 2, false);
		oneTest("jhh/myexample2", 10, 0, true);
		oneTest("jhh/myexample2", 10, 2, true);
		oneTest("jhh/myexample3", 10, 0, false);
		oneTest("jhh/myexample3", 10, 2, true);
		oneTest("jhh/myexample4", 10, 0, false);
		oneTest("jhh/myexample4", 10, 2, false);
		oneTest("jhh/myexample5", 20, 0, true);
		oneTest("jhh/myexample7", 4, 0, true);
		oneTest("jhh/myexample7", 3, 0, false);
		oneTest("ndet/nondet", 5, 2, false);
		//oneTest("burglar/burglar", 7, 3, true);
		//oneTest("burglar/burglar", 6, 2, false);
		oneTest("jhh/robots_true", 20, 0, true);
		oneTest("jhh/robots_false", 20, 0, false);
		oneTest("constructedExample/constructedExample", 3, 0, false);
		oneTest("constructedExample/constructedExample", 4, 0, true);
		oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 3, 0, false);
		oneTest("constructedExampleWithoutLoop/constructedExampleWithoutLoop", 4, 0, true);
		oneTest("container/container", 20, 0, false);
		oneTest("deadlock/missDeadlock", 3, 0, false);
		oneTest("deadlock/missDeadlock", 4, 0, true);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 2, false);
		oneTest("firstExamplePaper/firstExamplePaper", 4, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper", 5, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper", 10, 3, true);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 0, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 3, false);
		oneTest("firstExamplePaper/firstExamplePaper_extended", 10, 10, false);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 2, 0, false);
		oneTest("ma_vsp/vsp_1_withBadPlaces", 3, 0, true);
		oneTest("testingNets/envSkipsSys", 15, 3, false);
		/*oneTest("nm/sendingprotocol", 6, 2, true);
		oneTest("nm/sendingprotocol", 5, 2, false);
		oneTest("nm/sendingprotocolTwo", 12, 2, true);
		oneTest("nm/sendingprotocolTwo", 11, 2, false);*/
	}
	
	private void oneTest(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/safety/" + str + ".apt";
		testPath(path, n, b, result);
	}
}
