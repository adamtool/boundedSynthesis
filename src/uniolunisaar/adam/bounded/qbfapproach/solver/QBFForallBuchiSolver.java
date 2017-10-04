package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.winningconditions.Buchi;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.tools.AdamProperties;

public class QBFForallBuchiSolver extends QBFFlowChainSolver<Buchi> {

	private int bl; // buchi loop
	
	public QBFForallBuchiSolver(QBFPetriGame game, Buchi winCon, QBFSolverOptions options) {
		super(game, winCon, options);
	}
	
	@Override
	public String getInitial() {
		Marking initialMarking = pg.getNet().getInitialMarking();
		Set<Integer> initial = new HashSet<>();
		for (Place p : pn.getPlaces()) {
			if (initialMarking.getToken(p).getValue() == 1) {
				if (getWinningCondition().getBuchiPlaces().contains(p)) {
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
				if (getWinningCondition().getBuchiPlaces().contains(p)) {
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
	protected void writeLoop() throws IOException {
		String loop = getBuchiLoop();
		bl = createUniqueID();
		writer.write(bl + " = " + loop);
	}

	public String getBuchiLoop() throws IOException {
		Set<Integer> outerAnd = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		Set<Integer> innerAnd = new HashSet<>();
		
		for (Place p : pn.getPlaces()) {
			outerAnd.add(-getVarNr(p.getId() + "." + pg.getN() + "." + "safe", true));
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
					break;
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
				innerAnd.add(-getVarNr(p.getId() + "." + i + "." + "safe", true));
				
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
	protected boolean exWinStrat() {
		game = pg.copy("originalGame");
		game_winCon = new Safety();
		game_winCon.buffer(game);
		
		ForNonDeterministicUnfolder unfolder = new ForNonDeterministicUnfolder(pg, null); // null forces unfolder to use b as bound for every place
		try {
			unfolder.prepareUnfolding();
		} catch (UnboundedException | FileNotFoundException | NetNotSafeException | NoSuitableDistributionFoundException e) {
			System.out.println("Error: The bounded unfolding of the game failed.");
			e.printStackTrace();
		}
		// Adding the newly unfolded places to the set of buchi places
		getWinningCondition().buffer(pg);
		
		unfolding = pg.copy("unfolding");
		unfolding_winCon = new Safety();
		unfolding_winCon.buffer(unfolding);

		// These variables depend on the number of transitions and can only be initialized after the unfolding
		transitions = pn.getTransitions().toArray(new Transition[0]);
		flowSubFormulas = new int[pg.getN() * pn.getTransitions().size()];
		deadlockSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];
		
		oneTransitionFormulas = new int[pn.getTransitions().size()][pg.getN() + 1];
		for (int i = 0; i < transitions.length; ++i) {
			transitionKeys.put(transitions[i], i);
		}
		
		int exitcode = -1;
		try {
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
			int index_for_non_det_unfolding_info = enumerateStratForNonDetUnfold(unfolder.systemHasToDecideForAtLeastOne);
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
			// generating qcir benchmarks
			// FileUtils.copyFile(file, new File(pg.getNet().getName() + ".qcir"));

			ProcessBuilder pb = null;
			// Run solver on problem
			System.out.println("You are using " + System.getProperty("os.name") + ".");
			String os = System.getProperty("os.name");
			if (os.startsWith("Mac")) {
				System.out.println("Your operation system is supported.");
				pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_mac", "--partial-assignment", file.getAbsolutePath());
			} else if (os.startsWith("Linux")) {
				System.out.println("Your operation system is supported.");
				pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_unix", "--partial-assignment", file.getAbsolutePath());
			} else {
				System.out.println("Your operation system is not supported.");
				return false;
			}
			System.out.println("A temporary file is saved to \"" + file.getAbsolutePath() + "\".");

			Process pr = pb.start();
			// Read caqe's output
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line_read;
			outputQBFsolver = "";
			while ((line_read = inputReader.readLine()) != null) {
				outputQBFsolver += line_read + "\n";
			}

			exitcode = pr.waitFor();
			inputReader.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		// Storing results
		if (exitcode == 20) {
			solvable = true;
			sat = false;
			error = false;
			System.out.println("UNSAT ");
			return false;
		} else if (exitcode == 10) {
			solvable = true;
			sat = true;
			error = false;
			System.out.println("SAT");
			return true;
		} else {
			System.out.println("QCIR ERROR with FULL output:" + outputQBFsolver);
			solvable = false;
			sat = null;
			error = true;
			return false;
		}
	}
}
