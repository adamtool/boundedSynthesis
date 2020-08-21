package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.connectivity.Connectivity;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.pg.CalculationInterruptedException;
import uniolunisaar.adam.exceptions.pg.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.pg.SolvingException;

/**
 * 
 * @author Niklas Metzger
 *
 */

public class QbfConSafetySolver extends QbfConSolver<Safety> {

	// variable to store keys of calculated components for later use
	private int[] bad;
	private int detenvlocal;
	private int detenv;
	private int detAdditionalSys;
	private int strongdet;
	

	
	
	public QbfConSafetySolver(QbfSolvingObject<Safety> solObj, QbfConSolverOptions so) throws SolvingException {
		super(solObj, so);
		bad = new int[getSolvingObject().getN() + 1];
	}

	private void writeDetEnv() throws IOException {
		detenv = createUniqueID();
		if (!getSolvingObject().getGame().getEnvPlaces().isEmpty())
			writer.write(detenv + " = " + getDetEnv());
	}

	/**
	 * Filters the strongComponent places into the strongComponents field
	 */
	private void getLoopingPlaces(){
		Set<? extends Set<Node>> components = Connectivity.getStronglyConnectedComponents(getSolvingObject().getGame());
		strongComponents = new HashSet<Set<Place>>();
		for (Set<Node> currentSet : components) {
			Set<Place> newSet = new HashSet<Place>();
			for (Node currentNode : currentSet) {
				if (currentNode.getClass() == Place.class) {
					newSet.add((Place)currentNode);
				}
			}
			strongComponents.add(newSet);
		}
		//All single set sccs are translated to one SCC, otherwise 
		//no progress in a scc is translated to winning!
		Set<Set<Place>> newComponents = new HashSet<>();
		Set<Place> singleSets = new HashSet<Place>();
		for (Set<Place> set : strongComponents) {
			if (set.size() == 1){
				singleSets.addAll(set);
			}
			if (set.size() > 1){
				newComponents.add(set);
			}
		}
		newComponents.add(singleSets);
		strongComponents = newComponents;
		
	}
	/**
	 * True, if places can be reached > 1 times, false otherwise
	 * 
	 * @param components: strong components of the petri net
	 * @return
	 */
	private boolean checkLocalLoops(Set<Set<Place>> components) {
		for (Set<Place> currentSet : components)
			if (currentSet.size() > 1)
				return true;
		return false;
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
				forall.add(number);
			}
		}
		loopBound = checkLocalLoops(strongComponents) ? getSolvingObject().getN() : 1;
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
				Set<String> truncatedIDs = new HashSet<>();
				for (Transition t : p.getPostset()) {
					String ID = t.getId();
					if (!truncatedIDs.contains(ID)) { 
						truncatedIDs.add(ID);
						for (int i = 1; i <= loopBound; ++i) { 
							int number = createVariable(p.getId() + "**" + ID + "**" + i);
							forall.add(number);
						}
					}
				}

			}
		}
		//Add environment stalling transitions that are not purely environmental
		//Truncated transitions behave equally
		Set<String> testSet = new HashSet<>();
		for (Transition t : getSolvingObject().getGame().getTransitions()){
			Set<Place> sysPrePlaces = new HashSet<Place>(t.getPreset());
			sysPrePlaces.removeAll(getSolvingObject().getGame().getEnvPlaces());
			if (!sysPrePlaces.isEmpty()) { //At least one preplace has to be system
			String truncatedID = getTruncatedId(t.getId());
			if (!testSet.contains(truncatedID)){
				testSet.add(truncatedID);
				forall.add(createVariable(truncatedID + "**" + "stall"));
				}
			}
		}
		if (!forall.isEmpty()) {
			writer.write(writeForall(forall));
		}
		
	}

	
	@Override
	protected void writeQCIR() throws IOException {
		Map<Place, Set<Transition>> systemHasToDecideForAtLeastOne = unfoldPG();

		Set<Place> oldBad = new HashSet<>(getSolvingObject().getWinCon().getBadPlaces());
		getWinningCondition().buffer(getSolvingObject().getGame()); 
		if (getSolvingObject().getB() > 1 && QbfControl.rebuildingUnfolder) {
			for (Place old : oldBad) {
				getSolvingObject().getWinCon().getBadPlaces().remove(old); 
			}
		}

		initializeCaches();
		getLoopingPlaces();
		writer.write("#QCIR-G14" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak); // spaces left to add variable count in the end
		addExists();
		addForall();
		writer.write("output(1)" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak);
		writeDetEnv();
		writeStrongDet(); // strong determinism for additional system places enforced
		writeInitial();
		writer.write("# End of Initial\n");
		writeTerminating();
		writer.write("# End of terminating\n");
		writeDeadlock();
		writer.write("# End of Deadlock\n");
		writeFlow();
		writer.write("# End of Flow\n");
		writeSequence();
		writer.write("# start of no bad marking\n");
		writeNoBadMarking();
		writer.write("# End of No Bad Marking\n");
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
		int index_for_non_det_unfolding_info = enumerateStratForNonDetUnfold(systemHasToDecideForAtLeastOne);

		if (index_for_non_det_unfolding_info != -1) {
			phi.add(index_for_non_det_unfolding_info);
		}

		for (int i = 1; i <= getSolvingObject().getN() - 1; ++i) { // slightly optimized in the sense that winning and  loop are put together for n
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
		writer.write(seqImpliesWin[getSolvingObject().getN()] + " = " + "or(-" + seq[getSolvingObject().getN()] + ","
				+ wnandLoop + ")" + QbfControl.linebreak);
		phi.add(seqImpliesWin[getSolvingObject().getN()]);
		int phi_number = createUniqueID();
		writer.write(phi_number + " = " + writeAnd(phi));
		int output = -1;
		if (!getSolvingObject().getGame().getEnvPlaces().isEmpty() && (detAdditionalSys != 0)) { // with strongdet
			output = createUniqueID();
			writer.write(output + " = " + "and(" + detAdditionalSys + "," + writeImplication(detenv, phi_number) + ")");
		} else if (!getSolvingObject().getGame().getEnvPlaces().isEmpty()) { // without strongdet (no additional system places)
			output = writeImplication(detenv, phi_number);
		} else { // no env places
			output = phi_number;
		}
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
		
		//assert (QCIRconsistency.checkConsistency(file)); //TODO make compatible with true concurrent
	}

	@Override
	protected PetriGame calculateStrategy() throws NoStrategyExistentException, CalculationInterruptedException {
		return calculateStrategy(true);
	}
}
