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
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.ds.winningconditions.Safety;

public class QBFExistsSafetySolver extends QBFFlowChainSolver<Safety> {
	
	private int[] bad;
	private int sFlCE;
	
	public QBFExistsSafetySolver(QBFPetriGame game, Safety winCon, QBFSolverOptions options) throws BoundedParameterMissingException {
		super(game, winCon, options);
		bad = new int[pg.getN() + 1];
	}
	
	@Override
	public String getInitial() {
		Marking initialMarking = pg.getNet().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : pn.getPlaces()) {
			if (initialMarking.getToken(p).getValue() == 1) {
				if (AdamExtensions.isBad(p)) {
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
				if (AdamExtensions.isBad(p)) {
					and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "objective", true));
				} else {
					Set<Place> tokenFlow = getIncomingTokenFlow(t, p);
					if (tokenFlow.isEmpty()) {
						and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "notobjective", true));
					} else {
						// all unsafe flow chains before
						and.add(writeImplication(getAllObjectiveFlowChain(p, t, i, tokenFlow), getVarNr(p.getId() + "." + (i + 1) + "." + "objective", true)));
						// safe flow chain before
						and.add(writeImplication(getOneNotObjectiveFlowChain(p, t, i, tokenFlow), getVarNr(p.getId() + "." + (i + 1) + "." + "notobjective", true)));
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

	protected void writeNoBadPlaces() throws IOException {
		if (!getWinningCondition().getBadPlaces().isEmpty()) {
			String[] nobadplaces = getNoBadPlaces();
			for (int i = 1; i <= pg.getN(); ++i) {
				bad[i] = createUniqueID();
				writer.write(bad[i] + " = " + nobadplaces[i]);
			}
		}
	}
	
	public String[] getNoBadPlaces() throws IOException {
		String[] nobadplaces = new String[pg.getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			or.clear();
			for (Place p : pn.getPlaces()) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					or.add(getVarNr(p.getId() + "." + i + "." + "notobjective", true));
				}
			}
			nobadplaces[i] = writeOr(or);
		}
		return nobadplaces;
	}
	
	protected void writeSafeFlowChainEnd() throws IOException {
		sFlCE = createUniqueID();
		writer.write(sFlCE + " = " + getSafeFlowChainEnd());
	}
	
	public String getSafeFlowChainEnd() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (Transition t : pn.getTransitions()) {
			for (Place p : t.getPreset()) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					Set<Place> outFlowChain = getOutgoingTokenFlow(t, p);
					if (outFlowChain.isEmpty()) {
						for (int i = 1; i < pg.getN() - 1; ++i) {
							and.clear();
							and.add(getVarNr(p.getId() + "." + i + "." + "notobjective", true));
							and.add(getOneTransition(t, i));
							int id = createUniqueID();
							writer.write(id + " = " + writeAnd(and));
							or.add(id);
						}
					}
				}
			}
		}
		return writeOr(or);
	}
	
	@Override
	public String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(additionalSystemName)) {
						int p_i_safe = getVarNr(p.getId() + "." + i + "." + "objective", true);
						int p_j_safe = getVarNr(p.getId() + "." + j + "." + "objective", true);
						and.add(writeImplication(p_i_safe, p_j_safe));
						and.add(writeImplication(p_j_safe, p_i_safe));
						int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "notobjective", true);
						int p_j_unsafe = getVarNr(p.getId() + "." + j + "." + "notobjective", true);
						and.add(writeImplication(p_i_unsafe, p_j_unsafe));
						and.add(writeImplication(p_j_unsafe, p_i_unsafe));
						int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
						int p_j_empty = getVarNr(p.getId() + "." + j + "." + "empty", true);
						and.add(writeImplication(p_i_empty, p_j_empty));
						and.add(writeImplication(p_j_empty, p_i_empty));
					}
				}
				int id = createUniqueID();
				writer.write(id + " = or(" + sFlCE + "," + bad[j] + ")" + QBFSolver.linebreak);
				and.add(id);
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
		}
		return writeOr(or);
	}
	
	protected void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			and.add(dlt[i]);
			and.add(det[i]);
			int id = createUniqueID();
			writer.write(id + " = or(" + sFlCE + "," + bad[i] + ")" + QBFSolver.linebreak);
			and.add(writeImplication(term[i], id));
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));
		}
		and.clear();
		int n = pg.getN();
		and.add(dlt[n]);
		and.add(det[n]);
		win[n] = createUniqueID();
		writer.write(win[n] + " = " + writeAnd(and));
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
		writeNoBadPlaces();
		writeTerminating();
		writeDeterministic();
		writeSafeFlowChainEnd();
		writeLoop();
		writeDeadlocksterm();
		writeWinning();

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

		int wnandLoop = createUniqueID();
		Set<Integer> wnandLoopSet = new HashSet<>();
		wnandLoopSet.add(l);
		wnandLoopSet.add(win[pg.getN()]);
		writer.write(wnandLoop + " = " + writeAnd(wnandLoopSet));

		seqImpliesWin[pg.getN()] = createUniqueID();
		writer.write(seqImpliesWin[pg.getN()] + " = " + "or(-" + seq[pg.getN()] + "," + wnandLoop + ")" + QBFSolver.linebreak);
		phi.add(seqImpliesWin[pg.getN()]);
		
		// use valid()
		int number = createUniqueID();
		writer.write(number + " = " + writeAnd(phi));
		writer.write("1 = or(-" + valid() + "," + number + ")" + QBFSolver.linebreak);
		
		// dont use valid()
		//writer.write("1 = " + writeAnd(phi));
		
		writer.close();

		// Total number of gates is only calculated during encoding and added to the file afterwards
		if (variablesCounter < 999999999) { // added 9 blanks as more than 999.999.999 variables wont be solvable
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			for (int i = 0; i < 10; ++i) { // read "#QCIR-G14 "
				raf.readByte();
			}
			String counter_str = Integer.toString(variablesCounter - 1); // has NEXT usable counter in it
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
