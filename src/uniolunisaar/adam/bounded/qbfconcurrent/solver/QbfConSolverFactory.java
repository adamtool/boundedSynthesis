package uniolunisaar.adam.bounded.qbfconcurrent.solver;

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
 * @author Niklas Metzger
 *
 */
public class QbfConSolverFactory extends LLSolverFactory<QbfConSolverOptions, QbfConSolver<? extends Condition<?>>> {

    private static QbfConSolverFactory instance = null;

    public static QbfConSolverFactory getInstance() {
        if (instance == null) {
            instance = new QbfConSolverFactory();
        }
        return instance;
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
    protected QbfConSolver<? extends Condition<?>> getESafetySolver(SolvingObject<PetriGame, Safety> solverObject, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<? extends Condition<?>> getASafetySolver(SolvingObject<PetriGame, Safety> solverObject, QbfConSolverOptions options) throws SolvingException {
        return new QbfConSafetySolver((QbfSolvingObject<Safety>) solverObject, options);
    }

    @Override
    protected QbfConSolver<? extends Condition<?>> getEReachabilitySolver(SolvingObject<PetriGame, Reachability> solverObject, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<? extends Condition<?>> getAReachabilitySolver(SolvingObject<PetriGame, Reachability> solverObject, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<? extends Condition<?>> getEBuchiSolver(SolvingObject<PetriGame, Buchi> solverObject, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<? extends Condition<?>> getABuchiSolver(SolvingObject<PetriGame, Buchi> solverObject, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
