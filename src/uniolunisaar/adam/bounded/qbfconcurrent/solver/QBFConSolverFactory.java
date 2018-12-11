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

public class QbfConSolverFactory extends SolverFactory<QbfConSolverOptions, QbfConSolverEnvDecision<? extends WinningCondition>> {

	private static QbfConSolverFactory instance = null;

	public static QbfConSolverFactory getInstance() {
		if (instance == null) {
			instance = new QbfConSolverFactory();
		}
		return instance;
	}

	@Override
	protected QbfConSolverEnvDecision<? extends WinningCondition> getESafetySolver(PetriGame game, Safety winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolverEnvDecision<? extends WinningCondition> getASafetySolver(PetriGame game, Safety winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		return new QbfConSafetySolverEnvDecision(game, winCon, options);
	}

	@Override
	protected QbfConSolverEnvDecision<? extends WinningCondition> getEReachabilitySolver(PetriGame game, Reachability winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolverEnvDecision<? extends WinningCondition> getAReachabilitySolver(PetriGame game, Reachability winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolverEnvDecision<? extends WinningCondition> getEBuchiSolver(PetriGame game, Buchi winCon, boolean skipTests,
			QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolverEnvDecision<? extends WinningCondition> getABuchiSolver(PetriGame game, Buchi winCon, boolean skipTests,
			QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}
}
