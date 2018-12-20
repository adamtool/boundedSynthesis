package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Buchi;
import uniolunisaar.adam.ds.objectives.Reachability;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.logic.solver.SolverFactory;

/**
 * 
 * @author niklasmetzger
 *
 */

public class QbfConSolverFactory extends SolverFactory<QbfConSolverOptions, QbfConSolver<? extends Condition>> {

	private static QbfConSolverFactory instance = null;

	public static QbfConSolverFactory getInstance() {
		if (instance == null) {
			instance = new QbfConSolverFactory();
		}
		return instance;
	}

	@Override
	protected QbfConSolver<? extends Condition> getESafetySolver(PetriGame game, Safety winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolver<? extends Condition> getASafetySolver(PetriGame game, Safety winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		return new QbfConSafetySolver(game, winCon, options);
	}

	@Override
	protected QbfConSolver<? extends Condition> getEReachabilitySolver(PetriGame game, Reachability winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolver<? extends Condition> getAReachabilitySolver(PetriGame game, Reachability winCon,
			boolean skipTests, QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolver<? extends Condition> getEBuchiSolver(PetriGame game, Buchi winCon, boolean skipTests,
			QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QbfConSolver<? extends Condition> getABuchiSolver(PetriGame game, Buchi winCon, boolean skipTests,
			QbfConSolverOptions options) throws SolvingException {
		throw new RuntimeException("Method not yet implemented");
	}
}
