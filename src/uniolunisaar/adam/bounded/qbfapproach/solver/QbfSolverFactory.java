package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinet.objectives.Buchi;
import uniolunisaar.adam.ds.petrinet.objectives.Reachability;
import uniolunisaar.adam.ds.petrinet.objectives.Safety;
import uniolunisaar.adam.ds.petrinet.objectives.Condition;
import uniolunisaar.adam.ds.solver.SolvingObject;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.logic.pg.solver.LLSolverFactory;

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
    protected <W extends Condition<W>> SolvingObject<PetriGame, W> createSolvingObject(PetriGame game, W winCon) throws NotSupportedGameException {
        try {
            return new QbfSolvingObject<>(game, winCon, false);
        } catch (SolvingException ex) {
            throw new NotSupportedGameException("Could not create solving object.", ex);
        }
    }

    @Override
    protected QbfSolver<? extends Condition<?>> getESafetySolver(SolvingObject<PetriGame, Safety> solverObject, QbfSolverOptions options) throws SolvingException {
        return new QbfESafetySolver((QbfSolvingObject<Safety>) solverObject, options);
    }

    @Override
    protected QbfSolver<? extends Condition<?>> getASafetySolver(SolvingObject<PetriGame, Safety> solverObject, QbfSolverOptions options) throws SolvingException {
        return new QbfASafetySolver((QbfSolvingObject<Safety>) solverObject, options);
    }

    @Override
    protected QbfSolver<? extends Condition<?>> getEReachabilitySolver(SolvingObject<PetriGame, Reachability> solverObject, QbfSolverOptions options) throws SolvingException {
        return new QbfEReachabilitySolver((QbfSolvingObject<Reachability>) solverObject, options);
    }

    @Override
    protected QbfSolver<? extends Condition<?>> getAReachabilitySolver(SolvingObject<PetriGame, Reachability> solverObject, QbfSolverOptions options) throws SolvingException {
        return new QbfAReachabilitySolver((QbfSolvingObject<Reachability>) solverObject, options);
    }

    @Override
    protected QbfSolver<? extends Condition<?>> getEBuchiSolver(SolvingObject<PetriGame, Buchi> solverObject, QbfSolverOptions options) throws SolvingException {
        return new QbfEBuchiSolver((QbfSolvingObject<Buchi>) solverObject, options);
    }

    @Override
    protected QbfSolver<? extends Condition<?>> getABuchiSolver(SolvingObject<PetriGame, Buchi> solverObject, QbfSolverOptions options) throws SolvingException {
        return new QbfABuchiSolver((QbfSolvingObject<Buchi>) solverObject, options);
    }

}
