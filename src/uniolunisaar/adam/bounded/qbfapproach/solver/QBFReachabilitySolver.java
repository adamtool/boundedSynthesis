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
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QCIRconsistency;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.winningconditions.Reachability;

public class QBFReachabilitySolver extends QBFSolver<Reachability> {

	// variable to store keys of calculated components for later use (special to this winning condition)
	private int[] goodPlaces;

	public QBFReachabilitySolver(PetriNet net, Reachability win, QBFSolverOptions so) throws NotSupportedGameException, BoundedParameterMissingException {
		super(new QBFPetriGame(net), win, so);
		goodPlaces = new int[pg.getN() + 1];
	}

	private void writeGoodPlaces() throws IOException {
		if (!getWinningCondition().getPlaces2Reach().isEmpty()) {
			String[] good = getGoodPlaces();
			for (int i = 1; i <= pg.getN(); ++i) {
				goodPlaces[i] = createUniqueID();
				writer.write(goodPlaces[i] + " = " + good[i]);
			}
		}
	}

	public String[] getGoodPlaces() {
		String[] goodPlaces = new String[pg.getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			or.clear();
			for (Place p : getWinningCondition().getPlaces2Reach()) {
				or.add(getVarNr(p.getId() + "." + i, true));
			}
			goodPlaces[i] = writeOr(or);
		}
		return goodPlaces;
	}

	private void writeWinning() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
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
						writer.write(result.getSecond() + " = or()" + QBFSolver.linebreak);
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
		and.add(dlt[pg.getN()]);
		and.add(det[pg.getN()]);
		or.clear();
		for (int i = 1; i <= pg.getN(); ++i) {
			if (goodPlaces[i] != 0) {
				or.add(goodPlaces[i]);
			} 
		}
		int orID = createUniqueID();
		writer.write(orID + " = " + writeOr(or));
		and.add(orID);
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
		writeFlow();
		writeSequence();
		writeGoodPlaces();
		writeDeadlock();
		writeTerminating();
		writeDeadlocksterm();
		writeDeterministic();
		writeWinning();
		writeLoop();
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

		for (int i = 1; i <= pg.getN() - 1; ++i) { // slightly optimized in the sense that winning and loop are put together for n
			seqImpliesWin[i] = createUniqueID();
			writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QBFSolver.linebreak);
			phi.add(seqImpliesWin[i]);
		}

		int winandLoop = createUniqueID();
		Set<Integer> winandLoopSet = new HashSet<>();
		winandLoopSet.add(l);
		winandLoopSet.add(win[pg.getN()]);
		writer.write(winandLoop + " = " + writeAnd(winandLoopSet));
		
		// TODO loop direkt bei getWinning hinzufügen
		seqImpliesWin[pg.getN()] = createUniqueID();
		writer.write(seqImpliesWin[pg.getN()] + " = " + "or(-" + seq[pg.getN()] + "," + winandLoop + "," + u + ")" + QBFSolver.linebreak);
		phi.add(seqImpliesWin[pg.getN()]);

		writer.write("1 = " + writeAnd(phi));
		writer.close();
		
		if (QBFSolver.debug) {
			FileUtils.copyFile(file, new File(pn.getName() + ".qcir"));
		}
		
		assert(QCIRconsistency.checkConsistency(file));

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
	}
}
