package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.solver.SolverFactory;

/**
 *
 * @author Manuel Gieseking
 */
public class QBFSolverFactory extends SolverFactory<QBFSolver, QBFSolverOptions> {

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
    protected QBFSolver getSafetySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException {
        return new QBFSafetySolver(pn, so);
    }

    @Override
    protected QBFSolver getReachabilitySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException {
        throw new RuntimeException("Method not yet implemented.");
    }

    @Override
    protected QBFSolver getBuchiSolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException {
        throw new RuntimeException("Method not yet implemented.");
    }

}
