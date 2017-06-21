package uniolunisaar.adam.bounded.benchmarks;

import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.Clerks;

import uniol.apt.adt.pn.PetriNet;

public class DocumentWorkflowBenchmark {						//    190 sec
	
	public void DWBenchmark_6() throws Exception {
		int j = 6;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-unsat-");
			j += 2;
		}
	}
	
	public void DWBenchmark_tooShort() throws Exception {
		int j = 7;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-unsat-");
			j += 2;
		}
	}
	
	public void DWBenchmark_short() throws Exception {
		int j = 8;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-sat-");
			j += 2;
		}
	}
	
	public void DWBenchmark_9() throws Exception {
		int j = 9;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-sat-");
			j += 2;
		}
	}
	
	public void DWBenchmark_10() throws Exception {
		int j = 10;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-sat-");
			j += 2;
		}
	}
	
	/*public void DWBenchmark_11() throws Exception {
		int j = 11;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-sat-");
			j += 2;
		}
	}
	
	public void DWBenchmark_12() throws Exception {
		int j = 12;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-sat-");
			j += 2;
		}
	}
	
	public void DWBenchmark_long() throws Exception {
		int j = 13;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, false, "DW-sat-");
			j += 2;
		}
	}*/
	
	public void DWsBenchmark_3() throws Exception {
		int j = 3;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-unsat-");
			j += 2;
		}
	}
	
	public void DWsBenchmark_tooShort() throws Exception {
		int j = 4;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-unsat-");
			j += 2;
		}
	}
	
	public void DWsBenchmark_short() throws Exception {
		int j = 5;
		for (int i = 1; i <= 25; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-sat-");
			j += 2;
		}
	}
	
	public void DWsBenchmark_6() throws Exception {
		int j = 6;
		for (int i = 1; i <= 15; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-sat-");
			j += 2;
		}
	}
	
	public void DWsBenchmark_7() throws Exception {
		int j = 7;
		for (int i = 1; i <= 15; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-sat-");
			j += 2;
		}
	}
	
	/*public void DWsBenchmark_8() throws Exception {
		int j = 8;
		for (int i = 1; i <= 15; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-sat-");
			j += 2;
		}
	}
	
	public void DWsBenchmark_9() throws Exception {
		int j = 9;
		for (int i = 1; i <= 15; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-sat-");
			j += 2;
		}
	}
	
	public void DWsBenchmark_long() throws Exception {
		int j = 10;
		for (int i = 1; i <= 15; ++i) {
			oneBenchmark(i, j, 1, true, "DWs-sat-");
			j += 2;
		}
	}*/
	
	private void oneBenchmark (int problemSize, int n, int b, boolean concurrencyPreserving, String id) throws Exception {
		PetriNet pn;
		if (concurrencyPreserving) {
			pn = Clerks.generateCP(problemSize, true, true);
		} else {
			pn = Clerks.generateNonCP(problemSize, true, true);
		}
		pn.setName("Benchmarks/DocumentWorkflow/" + "" + id + String.format("%02d", problemSize) + "-" + String.format("%02d", n) + "-" + b);
		
		QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		
		sol.writeQCIR();
	}
}
