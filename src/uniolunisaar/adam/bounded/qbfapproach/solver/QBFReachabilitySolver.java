package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.winningconditions.Reachability;

public class QBFReachabilitySolver extends QBFSolver<Reachability> {

	// variable to store keys of calculated components for later use (special to this winning condition)
	private int[] goodPlaces;

	public QBFReachabilitySolver(PetriNet net, QBFSolverOptions so) throws UnboundedPGException {
		super(new QBFPetriGame(net), new Reachability(), so);
		goodPlaces = new int[pg.getN()];
	}

	private void writeGoodPlaces() throws IOException {
		if (!getWinningCondition().getPlaces2Reach().isEmpty()) {
			String[] good = getGoodPlaces();
			for (int i = 1; i <= pg.getN(); ++i) {
				goodPlaces[i] = createUniqueID();
				writer.write(goodPlaces[i] + " = " + good[i]);
			}
		}
	}

	public String[] getGoodPlaces() {
		String[] goodPlaces = new String[pg.getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			or.clear();
			for (Place p : getWinningCondition().getPlaces2Reach()) {
				or.add(getVarNr(p.getId() + "." + i, true));
			}
			goodPlaces[i] = writeOr(or);
		}
		return goodPlaces;
	}

	private void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			if (dlt[i] != 0) {
				and.add(dlt[i]);
			}
			if (det[i] != 0) {
				and.add(det[i]);
			}
			or.clear();
			or.add(fl[i +1 ]);
			for (int j = 1; j <= i; ++j) {
				or.add(goodPlaces[j]);
			}
			int orID = createUniqueID();
			writer.write(orID + " = " + writeOr(or));
			and.add(orID);
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));

		}
		and.clear();
		if (dlt[pg.getN()] != 0) {
			and.add(dlt[pg.getN()]);
		}
		if (det[pg.getN()] != 0) {
			and.add(det[pg.getN()]);
		}
		or.clear();
		for (int i = 1; i <= pg.getN(); ++i) {
			or.add(goodPlaces[i]);
		}
		int orID = createUniqueID();
		writer.write(orID + " = " + writeOr(or));
		and.add(orID);
		win[pg.getN()] = createUniqueID();
		writer.write(win[pg.getN()] + " = " + writeAnd(and));
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
