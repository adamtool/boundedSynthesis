package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.winningconditions.Buchi;

public class QBFForallBuchiSolver extends QBFFlowChainSolver<Buchi> {

	protected QBFForallBuchiSolver(QBFPetriGame game, Buchi winCon, QBFSolverOptions options) {
		super(game, winCon, options);
	}

	public String getBuchiLoop() throws IOException {
		Set<Integer> outerAnd = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		Set<Integer> innerAnd = new HashSet<>();
		
		for (Place p : pn.getPlaces()) {
			outerAnd.add(-getVarNr(p.getId() + "." + pg.getN() + "." + "unsafe", true));
		}
		for (Transition t : pn.getTransitions()) {
			boolean addFlow = false;
			for (Place p : t.getPreset()) {
				boolean cont = false;
				for (Pair<Place, Place> pair : pg.getFl().get(t)) {
					if (pair.getFirst().equals(p)) {
						cont = true;
						break;
					}
				}
				if (!cont) {
					addFlow = true;
				}
			}
			if (addFlow) {
				for (int i = 1; i <= pg.getN(); ++i) {
					outerAnd.add(-getOneTransition(t, i));
				}
			}
		}
		
		for (int i = 1; i < pg.getN(); ++i) {
			innerAnd.clear();
			for (Place p : pn.getPlaces()) {
				innerAnd.add(-getVarNr(p.getId() + "." + i + "." + "unsafe", true));
				
				int p_i_safe = getVarNr(p.getId() + "." + i + "." + "safe", true);
				int p_n_safe = getVarNr(p.getId() + "." + pg.getN() + "." + "safe", true);
				innerAnd.add(writeImplication(p_i_safe, p_n_safe));
				innerAnd.add(writeImplication(p_n_safe, p_i_safe));
				
				int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "unsafe", true);
				int p_n_unsafe = getVarNr(p.getId() + "." + pg.getN() + "." + "unsafe", true);
				innerAnd.add(writeImplication(p_i_unsafe, p_n_unsafe));
				innerAnd.add(writeImplication(p_n_unsafe, p_i_unsafe));
				
				int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
				int p_n_empty = getVarNr(p.getId() + "." + pg.getN() + "." + "empty", true);
				innerAnd.add(writeImplication(p_i_empty, p_n_empty));
				innerAnd.add(writeImplication(p_n_empty, p_i_empty));
			}
			int id = createUniqueID();
			writer.write(id + " = " + writeAnd(innerAnd));
			or.add(id);
		}
		int id = createUniqueID();
		writer.write(id + " = " + writeOr(or));
		outerAnd.add(id);
		return writeAnd(outerAnd);
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
