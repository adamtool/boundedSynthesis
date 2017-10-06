package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.solver.SolverFactory;
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
    protected QBFSolver<? extends WinningCondition> getESafetySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFSafetySolver(pn, new Safety(true), so);
    }

    @Override
    protected QBFSolver<? extends WinningCondition> getASafetySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFSafetySolver(pn, new Safety(false), so);
    }

    @Override
    protected QBFSolver<? extends WinningCondition> getEReachabilitySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFReachabilitySolver(pn, new Reachability(true), so);
    }

    @Override
    protected QBFSolver<? extends WinningCondition> getAReachabilitySolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFReachabilitySolver(pn, new Reachability(false), so);
    }

    @Override
    protected QBFSolver<? extends WinningCondition> getEBuchiSolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFBuchiSolver(pn, new Buchi(true), so);
    }

    @Override
    protected QBFSolver<? extends WinningCondition> getABuchiSolver(PetriNet pn, boolean skipTests, QBFSolverOptions so) throws UnboundedPGException, NetNotSafeException, NoSuitableDistributionFoundException, BoundedParameterMissingException {
        return new QBFBuchiSolver(pn, new Buchi(false), so);
    }

}
