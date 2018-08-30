package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.logic.exceptions.ParameterMissingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Buchi;
import uniolunisaar.adam.ds.winningconditions.Reachability;
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
    protected QBFConSolver<? extends WinningCondition> getESafetySolver(PetriGame game, Safety winCon, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, ParameterMissingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QBFConSolver<? extends WinningCondition> getASafetySolver(PetriGame game, Safety winCon, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, ParameterMissingException {
        return new QBFConSafetySolver(game, winCon, options);
    }

    @Override
    protected QBFConSolver<? extends WinningCondition> getEReachabilitySolver(PetriGame game, Reachability winCon, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, ParameterMissingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QBFConSolver<? extends WinningCondition> getAReachabilitySolver(PetriGame game, Reachability winCon, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, ParameterMissingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QBFConSolver<? extends WinningCondition> getEBuchiSolver(PetriGame game, Buchi winCon, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, ParameterMissingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QBFConSolver<? extends WinningCondition> getABuchiSolver(PetriGame game, Buchi winCon, boolean skipTests, QBFConSolverOptions options) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, ParameterMissingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

	

}
