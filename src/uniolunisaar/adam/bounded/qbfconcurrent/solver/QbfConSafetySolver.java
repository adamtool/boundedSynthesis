package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.FiniteDeterministicUnfolder;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.Unfolder;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.tools.AdamProperties;

public class QbfConSafetySolver extends QbfConSolver<Safety> {

	// variable to store keys of calculated components for later use
	private int[] bad;
	private int detenvlocal;
	private int detenv;
	private int detAdditionalSys;
	private int strongdet;

	public QbfConSafetySolver(PetriGame game, Safety winCon, QbfConSolverOptions so) throws SolvingException {
		super(game, winCon, so);
		fl = new int[getSolvingObject().getN() + 1];
		bad = new int[getSolvingObject().getN() + 1];
		term = new int[getSolvingObject().getN() + 1];
		det = new int[getSolvingObject().getN() + 1];
		dl = new int[getSolvingObject().getN() + 1];
		seq = new int[getSolvingObject().getN() + 1];
		dlt = new int[getSolvingObject().getN() + 1];
		win = new int[getSolvingObject().getN() + 1];
	}

	private void writeDetEnv() throws IOException {
		detenv = createUniqueID();
		if (!getSolvingObject().getGame().getEnvPlaces().isEmpty())
			writer.write(detenv + " = " + getDetEnv());
	}

	/**
	 * Strong determinism for additional System places, since nondeterminism can never be legal for additional system places
	 * @throws IOException
	 */
	private void writeStrongDet() throws IOException {
		boolean existAddSysPlaces = false;
		for (Place p : getSolvingObject().getGame().getPlaces()){
			if (p.getId().startsWith(QbfControl.additionalSystemName) && !getSolvingObject().getGame().getEnvPlaces().contains(p)){
				existAddSysPlaces = true;
				break;
			}
		}
		if (existAddSysPlaces) {
			detAdditionalSys = createUniqueID();
			writer.write(detAdditionalSys + " = " + getDetAdditionalSys());
		}
	}

	private void writeInitial() throws IOException {
		in = createUniqueID();
		writer.write(in + " = " + getInitial());
	}

	private void writeDeadlock() throws IOException {
		String[] deadlock = getDeadlock();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			dl[i] = createUniqueID();
			writer.write(dl[i] + " = " + deadlock[i]);
		}
	}

	private void writeFlow() throws IOException {
		String[] flow = getFlow();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			fl[i] = createUniqueID();
			writer.write(fl[i] + " = " + flow[i] + "#WriteFlow\n");
		}
	}

	private void writeSequence() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.clear();
			and.add(in);
			for (int j = 1; j <= i - 1; ++j) {
				// and.add(-dl[j]); // performance evaluation showed that leaving this out makes
				// program faster as it is redundant
				and.add(fl[j]);
			}
			seq[i] = createUniqueID();
			writer.write(seq[i] + " = " + writeAnd(and) + "#WriteSequence\n");
		}
	}

	private void writeNoBadMarking() throws IOException {
		if (!getSolvingObject().getWinCon().getBadPlaces().isEmpty()) {
			String[] nobadmarking = getNobadmarking();
			if (!getSolvingObject().getWinCon().getBadPlaces().isEmpty()) {
				for (int i = 1; i <= getSolvingObject().getN(); ++i) {
					bad[i] = createUniqueID();
					writer.write(bad[i] + " = " + nobadmarking[i] + "\n # bad marking\n");
				}
			}
		}
	}

	public String[] getNobadmarking() {
		String[] nobadmarking = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.clear();
			for (Place p : getSolvingObject().getWinCon().getBadPlaces()) {
				and.add(-getVarNr(p.getId() + "." + i, true));
			}
			nobadmarking[i] = writeAnd(and);
		}
		return nobadmarking;
	}

	private void writeTerminating() throws IOException {
		String[] terminating = new String[getSolvingObject().getN() + 1];
		terminating = getTerminating();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			term[i] = createUniqueID();
			writer.write(term[i] + " = " + terminating[i]);
		}
	}

	private void writeDeterministic() throws IOException {
		if (QbfControl.deterministicStrat) {
			String[] deterministic = getDeterministic();
			for (int i = 1; i <= getSolvingObject().getN(); ++i) {
				det[i] = createUniqueID();
				writer.write(det[i] + " = " + deterministic[i]);
			}
		}
	}

	private void writeLoop() throws IOException {
		String loop = getLoopIJ();
		l = createUniqueID();
		writer.write(l + " = " + loop);
	}

	private void writeDeadlocksterm() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			or.clear();
			or.add(-dl[i]);
			or.add(term[i]);
			dlt[i] = createUniqueID();
			writer.write(dlt[i] + " = " + writeOr(or));
		}
	}

	private void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
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

	public void addExists() throws IOException {
		Set<Integer> exists = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (!getSolvingObject().getGame().getEnvPlaces().contains(p)) {
				if (p.getId().startsWith(QbfControl.additionalSystemName)) {
					for (Transition t : p.getPostset()) {
						int number = createVariable(p.getId() + ".." + t.getId());
						exists.add(number);
						// System.out.println(number + " = " + p.getId() + ".." + t.getId());
						exists_transitions.put(number, p.getId() + ".." + t.getId());
					}
				} else {
					Set<String> truncatedIDs = new HashSet<>();
					for (Transition t : p.getPostset()) {
						String truncatedID = getTruncatedId(t.getId());
						if (!truncatedIDs.contains(truncatedID)) {
							truncatedIDs.add(truncatedID);
							int number = createVariable(p.getId() + ".." + truncatedID);
							exists.add(number);
							// System.out.println(number + " = " + p.getId() + ".." + truncatedID);
							exists_transitions.put(number, p.getId() + ".." + truncatedID);
						}
					}
				}
			}
		}
		writer.write(writeExists(exists));
	}


	protected void addForall() throws IOException {
		Set<Integer> forall = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = 1; i <= getSolvingObject().getN(); ++i) {
				int number = createVariable(p.getId() + "." + i);
				// System.out.println(p.getId() + "." + i + " : number: " +
				// number);
				forall.add(number);
				// System.out.println(number + " = " + p.getId() + "." + i);
			}
		}
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
				Set<String> truncatedIDs = new HashSet<>();
				for (Transition t : p.getPostset()) {
					String truncatedID = getTruncatedId(t.getId());
					if (!truncatedIDs.contains(truncatedID)) {
						truncatedIDs.add(truncatedID);
						for (int i = 1; i <= 1; ++i) { //getSolvingObject().getN() //TODO
							int number = createVariable(p.getId() + "**" + truncatedID + "**" + i);
							forall.add(number);
						}
					}
				}

			}
		}
		//Add environment stalling for all transtition including env transitions
		//Truncated transitions behave equally
		Set<String> testSet = new HashSet<>();
		for (Transition t : getSolvingObject().getGame().getTransitions()){
			String truncatedID = getTruncatedId(t.getId());
			if (!testSet.contains(truncatedID)){
				testSet.add(truncatedID);
				forall.add(createVariable(truncatedID + "**" + "stall"));//Make this pretty again
			}
		}
		if (!forall.isEmpty()) {
			writer.write("#Forall env\n");
			writer.write(writeForall(forall));
		}
		// System.out.println(forall);

	}

	protected void addForallEnvStall() throws IOException{
		Set<Integer> forall = new HashSet<>();
		Set<String> testSet = new HashSet<>();

		for (Transition t : getSolvingObject().getGame().getTransitions()){
			String truncatedID = getTruncatedId(t.getId());
			if (!testSet.contains(truncatedID)){
				testSet.add(truncatedID);
				forall.add(createVariable(truncatedID + "**" + "stall"));//Make this pretty again
			}
		}
	}

	// Additional information from nondeterministic unfolding is utilized:
	// for each place a set of transitions is given of which at least one has to
	// be activated by the strategy to be deadlock-avoiding
	public int enumerateStratForNonDetUnfold(Map<Place, Set<Transition>> additionalInfoForNonDetUnfl) throws IOException {
		if (additionalInfoForNonDetUnfl.keySet().isEmpty()) {
			return -1;
		}
		int index = createUniqueID();
		Set<Integer> and = new HashSet<>();
		Set<Transition> transitions;
		int or_index;
		Set<Integer> or = new HashSet<>();
		for (Place p : additionalInfoForNonDetUnfl.keySet()) {
			transitions = additionalInfoForNonDetUnfl.get(p);
			or_index = createUniqueID();
			or.clear();
			for (Transition t : transitions) {
				or.add(getVarNr(p.getId() + ".." + t.getId(), true));
			}
			writer.write(or_index + " = " + writeOr(or));
			and.add(or_index);
		}
		writer.write(index + " = " + writeAnd(and));
		return index;
	}

	@Override
	protected boolean exWinStrat() {
		originalGame = new PetriGame(getSolvingObject().getGame());

		Unfolder unfolder = null;
		if (QbfControl.rebuildingUnfolder) {
			try {
				unfolder = new FiniteDeterministicUnfolder(getSolvingObject(), null);
			} catch (NotSupportedGameException e2) {
				e2.printStackTrace();
			}
		} else {
			unfolder = new ForNonDeterministicUnfolder(getSolvingObject(), null); // null forces unfolder to use b as bound for every place
		}

		try {
			unfolder.prepareUnfolding();
		} catch (SolvingException | UnboundedException | FileNotFoundException e1) {
			System.out.println("Error: The bounded unfolding of the game failed.");
			e1.printStackTrace();
		}

		unfolding = new PetriGame(getSolvingObject().getGame());

		if (QbfControl.rebuildingUnfolder) {
			Set<Place> oldBad = new HashSet<>(getSolvingObject().getWinCon().getBadPlaces());
			getWinningCondition().buffer(getSolvingObject().getGame());
			for (Place old : oldBad) {
				getSolvingObject().getWinCon().getBadPlaces().remove(old);
			}
		}

		seqImpliesWin = new int[getSolvingObject().getN() + 1];
		transitions = getSolvingObject().getGame().getTransitions().toArray(new Transition[0]);
		flowSubFormulas = new int[getSolvingObject().getN() * getSolvingObject().getGame().getTransitions().size()];
		deadlockSubFormulas = new int[(getSolvingObject().getN() + 1)
				* getSolvingObject().getGame().getTransitions().size()];
		terminatingSubFormulas = new int[(getSolvingObject().getN() + 1)
				* getSolvingObject().getGame().getTransitions().size()];
		int exitcode = -1;
		try {
			writer.write("#QCIR-G14          " + QbfControl.linebreak); // spaces left to add variable count in the
																			// end
			addExists();
			addForall();
			writer.write("output(1)" + QbfControl.linebreak); // 1 = \phi
			writeDetEnv();
			writeStrongDet(); //strong determinism for additional system places enforced
			writeInitial();
			writer.write("# End of Initial\n");
			writeDeadlock();
			writer.write("# End of Deadlock\n");
			writeFlow();
			writer.write("# End of Flow\n");
			writeSequence();
			writer.write("# start of no bad marking\n");
			writeNoBadMarking();
			writer.write("# End of No Bad Marking\n");
			writeTerminating();
			writer.write("# End of terminating\n");
			writeDeterministic();
			writer.write("# End of Deterministic\n");
			writeLoop();

			writer.write("# End of Loop\n");
			writeDeadlocksterm();

			writer.write("# End of Deadlocksterm\n");
			writeWinning();

			writer.write("# End of Winning\n");

			Set<Integer> phi = new HashSet<>();
			// When unfolding non-deterministically we add system places to
			// ensure deterministic decision.
			// It is required that these decide for exactly one transition which
			// is directly encoded into the problem.
			int index_for_non_det_unfolding_info = enumerateStratForNonDetUnfold(unfolder.systemHasToDecideForAtLeastOne);

			if (index_for_non_det_unfolding_info != -1) {
				phi.add(index_for_non_det_unfolding_info);
			}

			for (int i = 1; i <= getSolvingObject().getN() - 1; ++i) { // slightly optimized in the sense that winning and loop are put together for n
				seqImpliesWin[i] = createUniqueID();
				writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QbfControl.linebreak);
				phi.add(seqImpliesWin[i]);
			}
			// phi.add(detenv); // Add new detenv encoding
			int wnandLoop = createUniqueID();
			Set<Integer> wnandLoopSet = new HashSet<>();
			wnandLoopSet.add(l);
			wnandLoopSet.add(win[getSolvingObject().getN()]);
			writer.write(wnandLoop + " = " + writeAnd(wnandLoopSet));
			seqImpliesWin[getSolvingObject().getN()] = createUniqueID();
			writer.write(seqImpliesWin[getSolvingObject().getN()] + " = " + "or(-" + seq[getSolvingObject().getN()]
					+ "," + wnandLoop + ")" + QbfControl.linebreak);
			phi.add(seqImpliesWin[getSolvingObject().getN()]);
			int phi_number = createUniqueID();
			writer.write(phi_number + " = " + writeAnd(phi));
			if (!getSolvingObject().getGame().getEnvPlaces().isEmpty() && (detAdditionalSys != 0)) //with strongdet
				writer.write("1 = " + "and(" + detAdditionalSys + "," + writeImplication(detenv, phi_number) + ")");
			else if (!getSolvingObject().getGame().getEnvPlaces().isEmpty()) //without strongdet (no additional system places)
				writer.write("1 = " + "and(" + writeImplication(detenv, phi_number) + ")");
			else //no env places
				writer.write("1 = and(" + phi_number + ")");
			writer.close();

			// Total number of gates is only calculated during encoding and added to the
			// file afterwards
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
			if (QbfControl.debug) {
				FileUtils.copyFile(file, new File(getSolvingObject().getGame().getName() + ".qcir"));
			}

			ProcessBuilder pb = null;
			// Run solver on problem

			String os = System.getProperty("os.name");
			if (os.startsWith("Mac")) {
				pb = new ProcessBuilder(AdamProperties.LIBRARY_FOLDER + File.separator + QbfControl.solver + "_mac", "--partial-assignment"/* , "--preprocessing", "0" */, file.getAbsolutePath());
			} else if (os.startsWith("Linux")) {
				if (QbfControl.edacc) {
                	// for use with EDACC
					pb = new ProcessBuilder("./" + QbfControl.solver + "_unix", "--partial-assignment", file.getAbsolutePath());
				} else {
                	// for use with WEBSITE
					pb = new ProcessBuilder(AdamProperties.LIBRARY_FOLDER + File.separator + QbfControl.solver + "_unix", "--partial-assignment", file.getAbsolutePath());
				}
			} else {
				System.out.println("You are using " + os + ".");
				System.out.println("Your operation system is not supported.");
				return false;
			}
			if (QbfControl.debug) {
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
			System.out.println("UNSAT ");
			return false;
		} else if (exitcode == 10) {
			System.out.println("SAT");
			return true;
		} else {
			System.out.println("QCIR ERROR with FULL output:" + outputQBFsolver);
			return false;
		}
	}

	@Override
	protected PetriGame calculateStrategy() throws NoStrategyExistentException {
		if (existsWinningStrategy()) {
			System.out.println("Calculate strategy");
			for (String outputCAQE_line : outputQBFsolver.split("\n")) {
				if (outputCAQE_line.startsWith("V")) {
					String[] parts = outputCAQE_line.split(" ");
					for (int i = 0; i < parts.length; ++i) {
						if (!parts[i].equals("V")) {
							int num = Integer.parseInt(parts[i]);
							if (num > 0) {
								//System.out.println("ALLOW " + num);
							} else if (num < 0) {
								//System.out.println("DISALLOW " + num * (-1));
								String remove = exists_transitions.get(num * (-1));
								int in = remove.indexOf("..");
								if (in != -1) { // CTL has exists variables for path EF which mean not remove
									String place = remove.substring(0, in);
									String transition = remove.substring(in + 2, remove.length());
									if (place.startsWith(QbfControl.additionalSystemName)) {
										// additional system place exactly removes transitions
										// Transition might already be removed by recursion
										Set<Transition> transitions = new HashSet<>(
												getSolvingObject().getGame().getTransitions());
										for (Transition t : transitions) {
											if (t.getId().equals(transition)) {
												// System.out.println("starting " + t);
												getSolvingObject().removeTransitionRecursively(t);
											}
										}
									} else {
										// original system place removes ALL transitions
										Set<Place> places = new HashSet<>(getSolvingObject().getGame().getPlaces());
										for (Place p : places) {
											if (p.getId().equals(place)) {
												Set<Transition> transitions = new HashSet<>(p.getPostset());
												for (Transition post : transitions) {
													if (transition.equals(getTruncatedId(post.getId()))) {
														// System.out.println("starting " + post);
														getSolvingObject().removeTransitionRecursively(post);
													}
												}
											}
										}
									}
								}
							} else {
								// 0 is the last member
								// System.out.println("Finished reading strategy.");
								new PGSimplifier(getSolvingObject(), true, false, true).simplifyPG();
								strategy = new PetriGame(getSolvingObject().getGame());
								return getSolvingObject().getGame();
							}
						}
					}
				}
			}
			// There were no decision points for the system, thus the previous loop did not leave the method
			new PGSimplifier(getSolvingObject(), true, false, true).simplifyPG();
			strategy = new PetriGame(getSolvingObject().getGame());
			return getSolvingObject().getGame();
		}
		throw new NoStrategyExistentException();
	}
}
