package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public abstract class QBFSolver<W extends WinningCondition> extends Solver<QBFPetriGame, W, QBFSolverOptions> {

    public QBFSolver(QBFPetriGame game, W winCon, QBFSolverOptions so) {
        super(game, winCon, so);
    }

}
