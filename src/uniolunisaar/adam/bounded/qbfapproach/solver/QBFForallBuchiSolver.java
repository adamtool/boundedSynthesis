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
import uniolunisaar.adam.ds.winningconditions.Buchi;

public class QBFForallBuchiSolver extends QBFFlowChainSolver<Buchi> {

	private int bl; // buchi loop
	
	public QBFForallBuchiSolver(QBFPetriGame game, Buchi winCon, QBFSolverOptions options) throws BoundedParameterMissingException{
		super(game, winCon, options);
	}
	
	@Override
	public String getInitial() {
		Marking initialMarking = pg.getNet().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : pn.getPlaces()) {
			if (initialMarking.getToken(p).getValue() == 1) {
				if (AdamExtensions.isBuchi(p)) {
					initial.add(getVarNr(p.getId() + "." + 1 + "." + "objective", true));
				} else {
					initial.add(getVarNr(p.getId() + "." + 1 + "." + "notobjective", true));
				}
			} else {
				initial.add(getVarNr(p.getId() + "." + 1 + "." + "empty", true));
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
				or.add(getVarNr(p.getId() + "." + i + "." + "objective", true));
				or.add(getVarNr(p.getId() + "." + i + "." + "notobjective", true));
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
				if (AdamExtensions.isBuchi(p)) {
					and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "objective", true));
				} else {
					Set<Place> tokenFlow = getIncomingTokenFlow(t, p);
					if (tokenFlow.isEmpty()) {
						and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "notobjective", true));
					} else {
						and.add(writeImplication(getAllObjectiveFlowChain(p, t, i, tokenFlow), getVarNr(p.getId() + "." + (i + 1) + "." + "objective", true)));
						and.add(writeImplication(getOneNotObjectiveFlowChain(p, t, i, tokenFlow),  getVarNr(p.getId() + "." + (i + 1) + "." + "notobjective", true)));
					}
				}
			}
			
			Set<Place> places = new HashSet<>(pn.getPlaces());
			places.removeAll(t.getPreset());
			places.removeAll(t.getPostset());
			for (Place p : places) {
				// rest stays the same
				int p_i_safe = getVarNr(p.getId() + "." + i + "." + "objective", true);
				int p_i1_safe = getVarNr(p.getId() + "." + (i + 1) + "." + "objective", true);
				and.add(writeImplication(p_i_safe, p_i1_safe));
				and.add(writeImplication(p_i1_safe, p_i_safe));
				int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "notobjective", true);
				int p_i1_unsafe = getVarNr(p.getId() + "." + (i + 1) + "." + "notobjective", true);
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
	
	@Override
	protected void writeLoop() throws IOException {
		String loop = getBuchiLoop();
		bl = createUniqueID();
		writer.write(bl + " = " + loop);
	}

	public String getBuchiLoop() throws IOException {
		Set<Integer> outerAnd = new HashSet<>();
		Set<Integer> outerOr = new HashSet<>();
		Set<Integer> innerAnd = new HashSet<>();
		Set<Integer> innerOr = new HashSet<>();
		Set<Integer> innerInnerAnd = new HashSet<>();
		
		for (Transition t : pn.getTransitions()) {
			boolean addFlow = false;
			for (Place p : t.getPreset()) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					boolean cont = false;
					for (Pair<Place, Place> pair : pg.getFl().get(t)) {
						if (pair.getFirst().equals(p)) {
							cont = true;
							break;
						}
					}
					if (!cont) {
						addFlow = true;
						break;
					}
				}
			}
			if (addFlow) {
				for (int i = 1; i < pg.getN(); ++i) {
					outerAnd.add(-getOneTransition(t, i));	// this transition removes a token
				}
			}
		}
		
		for (int i = 1; i < pg.getN(); ++i) {
			innerAnd.clear();
			for (Place p : pn.getPlaces()) {				
				int p_i_safe = getVarNr(p.getId() + "." + i + "." + "objective", true);
				int p_n_safe = getVarNr(p.getId() + "." + pg.getN() + "." + "objective", true);
				innerAnd.add(writeImplication(p_i_safe, p_n_safe));
				innerAnd.add(writeImplication(p_n_safe, p_i_safe));
				
				int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "notobjective", true);
				int p_n_unsafe = getVarNr(p.getId() + "." + pg.getN() + "." + "notobjective", true);
				innerAnd.add(writeImplication(p_i_unsafe, p_n_unsafe));
				innerAnd.add(writeImplication(p_n_unsafe, p_i_unsafe));
				
				int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
				int p_n_empty = getVarNr(p.getId() + "." + pg.getN() + "." + "empty", true);
				innerAnd.add(writeImplication(p_i_empty, p_n_empty));
				innerAnd.add(writeImplication(p_n_empty, p_i_empty));
			}
			
			innerOr.clear();
			for (int j = i; j < pg.getN(); ++j) {
				innerInnerAnd.clear();
				for (Place p : pn.getPlaces()) {
					innerInnerAnd.add(-getVarNr(p.getId() + "." + j + "." + "notobjective", true));
					innerInnerAnd.add(writeImplication(getVarNr(p.getId() + "." +  j + "." + "objective", true), getVarNr(p.getId() + "." +  (j + 1) + "." + "notobjective", true)));
				}
			}
			int id = createUniqueID();
			writer.write(id + " = " + writeAnd(innerInnerAnd));
			innerOr.add(id);
			
			id = createUniqueID();
			writer.write(id + " = " + writeOr(innerOr));
			innerAnd.add(id);
		}
		int id = createUniqueID();
		writer.write(id + " = " + writeAnd(innerAnd));
		outerOr.add(id);
		
		id = createUniqueID();
		writer.write(id + " = " + writeOr(outerOr));
		outerAnd.add(id);
		return writeAnd(outerAnd);
	}

	private void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			and.clear();
			if (dl[i] != 0) {
				and.add(-dl[i]);
			}
			if (det[i] != 0) {
				and.add(det[i]);
			}
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));
		}
	}
	
	@Override
	protected void writeQCIR() throws IOException {
		Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = unfoldPG();

		initializeVariablesForWriteQCIR();
		
		writer.write("#QCIR-G14          " + QBFSolver.linebreak); // spaces left to add variable count in the end
		addExists();
		addForall();

		writeInitial();
		writeDeadlock();
		writeFlow();
		writeSequence();
		writeDeterministic();
		writeLoop();
		writeUnfair();
		writeWinning();

		Set<Integer> phi = new HashSet<>();
		// When unfolding non-deterministically we add system places to ensure deterministic decision.
		// It is required that these decide for exactly one transition which is directly encoded into the problem.
		int index_for_non_det_unfolding_info = enumerateStratForNonDetUnfold(systemHasToDecideForAtLeastOne);
		if (index_for_non_det_unfolding_info != -1) {
			phi.add(index_for_non_det_unfolding_info);
		}

		for (int i = 1; i <= pg.getN() - 1; ++i) { // slightly optimized in the sense that winning and loop are put together for i = n
			seqImpliesWin[i] = createUniqueID();
			writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QBFSolver.linebreak);
			phi.add(seqImpliesWin[i]);
		}

		int wnandLoop = createUniqueID();
		Set<Integer> wnandLoopSet = new HashSet<>();
		wnandLoopSet.add(bl);
		wnandLoopSet.add(win[pg.getN()]);
		writer.write(wnandLoop + " = " + writeAnd(wnandLoopSet));

		seqImpliesWin[pg.getN()] = createUniqueID();
		writer.write(seqImpliesWin[pg.getN()] + " = " + "or(-" + seq[pg.getN()] + "," + wnandLoop + "," + u + ")" + QBFSolver.linebreak);
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
