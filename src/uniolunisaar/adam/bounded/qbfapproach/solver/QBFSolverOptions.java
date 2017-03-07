package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.ds.solver.SolverOptions;

/**
 *
 * @author Manuel Gieseking
 */
public class QBFSolverOptions extends SolverOptions {
// TODO: Jesko adapt here
    private int b1 = 0;
    private int b2 = 0;

    public QBFSolverOptions() {
        super("qbf");
    }

    public QBFSolverOptions(int b1, int b2) {
        super("qbf");
        this.b1 = b1;
        this.b2 = b2;
    }

    public int getB1() {
        return b1;
    }

    public void setB1(int b1) {
        this.b1 = b1;
    }

    public int getB2() {
        return b2;
    }

    public void setB2(int b2) {
        this.b2 = b2;
    }

}
