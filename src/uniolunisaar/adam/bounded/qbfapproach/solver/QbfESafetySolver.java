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
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QCIRconsistency;
import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;

public class QbfESafetySolver extends QbfFlowChainSolver<Safety> {

	private int[] bad;
	private int[] simultan;
	private int sFlCE;

	public QbfESafetySolver(PetriGame game, Safety winCon, QbfSolverOptions options) throws SolvingException {
		super(game, winCon, options);
		bad = new int[getSolvingObject().getN() + 1];
		simultan = new int[getSolvingObject().getN() + 1];
		setTokenFlow();
	}

	@Override
	protected String getInitial() {
		Marking initialMarking = getSolvingObject().getGame().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (initialMarking.getToken(p).getValue() == 1) {
				if (getSolvingObject().getGame().isBad(p)) {
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
				// bad place reached
				if (getSolvingObject().getGame().isBad(p)) {
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

			Set<Place> places = new HashSet<>(getSolvingObject().getGame().getPlaces());
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

	protected void writeNoBadPlaces() throws IOException {
		String[] nobadplaces = getNoBadPlaces();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			bad[i] = createUniqueID();
			writer.write(bad[i] + " = " + nobadplaces[i]);
		}
	}

	protected String[] getNoBadPlaces() throws IOException {
		String[] nobadplaces = new String[getSolvingObject().getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			or.clear();
			for (Place p : getSolvingObject().getGame().getPlaces()) {
				if (!p.getId().startsWith(QbfSolver.additionalSystemName)) {
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

	protected String getSafeFlowChainEnd() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (Transition t : getTransitionFinishingTokenFlow()) {
			for (Place p : t.getPreset()) {
				if (getOutgoingTokenFlow(p, t).isEmpty()) {
					for (int i = 1; i < getSolvingObject().getN() - 1; ++i) {
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
		return writeOr(or);
	}

	protected Set<Integer> getSimultaneousSpawnAndBad(Transition t, int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		for (Place post : t.getPostset()) {
			Set<Place> tokenFlow = getIncomingTokenFlow(t, post);
			if (!tokenFlow.isEmpty()) {
				if (!getSolvingObject().getWinCon().getBadPlaces().contains(post)) {
					or.add(getOneNotObjectiveFlowChain(post, t, i, tokenFlow));
				} else {
					or.add(-getVarNr(post.getId() + "." + (i + 1) + "." + "objective", true)); // TODO wie soll das gehen, wenn schlechter Platz erreicht wird, wird das bad?
				}
			}
		}
		return or;
	}

	protected String[] getNoSimultaneousSpawnAndBad() throws IOException {
		String[] result = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			and.clear();
			for (Transition t : getTransitionCreatingTokenFlow()) {
				Set<Integer> or = getSimultaneousSpawnAndBad(t, i);
				if (!or.isEmpty()) {
					or.add(-getOneTransition(t, i));
					int id = createUniqueID();
					writer.write(id + " = " + writeOr(or));
					and.add(id);
				}
			}
			result[i] = writeAnd(and);
		}
		return result;
	}

	protected void writeSimultaneousSpawnAndBad() throws IOException {
		String[] result = getNoSimultaneousSpawnAndBad();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			if (result[i].startsWith("and()")) {
				Pair<Boolean, Integer> pair = getVarNrWithResult("and()");
				if (pair.getFirst()) {
					writer.write(pair.getSecond() + " = and()" + QbfSolver.linebreak);
				}
				simultan[i] = pair.getSecond();

			} else {
				simultan[i] = createUniqueID();
				writer.write(simultan[i] + " = " + result[i]);
			}
		}
	}

	@Override
	protected String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		Set<Integer> innerOr = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (int j = i + 1; j <= getSolvingObject().getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
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
				innerOr.clear();
				if (!getTransitionFinishingTokenFlow().isEmpty()) {
					innerOr.add(sFlCE);
				}

				Set<Integer> innerAnd = new HashSet<>();
				for (int k = i; k <= j; ++k) {
					innerAnd.add(bad[k]); // TODO chains dieser Länge berechnen? und irgendwie aufzählen? NEW should solve this
				}
				int idd = createUniqueID();
				writer.write(idd + " = " + writeAnd(innerAnd));
				innerOr.add(idd);

				if (innerOr.size() > 0) {
					int id = createUniqueID();
					writer.write(id + " = " + writeOr(innerOr));
					and.add(id);
				}
				if (!getTransitionCreatingTokenFlow().isEmpty()) {
					for (int k = i; k < j; ++k) {
						if (sFlCE != 0) { // if there is the possibility of a safe flow chain ending then this can suffice for winning the game regardless of the behavior simultan
							and.add(writeImplication(-sFlCE, simultan[k]));
						} else {
							and.add(simultan[k]);
						}
					}
				}
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
		}
		return writeOr(or);
	}

	protected void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			and.clear();
			and.add(dlt[i]);
			and.add(det[i]);
			or.clear();
			if (!getTransitionFinishingTokenFlow().isEmpty()) {
				or.add(sFlCE);
			}
			or.add(bad[i]);

			int id = createUniqueID();
			writer.write(id + " = " + writeOr(or));
			and.add(writeImplication(term[i], id));
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));
		}
		and.clear();
		int n = getSolvingObject().getN();
		and.add(dlt[n]);
		and.add(det[n]);
		and.add(l);	 // slightly optimized in the sense that winning and loop are put together for n
		win[n] = createUniqueID();
		writer.write(win[n] + " = " + writeAnd(and));
	}

	@Override
	protected void writeQCIR() throws IOException {
		Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = unfoldPG();

		initializeVariablesForWriteQCIR();

		writer.write("#QCIR-G14          " + QbfSolver.linebreak); // spaces left to add variable count in the end
		addExists();
		addForall();

		writeInitial();
		writeDeadlock();
		writeFlow();
		writeSequence();
		writeNoBadPlaces();
		writeTerminating();
		writeDeterministic();
		if (!getTransitionCreatingTokenFlow().isEmpty()) {
			writeSimultaneousSpawnAndBad();
		}
		if (!getTransitionFinishingTokenFlow().isEmpty()) {
			writeSafeFlowChainEnd();
		}
		writeLoop();
		writeDeadlocksterm();
		writeWinning();
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

		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			seqImpliesWin[i] = createUniqueID();
			if (i < getSolvingObject().getN()) {
				writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QbfSolver.linebreak);
			} else {
				writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + "," + u + ")" + QbfSolver.linebreak);		// adding unfair
			}
			phi.add(seqImpliesWin[i]);
		}

		// use valid()
		int number = createUniqueID();
		writer.write(number + " = " + writeAnd(phi));
		int valid = valid();
		writer.write(createUniqueID() + " = or(-" + valid + "," + number + ")" + QbfSolver.linebreak);

		// dont use valid()
		// writer.write(createUniqueID() + " = " + writeAnd(phi));

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

		if (QbfSolver.debug) {
			FileUtils.copyFile(file, new File(getSolvingObject().getGame().getName() + ".qcir"));
		}

		assert (QCIRconsistency.checkConsistency(file));
	}
}
