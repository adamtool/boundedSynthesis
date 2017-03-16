package uniolunisaar.adam.bounded.qbfapproach.solver;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.winningconditions.Buchi;

public class QBFBuchiSolver extends QBFSolver<Buchi> {

	public QBFBuchiSolver(QBFPetriGame game, Buchi winCon, QBFSolverOptions so) {
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
