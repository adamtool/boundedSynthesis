package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.winningconditions.Reachability;

public class QBFReachabilitySolver extends QBFSolver<Reachability> {

	public QBFReachabilitySolver(QBFPetriGame game, Reachability winCon, QBFSolverOptions so) {
		super(game, winCon, so);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean exWinStrat() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected PetriNet calculateStrategy() throws NoStrategyExistentException {
		// TODO Auto-generated method stub
		return null;
	}

}
