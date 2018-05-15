package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.exceptions.ParameterMissingException;
import uniolunisaar.adam.logic.solver.SolverFactory;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

/**
 * Small hack of instantiating QBF Solver, avoiding code dupication
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

	private QBFConSolverFactory() {

	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getESafetySolver(PetriNet net, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getASafetySolver(PetriNet net, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
		return new QBFConSafetySolver(net, new Safety(true), options);
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getEReachabilitySolver(PetriNet net, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getAReachabilitySolver(PetriNet net, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getEBuchiSolver(PetriNet net, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
		throw new RuntimeException("Method not yet implemented");
	}

	@Override
	protected QBFConSolver<? extends WinningCondition> getABuchiSolver(PetriNet net, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
		throw new RuntimeException("Method not yet implemented");
	}

	

}
