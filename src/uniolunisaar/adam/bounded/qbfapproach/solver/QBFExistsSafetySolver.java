package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.tools.AdamProperties;

public class QBFExistsSafetySolver extends QBFFlowChainSolver<Safety> {
	
	private int[] bad;
	
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
	
	protected void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			and.clear();
			if (bad[i] != 0) {
				and.add(bad[i]);
			}
			if (dlt[i] != 0) {
				and.add(dlt[i]);
			}
			if (det[i] != 0) {
				and.add(det[i]);
			}
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));
		}
	}
	
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

	@Override
	protected boolean exWinStrat() {
		int exitcode = -1;
		try {
			writeQCIR();

			// This line was used to create benchmarks:
			if (QBFSolver.debug) {
				FileUtils.copyFile(file, new File(pn.getName() + ".qcir"));
			}

			ProcessBuilder pb = null;
			// Run solver on problem
			String os = System.getProperty("os.name");
			
			if (os.startsWith("Mac")) {
				pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_mac", "--partial-assignment", file.getAbsolutePath());
			} else if (os.startsWith("Linux")) {
				pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_unix", "--partial-assignment", file.getAbsolutePath());
			} else {
				System.out.println("You are using " + os + ".");
				System.out.println("Your operation system is not supported.");
				return false;
			}
			if (QBFSolver.debug) {
				System.out.println("A temporary file is saved to \"" + file.getAbsolutePath() + "\".");
			}

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
