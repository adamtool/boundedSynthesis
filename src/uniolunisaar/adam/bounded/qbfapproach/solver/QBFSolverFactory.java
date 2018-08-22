package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.solver.SolverFactory;
import uniolunisaar.adam.ds.winningconditions.Buchi;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

/**
 *
 * @author Manuel Gieseking
 */
public class QBFSolverFactory extends SolverFactory<QBFSolverOptions, QBFSolver<? extends WinningCondition>> {

    private static QBFSolverFactory instance = null;

    public static QBFSolverFactory getInstance() {
        if (instance == null) {
            instance = new QBFSolverFactory();
        }
        return instance;
    }

    private QBFSolverFactory() {

    }

    @Override
    protected QBFSafetySolver getESafetySolver(PetriGame game, Safety winCon, boolean skipTests, QBFSolverOptions so) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFSafetySolver(game, winCon, so);
    }

    @Override
    protected QBFSafetySolver getASafetySolver(PetriGame game, Safety winCon, boolean skipTests, QBFSolverOptions so) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFSafetySolver(game, winCon, so);
    }

    @Override
    protected QBFReachabilitySolver getEReachabilitySolver(PetriGame game, Reachability winCon, boolean skipTests, QBFSolverOptions so) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFReachabilitySolver(game, winCon, so);
    }

    @Override
    protected QBFReachabilitySolver getAReachabilitySolver(PetriGame game, Reachability winCon, boolean skipTests, QBFSolverOptions so) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFReachabilitySolver(game, winCon, so);
    }

    @Override
    protected QBFBuchiSolver getEBuchiSolver(PetriGame game, Buchi winCon,  boolean skipTests, QBFSolverOptions so) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFBuchiSolver(game, winCon, so);
    }

    @Override
    protected QBFBuchiSolver getABuchiSolver(PetriGame game, Buchi winCon,  boolean skipTests, QBFSolverOptions so) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFBuchiSolver(game, winCon, so);
    }

}
