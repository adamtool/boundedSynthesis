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

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.NonDeterministicUnfolder;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.winningconditions.Buchi;
import uniolunisaar.adam.tools.ADAMProperties;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class QBFBuchiSolver extends QBFSolver<Buchi> {

	// variable to store keys of calculated components for later use (special to this winning condition)
	private int bl;

	public QBFBuchiSolver(PetriNet net, QBFSolverOptions so) throws UnboundedPGException {
		super(new QBFPetriGame(net), new Buchi(), so);
		fl = new int[pg.getN() + 1];
		det = new int[pg.getN() + 1];
		dl = new int[pg.getN() + 1];
		seq = new int[pg.getN() + 1];
		win = new int[pg.getN() + 1];
	}

	protected void writeLoop() throws IOException {
		String loop = getBuchiLoop();
		bl = createUniqueID();
		writer.write(bl + " = " + loop);
	}

	public String getBuchiLoop() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					int p_i = getVarNr(p.getId() + "." + i, true);
					int p_j = getVarNr(p.getId() + "." + j, true);
					and.add(writeImplication(p_i, p_j));
					and.add(writeImplication(p_j, p_i));
				}
				Set<Integer> innerOr = new HashSet<>();
				for (int k = i; k <= j; ++k) {
					for (Place buchi : getWinningCondition().getBuchiPlaces()) {
						innerOr.add(getVarNr(buchi.getId() + "." + k, true));
					}
				}
				int innerOrNumber = createUniqueID();
				writer.write(innerOrNumber + " = " + writeOr(innerOr));
				and.add(innerOrNumber);

				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				outerOr.add(andNumber);
			}
		}
		return writeOr(outerOr);
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
	protected boolean exWinStrat() {
		NonDeterministicUnfolder unfolder = new NonDeterministicUnfolder(pg, null); // null forces unfolder to use b as bound for every place
		try {
			unfolder.createUnfolding();
		} catch (UnboundedException | FileNotFoundException | NetNotSafeException | NoSuitableDistributionFoundException e1) {
			System.out.println("Error: The bounded unfolding of the game failed.");
			e1.printStackTrace();
		}

		// I dont want NonDetUnfolder<Safe/Reach/Buechi> and therefore add places after unfolding...
		for (Place p : pn.getPlaces()) {
			for (Pair<String, Object> pair : p.getExtensions()) {
				if (pair.getFirst().equals("buchi") && pair.getSecond().toString().equals("true")) {
					getWinningCondition().getBuchiPlaces().add(p);
				}
			}
		}

		seqImpliesWin = new int[pg.getN() + 1];
		transitions = pn.getTransitions().toArray(new Transition[0]);
		flowSubFormulas = new int[pg.getN() * pn.getTransitions().size()];
		deadlockSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];
		int exitcode = -1;
		try {
			writer.write("#QCIR-G14          " + QBFSolver.linebreak); // spaces left to add variable count in the end
			addExists();
			addForall();
			writer.write("output(1)" + QBFSolver.linebreak); // 1 = \phi

			writeInitial();
			writeDeadlock();
			writeFlow();
			writeSequence();
			writeDeterministic();
			writeLoop();
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
			wnandLoopSet.add(bl);
			wnandLoopSet.add(win[pg.getN()]);
			writer.write(wnandLoop + " = " + writeAnd(wnandLoopSet));

			seqImpliesWin[pg.getN()] = createUniqueID();
			writer.write(seqImpliesWin[pg.getN()] + " = " + "or(-" + seq[pg.getN()] + "," + wnandLoop + ")" + QBFSolver.linebreak);
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
				pb = new ProcessBuilder(ADAMProperties.getInstance().getLibFolder() + File.separator + solver + "_mac", "--partial-assignment", file.getAbsolutePath());
			} else if (os.startsWith("Linux")) {
				System.out.println("Your operation system is supported.");
				pb = new ProcessBuilder(ADAMProperties.getInstance().getLibFolder() + File.separator + solver + "_unix", "--partial-assignment", file.getAbsolutePath());
			} else {
				System.out.println("Your operation system is not supported.");
				return false;
			}
			System.out.println("A temporary file is saved to \"" + file.getAbsolutePath() + "\".");

			Process pr = pb.start();
			// Read caqe's output
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line_read;
			outputCAQE = "";
			while ((line_read = inputReader.readLine()) != null) {
				outputCAQE += line_read + "\n";
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
			System.out.println("QCIR ERROR with FULL output:" + outputCAQE);
			solvable = false;
			sat = null;
			error = true;
			return false;
		}
	}

	@Override
	protected PetriNet calculateStrategy() throws NoStrategyExistentException {
		if (existsWinningStrategy()) {
			for (String outputCAQE_line : outputCAQE.split("\n")) {
				if (outputCAQE_line.startsWith("V")) {
					String[] parts = outputCAQE_line.split(" ");
					for (int i = 0; i < parts.length; ++i) {
						if (!parts[i].equals("V")) {
							int num = Integer.parseInt(parts[i]);
							if (num > 0) {
								// System.out.println("ALLOW " + num);
							} else if (num < 0) {
								// System.out.println("DISALLOW " + num * (-1));
								String remove = exists_transitions.get(num * (-1));
								int in = remove.indexOf("..");
								if (in != -1) { // CTL has exists variables for path EF which mean not remove
									String place = remove.substring(0, in);
									String transition = remove.substring(in + 2, remove.length());
									if (place.startsWith(QBFSolver.additionalSystemName)) {
										// additional system place exactly removes transitions
										// Transition might already be removed by recursion
										Set<Transition> transitions = new HashSet<>(pg.getNet().getTransitions());
										for (Transition t : transitions) {
											if (t.getId().equals(transition)) {
												// System.out.println("starting " + t);
												pg.removeTransitionRecursively(t);
											}
										}
									} else {
										// original system place removes ALL transitions
										Set<Place> places = new HashSet<>(pg.getNet().getPlaces());
										for (Place p : places) {
											if (p.getId().equals(place)) {
												Set<Transition> transitions = new HashSet<>(p.getPostset());
												for (Transition post : transitions) {
													if (transition.equals(getTruncatedId(post.getId()))) {
														// System.out.println("starting " + post);
														pg.removeTransitionRecursively(post);
													}
												}
											}
										}
									}
								}
							} else {
								// 0 is the last member
								// System.out.println("Finished reading strategy.");
								PGSimplifier.simplifyPG(pg, true);
								return pg.getNet();
							}
						}
					}
				}
			}
		}
		throw new NoStrategyExistentException();
	}

}