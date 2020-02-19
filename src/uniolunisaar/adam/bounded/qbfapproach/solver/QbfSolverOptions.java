package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.ds.solver.LLSolverOptions;

/**
 *
 * @author Manuel Gieseking
 */
public class QbfSolverOptions extends LLSolverOptions {
    private int n = 0;
    private int b = 0;

    public QbfSolverOptions() {
        super("qbf");
    }
    
    public QbfSolverOptions(boolean skip) {
        super(skip, "qbf");
    }
    
    public QbfSolverOptions(int n, int b, boolean skipTests) {
        super(skipTests,"qbf");
	this.n = n;
	this.b = b;
    }

    public QbfSolverOptions(int n, int b) {
        super("qbf");
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
