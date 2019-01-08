package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniolunisaar.adam.ds.solver.SolverOptions;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class QbfConSolverOptions extends SolverOptions {

	private int n = 0;
	private int b = 0;

	public QbfConSolverOptions() {
		super("qbfcon");
	}

	public QbfConSolverOptions(int n, int b) {
		super("qbfcon");
		this.n = n;
		this.b = b;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getB() {
		return b;
	}

	public void setB(int b) {
		this.b = b;
	}

}
