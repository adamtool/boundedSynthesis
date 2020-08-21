package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Buchi;
import uniolunisaar.adam.ds.objectives.Reachability;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.objectives.Condition;
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
    protected <W extends Condition<W>> QbfSolvingObject<W> createSolvingObject(PetriGame game, W winCon) throws NotSupportedGameException {
        try {
            return new QbfSolvingObject<>(game, winCon, false);
        } catch (SolvingException ex) {
            throw new NotSupportedGameException("Could not create solving object.", ex);
        }
    }

    @Override
    protected QbfConSolver<Safety> getESafetySolver(PetriGame game, Safety con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Safety> getASafetySolver(PetriGame game, Safety con, QbfConSolverOptions options) throws SolvingException {
        return new QbfConSafetySolver(createSolvingObject(game, con), options);
    }

    @Override
    protected QbfConSolver<Reachability> getEReachabilitySolver(PetriGame game, Reachability con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Reachability> getAReachabilitySolver(PetriGame game, Reachability con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Buchi> getEBuchiSolver(PetriGame game, Buchi con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Buchi> getABuchiSolver(PetriGame game, Buchi con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
