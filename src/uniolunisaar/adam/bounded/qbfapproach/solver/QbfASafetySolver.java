package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QCIRconsistency;
import uniolunisaar.adam.exceptions.pg.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Safety;

/**
 *
 * @author Jesko Hecking-Harbusch
 *
 *         This implements bad places.
 */

public class QbfASafetySolver extends QbfSolver<Safety> {

	// variable to store keys of calculated components for later use (special to this winning condition)
	private int[] bad;

	public QbfASafetySolver(PetriGame game, Safety win, QbfSolverOptions so) throws SolvingException {
		super(game, win, so);
		bad = new int[getSolvingObject().getN() + 1];
	}

	protected void writeNoBadPlaces() throws IOException {
		if (!getSolvingObject().getWinCon().getBadPlaces().isEmpty()) {
			String[] nobadplaces = getNoBadPlaces();
			for (int i = 1; i <= getSolvingObject().getN(); ++i) {
				bad[i] = createUniqueID();
				writer.write(bad[i] + " = " + nobadplaces[i]);
			}
		}
	}

	public String[] getNoBadPlaces() throws IOException {
		String[] nobadplaces = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.clear();
			for (Place p : getSolvingObject().getWinCon().getBadPlaces()) {
				and.add(-getVarNr(p.getId() + "." + i, true));
			}
			nobadplaces[i] = writeAnd(and);
		}
		return nobadplaces;
	}

	protected void writeWinning() throws IOException {
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
			if (i == getSolvingObject().getN()) { // slightly optimized in the sense that winning and loop are put together for n
				and.add(l);
			}
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));
		}
	}

	@Override
	protected void writeQCIR() throws IOException {
		Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = unfoldPG();
		
		if (QbfControl.rebuildingUnfolder) {
			Set<Place> oldBad = new HashSet<>(getSolvingObject().getWinCon().getBadPlaces());
			getWinningCondition().buffer(getSolvingObject().getGame()); 
			for (Place old : oldBad) {
				getSolvingObject().getWinCon().getBadPlaces().remove(old); 
			}
		}
		
		initializeAfterUnfolding();

		writer.write("#QCIR-G14" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak); // spaces left to add variable count in the end
		addExists();
		addForall();
		writer.write("output(1)" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak);

		writeInitial();
		writeTerminating();
		writeDeadlock();
		writeFlow();
		writeSequence();
		writeNoBadPlaces();
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

		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			seqImpliesWin[i] = createUniqueID();
			writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QbfControl.linebreak);
			phi.add(seqImpliesWin[i]);
		}

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

		if (QbfControl.debug) {
			FileUtils.copyFile(file, new File(getSolvingObject().getGame().getName() + ".qcir"));
		}

		assert (QCIRconsistency.checkConsistency(file));
	}

	@Override
	protected PetriGame calculateStrategy() throws NoStrategyExistentException {
		return calculateStrategy(false);
	}
}
