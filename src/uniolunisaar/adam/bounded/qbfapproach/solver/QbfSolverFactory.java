package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Buchi;
import uniolunisaar.adam.ds.objectives.Reachability;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.logic.synthesis.solver.LLSolverFactory;

/**
 *
 * @author Manuel Gieseking
 */
public class QbfSolverFactory extends LLSolverFactory<QbfSolverOptions, QbfSolver<? extends Condition<?>>> {

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
    protected <W extends Condition<W>> QbfSolvingObject<W> createSolvingObject(PetriGame game, W winCon) throws NotSupportedGameException {
        try {
            return new QbfSolvingObject<>(game, winCon, false);
        } catch (SolvingException ex) {
            throw new NotSupportedGameException("Could not create solving object.", ex);
        }
    }

    @Override
    protected QbfSolver<Safety> getESafetySolver(PetriGame game, Safety con, QbfSolverOptions options) throws SolvingException {
        return new QbfESafetySolver(createSolvingObject(game, con), options);
    }

    @Override
    protected QbfSolver<Safety> getASafetySolver(PetriGame game, Safety con, QbfSolverOptions options) throws SolvingException {
        return new QbfASafetySolver(createSolvingObject(game, con), options);
    }

    @Override
    protected QbfSolver<Reachability> getEReachabilitySolver(PetriGame game, Reachability con, QbfSolverOptions options) throws SolvingException {
        return new QbfEReachabilitySolver(createSolvingObject(game, con), options);
    }

    @Override
    protected QbfSolver<Reachability> getAReachabilitySolver(PetriGame game, Reachability con, QbfSolverOptions options) throws SolvingException {
        return new QbfAReachabilitySolver(createSolvingObject(game, con), options);
    }

    @Override
    protected QbfSolver<Buchi> getEBuchiSolver(PetriGame game, Buchi con, QbfSolverOptions options) throws SolvingException {
        return new QbfEBuchiSolver(createSolvingObject(game, con), options);
    }

    @Override
    protected QbfSolver<Buchi> getABuchiSolver(PetriGame game, Buchi con, QbfSolverOptions options) throws SolvingException {
        return new QbfABuchiSolver(createSolvingObject(game, con), options);
    }

}
