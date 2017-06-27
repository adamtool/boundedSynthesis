package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.solver.SolverFactory;
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
    protected QBFSolver<? extends WinningCondition> getSafetySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException {
        return new QBFSafetySolver(pn, so);
    }

    @Override
    protected QBFSolver<? extends WinningCondition> getReachabilitySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException {
    	return new QBFReachabilitySolver(pn, so);
    }

    @Override
    protected QBFSolver<? extends WinningCondition> getBuchiSolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException {
    	return new QBFBuchiSolver(pn, so);
    }

}
