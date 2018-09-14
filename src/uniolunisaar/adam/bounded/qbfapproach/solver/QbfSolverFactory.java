package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Buchi;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.logic.solver.SolverFactory;

/**
 *
 * @author Manuel Gieseking
 */
public class QbfSolverFactory extends SolverFactory<QbfSolverOptions, QbfSolver<? extends WinningCondition>> {

    private static QbfSolverFactory instance = null;

    public static QbfSolverFactory getInstance() {
        if (instance == null) {
            instance = new QbfSolverFactory();
        }
        return instance;
    }

    private QbfSolverFactory() {

    }

    @Override
    protected QbfESafetySolver getESafetySolver(PetriGame game, Safety winCon, boolean skipTests, QbfSolverOptions so) throws SolvingException {
        return new QbfESafetySolver(game, winCon, so);
    }

    @Override
    protected QbfASafetySolver getASafetySolver(PetriGame game, Safety winCon, boolean skipTests, QbfSolverOptions so) throws SolvingException {
        return new QbfASafetySolver(game, winCon, so);
    }

    @Override
    protected QbfEReachabilitySolver getEReachabilitySolver(PetriGame game, Reachability winCon, boolean skipTests, QbfSolverOptions so) throws SolvingException {
        return new QbfEReachabilitySolver(game, winCon, so);
    }

    @Override
    protected QbfAReachabilitySolver getAReachabilitySolver(PetriGame game, Reachability winCon, boolean skipTests, QbfSolverOptions so) throws SolvingException {
        return new QbfAReachabilitySolver(game, winCon, so);
    }

    @Override
    protected QbfEBuchiSolver getEBuchiSolver(PetriGame game, Buchi winCon, boolean skipTests, QbfSolverOptions so) throws SolvingException {
        return new QbfEBuchiSolver(game, winCon, so);
    }

    @Override
    protected QbfABuchiSolver getABuchiSolver(PetriGame game, Buchi winCon, boolean skipTests, QbfSolverOptions so) throws SolvingException {
        return new QbfABuchiSolver(game, winCon, so);
    }

}