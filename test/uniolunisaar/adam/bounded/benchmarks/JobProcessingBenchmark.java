package uniolunisaar.adam.bounded.benchmarks;

import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.generators.games.ManufactorySystem;
import uniolunisaar.adam.tools.Tools;

//@Test
public class JobProcessingBenchmark {

	//@Test
	public void test() throws Exception {
		oneBenchmark(2, 7, 3, "JP-sat-");
		oneBenchmark(3, 8, 4, "JP-sat-");
		oneBenchmark(3, 9, 5, "JP-sat-");
		
		oneBenchmark(2, 8, 3, "JP-sat-");
		oneBenchmark(3, 9, 4, "JP-sat-");
		oneBenchmark(3,10, 5, "JP-sat-");
		
		oneBenchmark(2, 7, 4, "JP-sat-");
		oneBenchmark(3, 8, 5, "JP-sat-");
		oneBenchmark(3, 9, 6, "JP-sat-");
		
		oneBenchmark(2, 6, 3, "JP-unsat-");
		oneBenchmark(3, 7, 4, "JP-unsat-");
		oneBenchmark(3, 8, 5, "JP-unsat-");
		
		oneBenchmark(2, 7, 2, "JP-unsat-");
		oneBenchmark(3, 8, 3, "JP-unsat-");
		oneBenchmark(3, 9, 4, "JP-unsat-");
	}
	
	private void oneBenchmark (int problemSize, int n, int b, String id) throws Exception {
		PetriNet pn = ManufactorySystem.generate(problemSize, true, true, true);
		pn.setName("Benchmarks/JobProcessing/" + "" + id + String.format("%02d", problemSize) + "-" + String.format("%02d", n) + "-" + b);
		
		Tools.saveFile("Benchmarks/JobProcessing/" + "" + id + String.format("%02d", problemSize) + "-" + String.format("%02d", n) + "-" + b + ".apt", Tools.getPN(pn));
	}
}
