package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniolunisaar.adam.ds.synthesis.solver.LLSolverOptions;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class QbfConSolverOptions extends LLSolverOptions {

	private int n = 0;
	private int b = 0;

	public QbfConSolverOptions() {
		super("qbfcon");
	}
        
	public QbfConSolverOptions(boolean skip) {
		super(skip, "qbfcon");
	}
        
	public QbfConSolverOptions(int n, int b, boolean skipTests) {
                super(skipTests,"qbfcon");
		this.n = n;
		this.b = b;
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
