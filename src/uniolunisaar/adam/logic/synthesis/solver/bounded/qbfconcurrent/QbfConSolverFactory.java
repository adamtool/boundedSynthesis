package uniolunisaar.adam.logic.synthesis.solver.bounded.qbfconcurrent;

import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfconcurrent.QbfConSolverOptions;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolvingObject;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.objectives.Buchi;
import uniolunisaar.adam.ds.objectives.Reachability;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.logic.synthesis.solver.LLSolverFactory;

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
    protected <W extends Condition<W>> QbfSolvingObject<W> createSolvingObject(PetriGameWithTransits game, W winCon) throws NotSupportedGameException {
        try {
            return new QbfSolvingObject<>(game, winCon, false);
        } catch (SolvingException ex) {
            throw new NotSupportedGameException("Could not create solving object.", ex);
        }
    }

    @Override
    protected QbfConSolver<Safety> getESafetySolver(PetriGameWithTransits game, Safety con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Safety> getASafetySolver(PetriGameWithTransits game, Safety con, QbfConSolverOptions options) throws SolvingException {
        return new QbfConSafetySolver(createSolvingObject(game, con), options);
    }

    @Override
    protected QbfConSolver<Reachability> getEReachabilitySolver(PetriGameWithTransits game, Reachability con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Reachability> getAReachabilitySolver(PetriGameWithTransits game, Reachability con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Buchi> getEBuchiSolver(PetriGameWithTransits game, Buchi con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected QbfConSolver<Buchi> getABuchiSolver(PetriGameWithTransits game, Buchi con, QbfConSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
