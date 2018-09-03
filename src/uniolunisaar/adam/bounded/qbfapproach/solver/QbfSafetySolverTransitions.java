package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;

public class QbfSafetySolverTransitions extends QbfSolver<Safety> {
	
	// variable to store keys of calculated components for later use (special to this winning condition)
	private int[] bad;
	private Map<Place, Integer[]> gp = new HashMap<>();
	private Map<Place, Integer[]> rp = new HashMap<>();
	private Map<Place, Integer[]> sp = new HashMap<>();
	private Map<Transition, Integer[]> et = new HashMap<>();
	private Map<Transition, Integer[]> tt = new HashMap<>();
	private Map<Place, Integer[]> nc = new HashMap<>();

	public QbfSafetySolverTransitions(PetriGame game, Safety winCon, QbfSolverOptions so) throws SolvingException {
		super(game, winCon, so);
		bad = new int[getSolvingObject().getN() + 1];
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			gp.put(p, new Integer[getSolvingObject().getN() + 1]);
			rp.put(p, new Integer[getSolvingObject().getN() + 1]);
			sp.put(p, new Integer[getSolvingObject().getN() + 1]);
			nc.put(p, new Integer[getSolvingObject().getN()]);
		}
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			et.put(t, new Integer[getSolvingObject().getN()]);
			tt.put(t, new Integer[getSolvingObject().getN()]);
		}
	}
	
	protected void addForall() throws IOException {
		Set<Integer> forall = new HashSet<>();
		/*for (int i = 1; i <= getSolvingObject().getN(); ++i) {		// TODO removed env decision
			for (Place env : getSolvingObject().getEnvPlaces()) {
				for (Transition t : env.getPostset()) {
					int number = createVariable(env.getId() + "." + t.getId() + "." + i);
					System.out.println(number + " : " + env.getId() + "." + t.getId() + "." + i);
					forall.add(number);
				}
			}
		}*/
		
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				int number = createVariable(t.getId() + "." + i);
				System.out.println(number + " : " + t.getId() + "." + i);
				forall.add(number);
			}
		}
		writer.write(writeForall(forall));
		//writer.write("output(1)" + QBFSolver.spacesToReplaceWithMaxVarNumber + QBFSolver.linebreak);
	}
	
	protected void addPlaces() throws IOException {
		Set<Integer> exists = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			for (Place p : getSolvingObject().getGame().getPlaces()) {
				int number = 
				createVariable(p.getId() + "." + i);
				System.out.println(number + " : " + p.getId() + "." + i);
				exists.add(number);
			}
		}

		writer.write(writeExists(exists));
		writer.write("output(1)" + QbfSolver.replaceAfterWardsSpaces + QbfSolver.linebreak);
	}
	
	protected Map<Place, String[]> getGenerateToken() {
		Map<Place, String[]> generateToken = new HashMap<>();
		Set<Integer> or = new HashSet<>(); 
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			String[] gp = new String[getSolvingObject().getN() + 1];
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				or.clear();
				for (Transition pre : p.getPreset()) {
					or.add(getVarNr(pre.getId() + "." + i, true));
				}
				gp[i + 1] = writeOr(or);
			}
			generateToken.put(p, gp);
		}
		return generateToken;
	}
	
	protected void writeGenerateToken() throws IOException {
		Map<Place, String[]> generateToken = getGenerateToken();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = 2; i <= getSolvingObject().getN(); ++i) {
				int id = createUniqueID();
				writer.write(id + " = " + generateToken.get(p)[i]);
				gp.get(p)[i] = id;
			}
		}
		writer.write("# end genToken \n");
	}
	
	protected Map<Place, String[]> getRemoveToken() {
		Map<Place, String[]> removeToken = new HashMap<>();
		Set<Integer> or = new HashSet<>(); 
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			String[] rp = new String[getSolvingObject().getN() + 1];
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				or.clear();
				for (Transition post : p.getPostset()) {
					or.add(getVarNr(post.getId() + "." + i, true));
				}
				rp[i + 1] = writeOr(or);
			}
			removeToken.put(p, rp);
		}
		return removeToken;
	}
	
	protected void writeRemoveToken() throws IOException {
		Map<Place, String[]> removeToken = getRemoveToken();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = 2; i <= getSolvingObject().getN(); ++i) {
				int id = createUniqueID();
				writer.write(id + " = " + removeToken.get(p)[i]);
				rp.get(p)[i] = id;
			}
		}
		writer.write("# end remToken \n");
	}
	
	protected Map<Place, String[]> getStayToken() {
		Map<Place, String[]> stayToken = new HashMap<>();
		Set<Integer> and = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			String[] sp = new String[getSolvingObject().getN() + 1];
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				and.clear();
				and.add(getVarNr(p.getId() + "." + i, true));
				and.add(-rp.get(p)[i+1]);
				sp[i+1] = writeAnd(and);
			}
			stayToken.put(p, sp);
		}
		return stayToken;
	}
	
	protected void writeStayToken() throws IOException {
		Map<Place, String[]> stayToken = getStayToken();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = 2; i <= getSolvingObject().getN(); ++i) {
				int id = createUniqueID();
				writer.write(id + " = " + stayToken.get(p)[i]);
				sp.get(p)[i] = id;
			}
		}
		writer.write("# end stayToken \n");
	}
	
	protected Map<Place, String[]> getToken() {
		Map<Place, String[]> token = new HashMap<>();
		Set<Integer> or = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			String[] pp = new String[getSolvingObject().getN() + 1];
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				or.clear();
				or.add(gp.get(p)[i+1]);
				or.add(sp.get(p)[i+1]);
				pp[i+1] = writeOr(or);
			}
			token.put(p, pp);
		}
		return token;
	}
	
	protected void writeToken() throws IOException {
		Map<Place, String[]> token = getToken();
		Marking initial = getSolvingObject().getGame().getInitialMarking();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (initial.getToken(p).getValue() >= 1) {
				writer.write(getVarNr(p.getId() + "." + 1, true) + " = and()\n");
			} else {
				writer.write(getVarNr(p.getId() + "." + 1, true) + " = or()\n");
			}
		}
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = 2; i <= getSolvingObject().getN(); ++i) {
				writer.write(getVarNr(p.getId() + "." + i, true) + " = " + token.get(p)[i]);
			}
		}
		writer.write("# end token \n");
	}
	
	protected Map<Transition, String[]> getEnabledTransition() {
		Map<Transition, String[]> enabledTransition = new HashMap<>();
		Set<Integer> and = new HashSet<>();
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			String[] et = new String[getSolvingObject().getN()];
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				and.clear();
				for (Place pre : t.getPreset()) {
					and.add(getVarNr(pre.getId() + "." + i, true));
					if (getSolvingObject().getGame().getEnvPlaces().contains(pre)) {
						//and.add(getVarNr(pre.getId() + "." + t.getId() + "." + i, true));		// TODO removed env decision
					} else {
						// system place
						and.add(getVarNr(pre.getId() + ".." + t.getId(), true));
					}
				}
				et[i] = writeAnd(and);
			}
			enabledTransition.put(t, et);
		}
		return enabledTransition;
	}
	
	protected void writeEnabledTransition() throws IOException {
		Map<Transition, String[]> enabledTransition = getEnabledTransition();
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				int id = createUniqueID();
				writer.write(id + " = " + enabledTransition.get(t)[i]);
				et.get(t)[i] = id;
			}
		}
		writer.write("# end enabledTransition \n");
	}
	
	protected Map<Transition, String[]> getTIE() {		// TIE = transition implies enabled 
		Map<Transition, String[]> tie = new HashMap<>();
		Set<Integer> or = new HashSet<>();
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			String[] tt = new String[getSolvingObject().getN()];
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				or.clear();
				or.add(-getVarNr(t.getId() + "." + i, true));
				or.add(et.get(t)[i]);
				tt[i] = writeOr(or);
			}
			tie.put(t, tt);
		}
		return tie;
	}
	
	protected void writeTIE() throws IOException {
		Map<Transition, String[]> tie = getTIE();
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				int id = createUniqueID();
				writer.write(id + " = " + tie.get(t)[i]);
				tt.get(t)[i] = id;
			}
		}
		writer.write("# end TIE \n");
	}
	
	protected Map<Place, String[]> getNoConflict() throws IOException {
		Map<Place, String[]> noConflict = new HashMap<>();
		Set<Integer> and = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			String[] nc = new String[getSolvingObject().getN()];
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				and.clear();
				Transition[] transitions = getSolvingObject().getGame().getTransitions().toArray(new Transition[0]);
				for (int j = 0; j < transitions.length; ++j) {
					Transition t1 = transitions[j];
					for (int k = j + 1; k < transitions.length; ++k) {
						Transition t2 = transitions[k];
						int i1 = getVarNr(t1.getId() + "." + i, true);
						int i2 = getVarNr(t2.getId() + "." + i, true);
						and.add(writeImplication(i1, -i2));
					}
				}
				nc[i] = writeAnd(and);
			}
			noConflict.put(p, nc);
		}
		return noConflict;
	}
	
	protected void writeNoConflict() throws IOException {
		Map<Place, String[]> noConflict = getNoConflict();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				int id = createUniqueID();
				writer.write(id + " = " + noConflict.get(p)[i]);
				nc.get(p)[i] = id;
			}
		}
		writer.write("# end NC \n");
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
	
	
	protected void writeNoBadPlaces() throws IOException {
		if (!getSolvingObject().getWinCon().getBadPlaces().isEmpty()) {
			String[] nobadplaces = getNoBadPlaces();
			for (int i = 1; i <= getSolvingObject().getN(); ++i) {
				bad[i] = createUniqueID();
				writer.write(bad[i] + " = " + nobadplaces[i]);
			}
		}
	}
	
	protected void writeDeadlockSubFormulas(int s, int e) throws IOException {
		Set<Integer> or = new HashSet<>();
		Transition t;
		int key;
		int strat;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
				t = transitions[j];
				or.clear();
				for (Place p : t.getPreset()) {
					or.add(-getVarNr(p.getId() + "." + i, true)); // "p.i"
					strat = addSysStrategy(p, t);
					if (strat != 0) {
						or.add(-strat); // "p.t" if sys
					}
					if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
						//or.add(-getVarNr(p.getId() + "." + t.getId() + "." + i, true));		// TODO removed env decision
					}
				}
				key = createUniqueID();
				writer.write(key + " = " + writeOr(or));
				deadlockSubFormulas[getSolvingObject().getGame().getTransitions().size() * (i - 1) + j] = key;
			}
		}
	}
	
	protected String[] getDeadlock() throws IOException {
		String[] deadlock = new String[getSolvingObject().getN() + 1];
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			and.clear();
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				and.add(-getVarNr(t.getId() + "." + i, true));
			}
			deadlock[i] = writeAnd(and);
		}
		// n via places
		writeDeadlockSubFormulas(getSolvingObject().getN(), getSolvingObject().getN());
		and.clear();
		for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
			and.add(deadlockSubFormulas[getSolvingObject().getGame().getTransitions().size() * (getSolvingObject().getN() - 1) + j]);
		}
		deadlock[getSolvingObject().getN()] = writeAnd(and);
		return deadlock;
	}
	
	protected void writeDeadlock() throws IOException {
		String[] deadlock = getDeadlock();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			dl[i] = createUniqueID();
			writer.write(dl[i] + " = " + deadlock[i]);
		}
	}
	
	protected String[] getFlow() throws IOException {
		String[] flow = new String[getSolvingObject().getN()];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			or.clear();
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				or.add(getVarNr(t.getId() + "." + i, true));
			}
			flow[i] = writeOr(or);
		}
		return flow;
	}
	
	protected void writeFlow() throws IOException {
		String[] flow = getFlow();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			fl[i] = createUniqueID();
			writer.write(fl[i] + " = " + flow[i]);
		}
	}
	
	protected int writeOneMissingPre(Transition t1, Transition t2, int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		int strat;
		for (Place p : t1.getPreset()) {
			or.add(-getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t1);
			if (strat != 0) {
				or.add(-strat);
			}
			if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
				// or.add(-getVarNr(p.getId() + "." + t1.getId() + "." + i, true));		// TODO removed env decision
			}
		}
		for (Place p : t2.getPreset()) {
			or.add(-getVarNr(p.getId() + "." + i, true));
			strat = addSysStrategy(p, t2);
			if (strat != 0) {
				or.add(-strat);
			}
			if (getSolvingObject().getGame().getEnvPlaces().contains(p)) {
				// or.add(-getVarNr(p.getId() + "." + t2.getId() + "." + i, true));		// TODO removed env decision
			}
		}

		int number = createUniqueID();
		writer.write(number + " = " + writeOr(or));
		return number;
	}
	
	protected String[] getDeterministic() throws IOException { // faster than naive implementation
		List<Set<Integer>> and = new ArrayList<>(getSolvingObject().getN() + 1);
		and.add(null); // first element is never accessed
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.add(new HashSet<Integer>());
		}
		Transition t1, t2;
		Transition[] sys_transitions;
		for (Place sys : getSolvingObject().getGame().getPlaces()) {
			// Additional system places are not forced to behave deterministically, this is
			// the faster variant (especially the larger the PG becomes)
			if (!getSolvingObject().getGame().isEnvironment(sys) && !sys.getId().startsWith(QbfSolver.additionalSystemName)) {
				if (sys.getPostset().size() > 1) {
					sys_transitions = sys.getPostset().toArray(new Transition[0]);
					for (int j = 0; j < sys_transitions.length; ++j) {
						t1 = sys_transitions[j];
						for (int k = j + 1; k < sys_transitions.length; ++k) {
							t2 = sys_transitions[k];
							for (int i = 1; i < getSolvingObject().getN(); ++i) {
								and.get(i).add(writeImplication(getVarNr(t1.getId() + "." + i, true), -getVarNr(t2.getId() + "." + i, true)));
							}
							// n with places
							and.get(getSolvingObject().getN()).add(writeOneMissingPre(t1, t2, getSolvingObject().getN()));
						}
					}
				}
			}
		}
		String[] deterministic = new String[getSolvingObject().getN() + 1];
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			if (!and.get(i).isEmpty()) {
				deterministic[i] = writeAnd(and.get(i));
			} else {
				Pair<Boolean, Integer> result = getVarNrWithResult("and()");
				if (result.getFirst()) {
					writer.write(result.getSecond() + " = and()" + QbfSolver.linebreak);
				}
				deterministic[i] = "";
			}
		}
		return deterministic;
	}
	
	protected void writeDeterministic() throws IOException {
		if (deterministicStrat) {
			String[] deterministic = getDeterministic();
			for (int i = 1; i <= getSolvingObject().getN(); ++i) {
				if (!deterministic[i].matches("")) {
					det[i] = createUniqueID();
					writer.write(det[i] + " = " + deterministic[i]);
				} else {
					det[i] = getVarNr("and()", true);
				}
			}
		}
	}
	
	protected void writeSequence() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.clear();
			and.add(in);
			for (int j = 1; j <= i - 1; ++j) {
				// and.add(-dl[j]); // performance evaluation showed that leaving this out makes  program faster as it is redundant
				and.add(fl[j]);
			}
			for (int j = 2; j <= i - 1; ++j) {
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					and.add(nc.get(p)[j-1]);
				}
				for (Transition t : getSolvingObject().getGame().getTransitions()) {
					and.add(tt.get(t)[j-1]);
				}
			}
			seq[i] = createUniqueID();
			writer.write(seq[i] + " = " + writeAnd(and));
		}
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

		if (QbfSolver.mcmillian) {
			Set<Place> oldBad = new HashSet<>(getSolvingObject().getWinCon().getBadPlaces());
	        getWinningCondition().buffer(getSolvingObject().getGame());
	        for (Place old : oldBad) {
	        	getSolvingObject().getWinCon().getBadPlaces().remove(old);
	        }
		}
        
        
		initializeVariablesForWriteQCIR();

		writer.write("#QCIR-G14" + QbfSolver.replaceAfterWardsSpaces + QbfSolver.linebreak); // spaces left to add variable count in the end
		addExists();
		addForall();
		addPlaces();
		
		writeGenerateToken();
		writeRemoveToken();
		writeStayToken();
		writeToken();
		writeEnabledTransition();
		writeTIE();
		writeNoConflict();
		
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
		
		/*// nc and tt have to be true
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				phi.add(nc.get(p)[i]);
			}
		}
		
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			for (int i = 1; i < getSolvingObject().getN(); ++i) {
				phi.add(tt.get(t)[i]);
			}
		}*/

		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			seqImpliesWin[i] = createUniqueID();
			writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QbfSolver.linebreak);
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
		raf.readLine(); // Read exists line // TODO changed
		for (int i = 0; i < 7; ++i) { // read "output(" and thus overwrite "1)"
			raf.readByte();
		}
		counter_str += ")";
		counter_char = counter_str.toCharArray();
		for (char c : counter_char) {
			raf.writeByte(c);
		}

		raf.close();

		if (QbfSolver.debug) {
			FileUtils.copyFile(file, new File(getSolvingObject().getGame().getName() + ".qcir"));
		}

		//assert(QCIRconsistency.checkConsistency(file));
	}

}
