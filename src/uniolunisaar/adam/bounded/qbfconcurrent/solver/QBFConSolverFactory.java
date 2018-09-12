package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Buchi;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.logic.solver.SolverFactory;

/**
 * 
 * @author niklasmetzger
 *
 */

public class QBFConSolverFactory extends SolverFactory<QBFConSolverOptions, QBFConSolver<? extends WinningCondition>> {

	private static QBFConSolverFactory instance = null;

	public static QBFConSolverFactory getInstance() {
		if (instance == null) {
			instance = new QBFConSolverFactory();
		}
		return instance;
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getESafetySolver(PetriGame game, Safety winCon,
			boolean skipTests, QBFConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getASafetySolver(PetriGame game, Safety winCon,
			boolean skipTests, QBFConSolverOptions options) throws SolvingException {
		return new QBFConSafetySolver(game, winCon, options);
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getEReachabilitySolver(PetriGame game, Reachability winCon,
			boolean skipTests, QBFConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getAReachabilitySolver(PetriGame game, Reachability winCon,
			boolean skipTests, QBFConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getEBuchiSolver(PetriGame game, Buchi winCon, boolean skipTests,
			QBFConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getABuchiSolver(PetriGame game, Buchi winCon, boolean skipTests,
			QBFConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}
}
