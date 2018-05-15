package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniolunisaar.adam.ds.solver.SolverOptions;

/**
 *
 * @author Manuel Gieseking
 */
public class QBFConSolverOptions extends SolverOptions {
    private int n = 0;
    private int b = 0;

    public QBFConSolverOptions() {
        super("con");
    }

    public QBFConSolverOptions(int n, int b) {
        super("con");
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
