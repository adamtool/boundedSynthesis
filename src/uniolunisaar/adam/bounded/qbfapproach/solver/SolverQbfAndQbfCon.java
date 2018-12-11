package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.solver.SolverOptions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public abstract class SolverQbfAndQbfCon<W extends WinningCondition, SOP extends SolverOptions> extends Solver<QbfSolvingObject<W>, SOP>{

	protected SolverQbfAndQbfCon(QbfSolvingObject<W> solverObject, SOP options) {
		super(solverObject, options);
	}

}
