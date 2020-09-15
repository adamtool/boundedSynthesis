package uniolunisaar.adam.tests.synthesis.bounded.benchmarks;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.generators.pgwt.Workflow;
import uniolunisaar.adam.tools.Tools;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

//@Test
public class ConcurrentMachinesBenchmark {						//    190 sec
	
	//@Test
	public void CMBenchmark_6_3() throws Exception {
		for (int i = 2; i <= 21; ++i) {
			oneBenchmark(i, 1, 6, 3, "CM-sat-");
		}
	}
	
	//@Test
	public void CMBenchmark_7_3() throws Exception {
		for (int i = 2; i <= 21; ++i) {
			oneBenchmark(i, 1, 7, 3, "CM-sat-");
		}
	}
	
	//@Test
	public void CMBenchmark_6_4() throws Exception {
		for (int i = 2; i <= 21; ++i) {
			oneBenchmark(i, 1, 6, 4, "CM-sat-");
		}
	}
	
	//@Test
	public void CMBenchmark_6_2() throws Exception {
		for (int i = 2; i <= 21; ++i) {
			oneBenchmark(i, 1, 6, 2, "CM-unsat-");
		}
	}
	
	//@Test
	public void CMBenchmark_5_3() throws Exception {
		for (int i = 2; i <= 21; ++i) {
			oneBenchmark(i, 1, 5, 3, "CM-unsat-");
		}
	}
	
	private void oneBenchmark (int ps1, int ps2, int n, int b, String id) throws Exception {
		PetriNet pn = Workflow.generate(ps1, ps2, true, true);
		pn.setName("Benchmarks/ConcurrentMachines/" + "" + id + String.format("%02d", ps1) + "-" + String.format("%02d", ps2) + "-" + String.format("%02d", n) + "-" + b);
		
		Tools.saveFile("Benchmarks/ConcurrentMachines/" + "" + id + String.format("%02d", ps1) + "-" + String.format("%02d", ps2) + "-" + String.format("%02d", n) + "-" + b + ".apt", Tools.getPN(pn));
	}
}
