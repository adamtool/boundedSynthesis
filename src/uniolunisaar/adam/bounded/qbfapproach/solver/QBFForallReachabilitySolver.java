package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.ds.winningconditions.Reachability;

public class QBFForallReachabilitySolver extends QBFFlowChainSolver<Reachability> {
	
	private int[] goodPlaces;
	
	public QBFForallReachabilitySolver(QBFPetriGame game, Reachability winCon, QBFSolverOptions options) throws BoundedParameterMissingException {
		super(game, winCon, options);
		goodPlaces = new int[pg.getN() + 1];
	}
	
	@Override
	public String getInitial() {
		Marking initialMarking = pg.getNet().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : pn.getPlaces()) {
			if (initialMarking.getToken(p).getValue() == 1) {
				if (AdamExtensions.isReach(p)) {
					initial.add( getVarNr(p.getId() + "." + 1 + "." + false, true));
					initial.add(-getVarNr(p.getId() + "." + 1 + "." + true, true));
				} else {
					initial.add(-getVarNr(p.getId() + "." + 1 + "." + false, true));
					initial.add( getVarNr(p.getId() + "." + 1 + "." + true, true));
				}
			} else {
				initial.add(-getVarNr(p.getId() + "." + 1 + "." + false, true));
				initial.add(-getVarNr(p.getId() + "." + 1 + "." + true, true));
			}
		}
		return writeAnd(initial);
	}
	
	@Override
	public int getOneTransition(Transition t, int i) throws IOException {
		if (oneTransitionFormulas[transitionKeys.get(t)][i] == 0) {
			Set<Integer> and = new HashSet<>();
			Set<Integer> or = new HashSet<>();
			int strat;
			int id;
			for (Place p : t.getPreset()) {
				// preset
				or.clear();
				or.add(getVarNr(p.getId() + "." + i + "." + true, true));
				or.add(getVarNr(p.getId() + "." + i + "." + false, true));
				id = createUniqueID();
				writer.write(id + " = " + writeOr(or));
				and.add(id);
				// strategy
				strat = addSysStrategy(p, t);
				if (strat != 0) {
					and.add(strat);
				}
			}
			
			for (Place p : t.getPostset()) {
				// bad place reached
				if (AdamExtensions.isReach(p)) {
					and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "unsafe", true));
				} else {
					// unsafe flow chain before
					and.add(writeImplication(getOneUnsafeFlowChain(p, t, i), getVarNr(p.getId() + "." + (i + 1) + "." + "unsafe", true)));
					// safe flow chain before
					and.add(writeImplication(getAllSafeFlowChain(p, t, i),  getVarNr(p.getId() + "." + (i + 1) + "." + "safe", true)));
				}
			}
			
			Set<Place> places = new HashSet<>(pn.getPlaces());
			places.removeAll(t.getPreset());
			places.removeAll(t.getPostset());
			for (Place p : places) {
				// rest stays the same
				int p_i_safe = getVarNr(p.getId() + "." + i + "." + "safe", true);
				int p_i1_safe = getVarNr(p.getId() + "." + (i + 1) + "." + "safe", true);
				and.add(writeImplication(p_i_safe, p_i1_safe));
				and.add(writeImplication(p_i1_safe, p_i_safe));
				int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "unsafe", true);
				int p_i1_unsafe = getVarNr(p.getId() + "." + (i + 1) + "." + "unsafe", true);
				and.add(writeImplication(p_i_unsafe, p_i1_unsafe));
				and.add(writeImplication(p_i1_unsafe, p_i_unsafe));
				int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
				int p_i1_empty = getVarNr(p.getId() + "." + (i + 1) + "." + "empty", true);
				and.add(writeImplication(p_i_empty, p_i1_empty));
				and.add(writeImplication(p_i1_empty, p_i_empty));
			}
			
			for (Place p : t.getPreset()) {
				if (!t.getPostset().contains(p)) {
					and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "empty", true));
				}
			}
			id = createUniqueID();
			writer.write(id + " = " + writeAnd(and));
			oneTransitionFormulas[transitionKeys.get(t)][i] = id;
			return id;
		}
		else {
			return oneTransitionFormulas[transitionKeys.get(t)][i];
		}
	}
	
	private void writeGoodPlaces() throws IOException {
		if (!getWinningCondition().getPlaces2Reach().isEmpty()) {
			String[] good = getGoodPlaces();
			for (int i = 1; i <= pg.getN(); ++i) {
				goodPlaces[i] = createUniqueID();
				writer.write(goodPlaces[i] + " = " + good[i]);//"or()" + "\n");
			}
		}
	}
	
	public String[] getGoodPlaces() throws IOException {
		String[] goodPlaces = new String[pg.getN() + 1];
		Set<Integer> or = new HashSet<>();
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			and.clear();
			for (Place p : pn.getPlaces()) {
				and.add(-getVarNr(p.getId() + "." + i + "." + "safe", true));
			}
			for (Transition t : pn.getTransitions()) {
				for (Place p : t.getPreset()) {
					boolean notPresent = true;
					for (Pair<Place, Place> pair : pg.getFl().get(t)) {
						if (pair.getFirst().equals(p)) {
							notPresent = false;
							break;
						}
					}
					if (notPresent) {
						for (int j = 1; j < i; ++j) {
							or.clear();
							or.add(getVarNr(p.getId() + "." + j + "." + "unsafe", true));
							or.add(-getOneTransition(t, j));
							int id = createUniqueID();
							writer.write(id + " = " + writeOr(or));
							and.add(id);
						}
					}
				}
			}
			goodPlaces[i] = writeAnd(and);
		}
		return goodPlaces;
	}
	
	private void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			and.add(dlt[i]);
			and.add(det[i]);
			if (goodPlaces[i] != 0) {
				and.add(writeImplication(dl[i], goodPlaces[i]));
			} else {
				// empty set of places to reach never lets system win
				Pair<Boolean, Integer> result = getVarNrWithResult("or()");
				if (result.getFirst()) {
					writer.write(result.getSecond() + " = or()" + QBFSolver.linebreak);
				}
				and.add(writeImplication(dl[i], result.getSecond()));
			}
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));

		}
		and.clear();
		and.add(dlt[pg.getN()]);
		and.add(det[pg.getN()]);
		if (goodPlaces[pg.getN()] != 0) {
			and.add(goodPlaces[pg.getN()]);
		} else {
			// empty set of places to reach never lets system win
			Pair<Boolean, Integer> result = getVarNrWithResult("or()");
			if (result.getFirst()) {
				writer.write(result.getSecond() + " = or()" + QBFSolver.linebreak);
			}
			and.add(result.getSecond());
		}
		win[pg.getN()] = createUniqueID();
		writer.write(win[pg.getN()] + " = " + writeAnd(and));
	}
	
	public String getUnfair() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		Set<Integer> outerAnd = new HashSet<>();
		Set<Integer> innerOr = new HashSet<>();
		Set<Integer> innerAnd = new HashSet<>();
		
		for (int i = 1; i < pg.getN() - 1; ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				outerAnd.clear();
				for (Place p : pn.getPlaces()) {
					if (!p.getId().startsWith(additionalSystemName)) {
						int p_i_safe = getVarNr(p.getId() + "." + i + "." + "safe", true);
						int p_j_safe = getVarNr(p.getId() + "." + j + "." + "safe", true);
						outerAnd.add(writeImplication(p_i_safe, p_j_safe));
						outerAnd.add(writeImplication(p_j_safe, p_i_safe));
						
						int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "unsafe", true);
						int p_j_unsafe = getVarNr(p.getId() + "." + j + "." + "unsafe", true);
						outerAnd.add(writeImplication(p_i_unsafe, p_j_unsafe));
						outerAnd.add(writeImplication(p_j_unsafe, p_i_unsafe));
						
						int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
						int p_j_empty = getVarNr(p.getId() + "." + j + "." + "empty", true);
						outerAnd.add(writeImplication(p_i_empty, p_j_empty));
						outerAnd.add(writeImplication(p_j_empty, p_i_empty));
					}
				}
				innerOr.clear();
				for (Transition t : pn.getTransitions()) {
					innerAnd.clear();
					for (int k = i; k < j; ++k){
						for (Place p : t.getPreset()) {
							int id = createUniqueID();
							writer.write(id + " = or(" + getVarNr(p.getId() + "." + k + "." + "safe", true) + "," + getVarNr(p.getId() + "." + k + "." + "unsafe", true) + ")" + QBFSolver.linebreak);
							innerAnd.add(id);
							int strategy = addSysStrategy(p, t);
							if (strategy != 0) {
								innerAnd.add(strategy);
							}
							for (Transition tt : p.getPostset()) {
								innerAnd.add(-getOneTransition(tt, k));
							}
						}
					}
					int id = createUniqueID();
					writer.write(id + " = " + writeAnd(innerAnd));
					innerOr.add(id);
				}
				int id = createUniqueID();
				writer.write(id + " = " + writeOr(innerOr));
				outerAnd.add(id);
				id = createUniqueID();
				writer.write(id + " = " + writeAnd(outerAnd));
				outerOr.add(id);
			}
		}
		return writeOr(outerOr);
	}
	
	@Override
	protected void writeQCIR() throws IOException {
		Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = unfoldPG();

		initializeVariablesForWriteQCIR();
		
		writer.write("#QCIR-G14          " + QBFSolver.linebreak); // spaces left to add variable count in the end
		addExists();
		addForall();

		writeInitial();
		writeFlow();
		writeSequence();
		writeGoodPlaces();
		writeDeadlock();
		writeTerminating();
		writeDeadlocksterm();
		writeDeterministic();
		writeWinning();
		writeLoop();
		writeUnfair();

		Set<Integer> phi = new HashSet<>();
		// When unfolding non-deterministically we add system places to
		// ensure deterministic decision.
		// It is required that these decide for exactly one transition which
		// is directly encoded into the problem.
		int index_for_non_det_unfolding_info = enumerateStratForNonDetUnfold(systemHasToDecideForAtLeastOne);
		if (index_for_non_det_unfolding_info != -1) {
			phi.add(index_for_non_det_unfolding_info);
		}

		for (int i = 1; i <= pg.getN() - 1; ++i) { // slightly optimized in the sense that winning and loop are put together for n
			seqImpliesWin[i] = createUniqueID();
			writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QBFSolver.linebreak);
			phi.add(seqImpliesWin[i]);
		}

		int winandLoop = createUniqueID();
		Set<Integer> winandLoopSet = new HashSet<>();
		winandLoopSet.add(l);
		winandLoopSet.add(win[pg.getN()]);
		writer.write(winandLoop + " = " + writeAnd(winandLoopSet));
		
		// TODO loop direkt bei getWinning hinzufügen
		seqImpliesWin[pg.getN()] = createUniqueID();
		writer.write(seqImpliesWin[pg.getN()] + " = " + "or(-" + seq[pg.getN()] + "," + winandLoop + "," + u + ")" + QBFSolver.linebreak);
		phi.add(seqImpliesWin[pg.getN()]);

		writer.write("1 = " + writeAnd(phi));
		writer.close();

		// Total number of gates is only calculated during encoding and added to the file afterwards
		if (variablesCounter < 999999999) { // added 9 blanks as more than 999.999.999 variables wont be solvable
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			for (int i = 0; i < 10; ++i) { // read "#QCIR-G14 "
				raf.readByte();
			}
			String counter_str = Integer.toString(variablesCounter - 1); // has NEXT usabel counter in it
			char[] counter_char = counter_str.toCharArray();
			for (char c : counter_char) {
				raf.writeByte(c);
			}
			raf.close();
		}
		
		if (QBFSolver.debug) {
			FileUtils.copyFile(file, new File(pn.getName() + ".qcir"));
		}
	}
}
