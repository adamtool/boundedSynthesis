package uniolunisaar.adam.bounded.benchmarks;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.generators.SelfOrganizingRobots;

public class SelfReconfiguringRobotsBenchmark {

	public void test() throws Exception {
		oneBenchmark(2, 1, 6, 2, "SR-sat-");
		oneBenchmark(3, 1, 7, 2, "SR-sat-");
		oneBenchmark(4, 1, 8, 2, "SR-sat-");
		
		oneBenchmark(2, 1, 7, 2, "SR-sat-");
		oneBenchmark(3, 1, 8, 2, "SR-sat-");
		oneBenchmark(4, 1, 9, 2, "SR-sat-");
		
		oneBenchmark(2, 1, 6, 3, "SR-sat-");
		oneBenchmark(3, 1, 7, 3, "SR-sat-");
		oneBenchmark(4, 1, 8, 3, "SR-sat-");
		
		oneBenchmark(2, 1, 5, 2, "SR-unsat-");
		oneBenchmark(3, 1, 6, 2, "SR-unsat-");
		oneBenchmark(4, 1, 7, 2, "SR-unsat-");
		
		oneBenchmark(2, 1, 6, 1, "SR-unsat-");
		oneBenchmark(3, 1, 7, 1, "SR-unsat-");
		oneBenchmark(4, 1, 8, 1, "SR-unsat-");
	}
	
	private void oneBenchmark (int robot1, int robot2, int n, int b, String id) throws Exception {
		PetriNet pn = SelfOrganizingRobots.generate(robot1, robot2, true, true);
		pn.setName("Benchmarks/SelfReconfiguringRobots/" + "" + id + String.format("%02d", robot1) + "-" + String.format("%02d", robot2) + "-" + String.format("%02d", n) + "-" + b);
		
		QBFSafetySolver sol = new QBFSafetySolver(pn, new QBFSolverOptions(n, b));
		
		sol.writeQCIR();
	}
}
