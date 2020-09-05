package uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach;

import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolverOptions;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.petrigame.QCIRconsistency;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolvingObject;
import uniolunisaar.adam.exceptions.pg.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Reachability;
import uniolunisaar.adam.exceptions.pg.CalculationInterruptedException;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public class QbfEReachabilitySolver extends QbfSolver<Reachability> {

	// variable to store keys of calculated components for later use (special to this winning condition)
	private final int[] goodPlaces;

	public QbfEReachabilitySolver(QbfSolvingObject<Reachability> solObj, QbfSolverOptions so) throws SolvingException {
		super(solObj, so);
		goodPlaces = new int[getSolvingObject().getN() + 1];
	}

	private void writeGoodPlaces() throws IOException {
		if (!getSolvingObject().getWinCon().getPlaces2Reach().isEmpty()) {
			String[] good = getGoodPlaces();
			for (int i = 1; i <= getSolvingObject().getN(); ++i) {
				goodPlaces[i] = createUniqueID();
				writer.write(goodPlaces[i] + " = " + good[i]);
			}
		}
	}

	public String[] getGoodPlaces() {
		String[] goodPlaces = new String[getSolvingObject().getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			or.clear();
			for (Place p : getSolvingObject().getWinCon().getPlaces2Reach()) {
				or.add(getVarNr(p.getId() + "." + i, true));
			}
			goodPlaces[i] = writeOr(or);
		}
		return goodPlaces;
	}

	private void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			and.clear();
			and.add(dlt[i]);
			and.add(det[i]);
			or.clear();
			or.add(-dl[i]); // TODO: -dl[i] or -term[i] (fl[i] does not work), makes probably no difference because of dlt
			for (int j = 1; j <= i; ++j) {
				if (goodPlaces[j] != 0) {
					or.add(goodPlaces[j]);
				} else {
					// empty set of places to reach never lets system win
					Pair<Boolean, Integer> result = getVarNrWithResult("or()");
					if (result.getFirst()) {
						writer.write(result.getSecond() + " = or()" + QbfControl.linebreak);
					}
					or.add(result.getSecond());
				}
			}
			int orID = createUniqueID();
			writer.write(orID + " = " + writeOr(or));
			and.add(orID);
			win[i] = createUniqueID();
			writer.write(win[i] + " = " + writeAnd(and));

		}
		and.clear();
		and.add(dlt[getSolvingObject().getN()]);
		and.add(det[getSolvingObject().getN()]);
		and.add(l);	// slightly optimized in the sense that winning and loop are put together for n
		or.clear();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			if (goodPlaces[i] != 0) {
				or.add(goodPlaces[i]);
			} else {
				// empty set of places to reach never lets system win
				Pair<Boolean, Integer> result = getVarNrWithResult("or()");
				if (result.getFirst()) {
					writer.write(result.getSecond() + " = or()" + QbfControl.linebreak);
				}
				or.add(result.getSecond());
			}
		}
		int orID = createUniqueID();
		writer.write(orID + " = " + writeOr(or));
		and.add(orID);
		win[getSolvingObject().getN()] = createUniqueID();
		writer.write(win[getSolvingObject().getN()] + " = " + writeAnd(and));
	}

	@Override
	protected void writeQCIR() throws IOException {
		Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = unfoldPG();

		initializeCaches();

		writer.write("#QCIR-G14" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak); // spaces left to add variable count in the end
		addExists();
		addForall();
		writer.write("output(1)" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak);

		writeInitial();
		writeFlow();
		writeSequence();
		writeGoodPlaces();
		writeTerminating();
		writeDeadlock();
		writeDeadlocksterm();
		writeDeterministic();
		writeLoop();
		writeUnfair();
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
			if (i < getSolvingObject().getN()) {
				writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QbfControl.linebreak);
			} else {
				writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + "," + u + ")" + QbfControl.linebreak);		// adding unfair
			}
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
	protected PetriGame calculateStrategy() throws NoStrategyExistentException, CalculationInterruptedException {
		return calculateStrategy(false);
	}
}
