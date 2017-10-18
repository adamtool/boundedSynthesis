package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.winningconditions.Safety;

/**
 *
 * @author Jesko Hecking-Harbusch
 *
 *         This implements bad places.
 */

public class QBFSafetySolver extends QBFSolver<Safety> {

	// variable to store keys of calculated components for later use (special to this winning condition)
	private int[] bad;

	public QBFSafetySolver(PetriNet net, Safety win, QBFSolverOptions so) throws NotSupportedGameException, BoundedParameterMissingException  {
		super(new QBFPetriGame(net), win, so);
		bad = new int[pg.getN() + 1];
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
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			and.clear();
			for (Place p : getWinningCondition().getBadPlaces()) {
				and.add(-getVarNr(p.getId() + "." + i, true));
			}
			nobadplaces[i] = writeAnd(and);
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

		writer.write("1 = " + writeAnd(phi));
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
