package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.solver.SolverOptions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public abstract class QbfSharedSolver<W extends WinningCondition, SOP extends SolverOptions> extends Solver<QBFSolvingObject<W>, SOP>{

	protected QbfSharedSolver(QBFSolvingObject<W> solverObject, SOP options) {
		super(solverObject, options);
	}

}
