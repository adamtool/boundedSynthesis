package uniolunisaar.adam.bounded.benchmarks;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.generators.pg.SecuritySystem;
import uniolunisaar.adam.tools.Tools;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 * BURGLAR
 *
 */

public class ASbenchmark {

	//@Test
	public void ASbenchmark_1() throws Exception {
		for (int i = 1; i <= 8; ++i) {
			oneBenchmark(2, i, 2, "AS-");
		}
	}
	
	private void oneBenchmark (int problemSize, int n, int b, String id) throws Exception {
		PetriNet pn = SecuritySystem.createSafetyVersion(problemSize, false);
		pn.setName("Benchmarks/Niklas/AS/" + "" + id + String.format("%02d", problemSize) + "-" + String.format("%02d", n) + "-" + b);
		
		Tools.saveFile("Benchmarks/Niklas/AS/" + "" + id + String.format("%02d", problemSize) + "-" + String.format("%02d", n) + "-" + b + ".apt", Tools.getPN(pn));
	}
}
