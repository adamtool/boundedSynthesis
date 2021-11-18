package uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach;

import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolverOptions;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolvingObject;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.objectives.local.Buchi;
import uniolunisaar.adam.ds.objectives.local.Reachability;
import uniolunisaar.adam.ds.objectives.local.Safety;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
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
    protected <W extends Condition<W>> QbfSolvingObject<W> createSolvingObject(PetriGameWithTransits game, W winCon, QbfSolverOptions options) throws NotSupportedGameException {
        try {
            return new QbfSolvingObject<>(game, winCon, false);
        } catch (SolvingException ex) {
            throw new NotSupportedGameException("Could not create solving object.", ex);
        }
    }

    @Override
    protected QbfSolver<Safety> getESafetySolver(PetriGameWithTransits game, Safety con, QbfSolverOptions options) throws SolvingException {
        return new QbfESafetySolver(createSolvingObject(game, con, options), options);
    }

    @Override
    protected QbfSolver<Safety> getASafetySolver(PetriGameWithTransits game, Safety con, QbfSolverOptions options) throws SolvingException {
        return new QbfASafetySolver(createSolvingObject(game, con, options), options);
    }

    @Override
    protected QbfSolver<Reachability> getEReachabilitySolver(PetriGameWithTransits game, Reachability con, QbfSolverOptions options) throws SolvingException {
        return new QbfEReachabilitySolver(createSolvingObject(game, con, options), options);
    }

    @Override
    protected QbfSolver<Reachability> getAReachabilitySolver(PetriGameWithTransits game, Reachability con, QbfSolverOptions options) throws SolvingException {
        return new QbfAReachabilitySolver(createSolvingObject(game, con, options), options);
    }

    @Override
    protected QbfSolver<Buchi> getEBuchiSolver(PetriGameWithTransits game, Buchi con, QbfSolverOptions options) throws SolvingException {
        return new QbfEBuchiSolver(createSolvingObject(game, con, options), options);
    }

    @Override
    protected QbfSolver<Buchi> getABuchiSolver(PetriGameWithTransits game, Buchi con, QbfSolverOptions options) throws SolvingException {
        return new QbfABuchiSolver(createSolvingObject(game, con, options), options);
    }

}
