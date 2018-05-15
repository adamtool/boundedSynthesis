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
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QCIRconsistency;
import uniolunisaar.adam.ds.exceptions.CouldNotFindSuitableWinningConditionException;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.ds.winningconditions.Buchi;

public class QBFForallBuchiSolver extends QBFFlowChainSolver<Buchi> {

	private int[] noFlowChainEnded;
	private int[] goodSimultan;
	private int[] buchiPlaces;
	private int[] reset;
	private int[] resetChoice;
	private int bl; // buchi loop

	public QBFForallBuchiSolver(QBFPetriGame game, Buchi winCon, QBFSolverOptions options) throws BoundedParameterMissingException, CouldNotFindSuitableWinningConditionException, ParseException {
		super(game, winCon, options);
		setTokenFlow();
		noFlowChainEnded = new int[pg.getN() + 1];
		goodSimultan = new int[pg.getN() + 1];
		buchiPlaces = new int[pg.getN() + 1];
		reset = new int[pg.getN() + 1];
		resetChoice = new int[pg.getN() + 1];
	}

	@Override
	protected String getInitial() {
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
	protected int getOneTransition(Transition t, int i) throws IOException {
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
				// Buchi place reached
				if (AdamExtensions.isBuchi(p)) {
					and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "objective", true));
				} else {
					Set<Place> tokenFlow = getIncomingTokenFlow(t, p);
					if (tokenFlow.isEmpty()) {
						and.add(getVarNr(p.getId() + "." + (i + 1) + "." + "notobjective", true));
					} else {
						// all flow chains reached before
						and.add(writeImplication(getAllObjectiveFlowChain(p, t, i, tokenFlow), getVarNr(p.getId() + "." + (i + 1) + "." + "objective", true)));
						// one flow chain did not reach before
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
		} else {
			return oneTransitionFormulas[transitionKeys.get(t)][i];
		}
	}

	protected void writeResetChoice() throws IOException {
		String[] resetArr = getResetChoice();
		for (int i = 1; i < pg.getN(); ++i) {
			resetChoice[i] = createUniqueID();
			writer.write(resetChoice[i] + " = " + resetArr[i]);
		}
	}

	protected String[] getResetChoice() throws IOException {
		String[] reset = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			for (Place p : pn.getPlaces()) {
				int id = createUniqueID();
				writer.write(id + " = or(" + getVarNr(p.getId() + "." + i + "." + "empty", true) + "," + getVarNr(p.getId() + "." + i + "." + "objective", true) + ")" + QBFSolver.linebreak);
				and.add(id);
			}
			reset[i] = writeAnd(and);
		}
		return reset;
	}

	protected void writeReset() throws IOException {
		String[] resetArr = getReset();
		for (int i = 1; i < pg.getN(); ++i) {
			reset[i] = createUniqueID();
			writer.write(reset[i] + " = " + resetArr[i]);
		}
	}

	protected String[] getReset() throws IOException {
		String[] reset = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			for (Place p : pn.getPlaces()) {
				and.add(writeImplication(getVarNr(p.getId() + "." + i + "." + "empty", true), getVarNr(p.getId() + "." + (i + 1) + "." + "empty", true)));

				and.add(writeImplication(getVarNr(p.getId() + "." + i + "." + "objective", true), getVarNr(p.getId() + "." + (i + 1) + "." + "notobjective", true)));
			}
			reset[i] = writeAnd(and);
		}
		return reset;
	}

	protected String[] getFlow() throws IOException {
		String[] flow = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			or.clear();
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				or.add(getOneTransition(transitions[j], i));
			}
			int normalFlow = createUniqueID();
			writer.write(normalFlow + " = " + writeOr(or));

			and.add(writeImplication(-resetChoice[i], normalFlow));
			and.add(writeImplication(resetChoice[i], reset[i]));

			flow[i] = writeAnd(and);
		}
		return flow;
	}

	protected void writeBuchiPlaces() throws IOException {
		String[] buchi = getBuchiPlaces();
		for (int i = 1; i < pg.getN(); ++i) {
			buchiPlaces[i] = createUniqueID();
			writer.write(buchiPlaces[i] + " = " + buchi[i]);
		}
	}

	protected String[] getBuchiPlaces() throws IOException {
		String[] buchi = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			for (Place p : pn.getPlaces()) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					and.add(-getVarNr(p.getId() + "." + i + "." + "notobjective", true));
				}
			}
			buchi[i] = writeAnd(and);
		}
		return buchi;
	}

	protected void writeNoFlowChainEnded() throws IOException {
		String[] noflended = getNoFlowChainEnded();
		for (int i = 1; i < pg.getN(); ++i) {
			noFlowChainEnded[i] = createUniqueID();
			writer.write(noFlowChainEnded[i] + " = " + noflended[i]);
		}
	}

	protected String[] getNoFlowChainEnded() throws IOException {
		String[] unreachEnded = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			for (Transition t : pn.getTransitions()) {
				for (Place p : t.getPreset()) {
					if (!p.getId().startsWith(additionalSystemName)) {
						if (getOutgoingTokenFlow(p, t).isEmpty()) {
							and.add(-getOneTransition(t, i));
						}
					}
				}
			}
			unreachEnded[i] = writeAnd(and);
		}
		return unreachEnded;
	}

	@Override
	protected void writeLoop() throws IOException {
		String loop = getBuchiLoop();
		bl = createUniqueID();
		writer.write(bl + " = " + loop);
	}

	protected String getBuchiLoop() throws IOException {
		Set<Integer> or = new HashSet<>();
		Set<Integer> innerOr = new HashSet<>();
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				and.clear();
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
				if (getTransitionCreatingTokenFlow().isEmpty()) { // TODO redundancy weil goodSimultan statt goodPlaces wegen additional system places
					innerOr.clear();
					for (int k = i; k < j; ++k) {
						innerOr.add(buchiPlaces[k]);
					}
					int id = createUniqueID();
					writer.write(id + " = " + writeOr(innerOr));
					and.add(id);
				} else {
					innerOr.clear();
					for (int k = i + 1; k <= j; ++k) {
						innerOr.add(goodSimultan[k]);
					}
					int id = createUniqueID();
					writer.write(id + " = " + writeOr(innerOr));
					and.add(id);
				}

				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
		}
		return writeOr(or);
	}

	protected void writeGoodSimultan() throws IOException {
		String[] goodSimu = getGoodSimultan();
		for (int i = 2; i <= pg.getN(); ++i) {
			goodSimultan[i] = createUniqueID();
			writer.write(goodSimultan[i] + " = " + goodSimu[i]);
		}
	}

	protected String[] getGoodSimultan() throws IOException {
		String[] goodSimultan = new String[pg.getN() + 1];
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 2; i <= pg.getN(); ++i) {
			and.clear();
			for (Place p : pn.getPlaces()) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					or.clear();
					or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
					or.add(getVarNr(p.getId() + "." + i + "." + "objective", true));
					for (Transition t : getTransitionCreatingTokenFlow()) {
						if (t.getPostset().contains(p) && getIncomingTokenFlow(t, p).isEmpty()) {
							or.add(getOneTransition(t, i - 1));
						}
					}
					int id = createUniqueID();
					writer.write(id + " = " + writeOr(or));
					and.add(id);
				}
			}
			goodSimultan[i] = writeAnd(and);
		}
		return goodSimultan;
	}

	protected void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			and.clear();
			and.add(-dl[i]);
			and.add(det[i]);
			if (!getTransitionFinishingTokenFlow().isEmpty()) {
				and.add(noFlowChainEnded[i]);
			}
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));

		}
		and.clear();
		and.add(-dl[pg.getN()]);
		and.add(det[pg.getN()]);
		and.add(bl);
		win[pg.getN()] = createUniqueID();
		writer.write(win[pg.getN()] + " = " + writeAnd(and));
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
		writeReset();
		writeResetChoice();
		writeFlow();
		writeSequence();
		if (getTransitionCreatingTokenFlow().isEmpty()) {
			writeBuchiPlaces();
		}
		if (!getTransitionCreatingTokenFlow().isEmpty()) {
			writeGoodSimultan();
		}
		if (!getTransitionFinishingTokenFlow().isEmpty()) {
			writeNoFlowChainEnded();
		}
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

		for (int i = 1; i <= pg.getN(); ++i) {
			seqImpliesWin[i] = createUniqueID();
			if (i < pg.getN()) {
				writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QBFSolver.linebreak);
			} else {
				writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + "," + u + ")" + QBFSolver.linebreak); // adding unfair
			}
			phi.add(seqImpliesWin[i]);
		}

		// use valid()
		// int number = createUniqueID();
		// writer.write(number + " = " + writeAnd(phi));
		// int valid = valid();
		// writer.write(createUniqueID() + " = or(-" + valid + "," + number + ")" + QBFSolver.linebreak);

		// dont use valid()
		writer.write(createUniqueID() + " = " + writeAnd(phi));

		writer.close();

		// Total number of gates is only calculated during encoding and added to the file afterwards

		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		for (int i = 0; i < 10; ++i) { // read "#QCIR-G14 "
			raf.readByte();
		}
		String counter_str = Integer.toString(variablesCounter - 1); // has NEXT usable counter in it
		char[] counter_char = counter_str.toCharArray();
		for (char c : counter_char) {
			raf.writeByte(c);
		}

		raf.readLine(); // Read remaining first line
		raf.readLine(); // Read exists line
		raf.readLine(); // Read forall line
		for (int i = 0; i < 7; ++i) { // read "output(" and thus overwrite "1)"
			raf.readByte();
		}
		counter_str += ")";
		counter_char = counter_str.toCharArray();
		for (char c : counter_char) {
			raf.writeByte(c);
		}

		raf.close();

		if (QBFSolver.debug) {
			FileUtils.copyFile(file, new File(pn.getName() + ".qcir"));
		}

		assert (QCIRconsistency.checkConsistency(file));
	}
}
