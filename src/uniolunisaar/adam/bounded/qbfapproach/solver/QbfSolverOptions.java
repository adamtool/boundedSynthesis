package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.ds.solver.SolverOptions;

/**
 *
 * @author Manuel Gieseking
 */
public class QbfSolverOptions extends SolverOptions {
    private int n = 0;
    private int b = 0;

    public QbfSolverOptions() {
        super("qbf");
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