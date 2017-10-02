package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.winningconditions.Safety;

public class QBFExistsSafetySolver extends QBFSafetySolver {

	public QBFExistsSafetySolver(PetriNet net, Safety win, QBFSolverOptions so) throws UnboundedPGException {
		super(net, win, so);
	}
	
	@Override
	protected void addForall() throws IOException {
		Set<Integer> forall = new HashSet<>();
		int id;
		for (Place p : pg.getNet().getPlaces()) {
			for (int i = 1; i <= pg.getN(); ++i) {
				id = createVariable(p.getId() + "." + i + "." + true);
				forall.add(id);
				id = createVariable(p.getId() + "." + i + "." + false);
				forall.add(id);
			}
		}
		writer.write(writeForall(forall));
		writer.write("output(1)" + QBFSolver.linebreak); // 1 = \phi
		makeThreeValuedLogic();
	}
	
	protected void makeThreeValuedLogic() throws IOException {
		int top;
		int bot;
		int id;
		for (Place p : pg.getNet().getPlaces()) {
			for (int i = 1; i <= pg.getN(); ++i) {
				top = getVarNr(p.getId() + "." + i + "." + true, true);
				bot = getVarNr(p.getId() + "." + i + "." + false, true);
				
				id = createVariable(p.getId() + "." + i + "." + "safe");
				writer.write(id + " = and(" + top + "," + "-" + bot + ")" + QBFSolver.linebreak);
				
				id = createVariable(p.getId() + "." + i + "." + "unsafe");
				writer.write(id + " = and(" + "-" + top + "," + bot + ")" + QBFSolver.linebreak);
				
				id = createVariable(p.getId() + "." + i + "." + "empty");
				writer.write(id + " = and(" + "-" + top + "," + "-" + bot + ")" + QBFSolver.linebreak);
			}
		}
	}
	
	@Override
	public String getInitial() {
		Marking initialMarking = pg.getNet().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : pn.getPlaces()) {
			if (initialMarking.getToken(p).getValue() == 1) {
				if (getWinningCondition().getBadPlaces().contains(p)) {
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
	
	protected int getOneUnsafeFlowChain(Place p, Transition t, int i) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("oneUNSAFEFlowChain" + p.getId() + "." + t.getId() + "." + i);
		if (result.getFirst()) {
			Set<Integer> or = new HashSet<>();
			for (Pair<Place, Place> pair : pg.getFl().get(t)) {
				if (pair.getSecond().equals(p)) {
					or.add(getVarNr(pair.getFirst().getId() + "." + i + "." + "unsafe", true));
				}
			}
			writer.write(result.getSecond() + " = " + writeOr(or));
		}
		return result.getSecond();
	}
	
	protected int getAllSafeFlowChain(Place p, Transition t, int i) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("allSAFEFlowChain" + p.getId() + "." + t.getId() + "." + i);
		if (result.getFirst()) {
			Set<Integer> and = new HashSet<>();
			for (Pair<Place, Place> pair : pg.getFl().get(t)) {
				if (pair.getSecond().equals(p)) {
					and.add(getVarNr(pair.getFirst().getId() + "." + i + "." + "safe", true));
				}
			}
			writer.write(result.getSecond() + " = " + writeAnd(and));
		}
		return result.getSecond();
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
				if (getWinningCondition().getBadPlaces().contains(p)) {
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
	
	@Override
	protected void writeDeadlockSubFormulas(int s, int e) throws IOException {
		Transition t;
		Set<Integer> or = new HashSet<>();
		int number;
		int strat;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				t = transitions[j];
				or.clear();
				for (Place p : t.getPreset()) {
					or.add(getVarNr(p.getId() + "." + i + "." + "empty", true)); // "p.i.empty"
					strat = addSysStrategy(p, t);
					if (strat != 0) {
						or.add(-strat);
					}
				}
				number = createUniqueID();
				writer.write(number + " = " + writeOr(or));
				deadlockSubFormulas[pn.getTransitions().size() * (i - 1) + j] = number;
			}
		}
	}
	
	@Override
	protected void writeTerminatingSubFormulas(int s, int e) throws IOException {
		Set<Integer> or = new HashSet<>();
		Set<Place> pre;
		Transition t;
		int key;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				t = transitions[j];
				pre = t.getPreset();
				or.clear();
				for (Place p : pre) {
					or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
				}
				key = createUniqueID();
				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[pn.getTransitions().size() * (i - 1) + j] = key;
			}
		}
	}
	
	@Override
	protected int writeOneMissingPre(Transition t1, Transition t2, int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		int strat;
		for (Place p : t1.getPreset()) {
			or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
			strat = addSysStrategy(p, t1);
			if (strat != 0)
				or.add(-strat);
		}
		for (Place p : t2.getPreset()) {
			or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
			strat = addSysStrategy(p, t2);
			if (strat != 0)
				or.add(-strat);
		}

		int number = createUniqueID();
		writer.write(number + " = " + writeOr(or));
		return number;
	}
	
	@Override
	public String[] getNoBadPlaces() throws IOException {
		String[] nobadplaces = new String[pg.getN() + 1];
		Set<Integer> or = new HashSet<>();
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			or.clear();
			for (Place p : pn.getPlaces()) {
				or.add(getVarNr(p.getId() + "." + i + "." + "safe", true));
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
							and.clear();
							and.add(getVarNr(p.getId() + "." + j + "." + "safe", true));
							and.add(getOneTransition(t, j));
							int id = createUniqueID();
							writer.write(id + " = " + writeAnd(and));
							or.add(id);
						}
					}
				}
			}
			nobadplaces[i] = writeOr(or);
		}
		return nobadplaces;
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
						int p_i_safe = getVarNr(p.getId() + "." + i + "." + "safe", true);
						int p_j_safe = getVarNr(p.getId() + "." + j + "." + "safe", true);
						and.add(writeImplication(p_i_safe, p_j_safe));
						and.add(writeImplication(p_j_safe, p_i_safe));
						int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "unsafe", true);
						int p_j_unsafe = getVarNr(p.getId() + "." + j + "." + "unsafe", true);
						and.add(writeImplication(p_i_unsafe, p_j_unsafe));
						and.add(writeImplication(p_j_unsafe, p_i_unsafe));
						int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
						int p_j_empty = getVarNr(p.getId() + "." + j + "." + "empty", true);
						and.add(writeImplication(p_i_empty, p_j_empty));
						and.add(writeImplication(p_j_empty, p_i_empty));
					}
				}
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
		}
		return writeOr(or);
	}
	
	protected int valid() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			for (Place p : pn.getPlaces()) {
				and.clear();
				and.add(getVarNr(p.getId() + "." + i + "." + true, true));
				and.add(getVarNr(p.getId() + "." + i + "." + false, true));
				int id = createUniqueID();
				writer.write(id + " = " + writeAnd(and));
				or.add(id);
			}
		}
		int returnValue = createUniqueID();
		writer.write(returnValue + " = " + writeOr(or));
		return returnValue;
	}
	
	// TODO still unclear whether I need to explicitly remove 4th value
	@Override
	public void writeQCIR() throws IOException {
		game = pg.copy("originalGame");
		game_winCon = new Safety();
		game_winCon.buffer(game);

		ForNonDeterministicUnfolder unfolder = new ForNonDeterministicUnfolder(pg, null); // null forces unfolder to use b as bound for every place
		try {
			unfolder.prepareUnfolding();
		} catch (UnboundedException | FileNotFoundException | NetNotSafeException | NoSuitableDistributionFoundException e1) {
			System.out.println("Error: The bounded unfolding of the game failed.");
		}
		// Adding the newly unfolded places to the set of bad places
		getWinningCondition().buffer(pg);

		unfolding = pg.copy("unfolding");
		unfolding_winCon = new Safety();
		unfolding_winCon.buffer(unfolding);

		seqImpliesWin = new int[pg.getN() + 1];
		transitions = pn.getTransitions().toArray(new Transition[0]);
		flowSubFormulas = new int[pg.getN() * pn.getTransitions().size()];
		deadlockSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];
		terminatingSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];

		oneTransitionFormulas = new int[pn.getTransitions().size()][pg.getN() + 1];
		for (int i = 0; i < transitions.length; ++i) {
			transitionKeys.put(transitions[i], i);
		}
		
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
		writeLoop();
		writeDeadlocksterm();
		writeWinning();

		Set<Integer> phi = new HashSet<>();
		// When unfolding non-deterministically we add system places to
		// ensure deterministic decision.
		// It is required that these decide for exactly one transition which
		// is directly encoded into the problem.
		int index_for_non_det_unfolding_info = enumerateStratForNonDetUnfold(unfolder.systemHasToDecideForAtLeastOne);
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
		int number = createUniqueID();
		writer.write(number + " = " + writeAnd(phi));

		writer.write("1 = or(-" + valid() + "," + number + ")" + QBFSolver.linebreak);
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
	}
}
