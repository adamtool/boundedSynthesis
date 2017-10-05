package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public abstract class QBFFlowChainSolver<W extends WinningCondition> extends QBFSolver<W> {

	protected QBFFlowChainSolver(QBFPetriGame game, W winCon, QBFSolverOptions options) {
		super(game, winCon, options);
	}

	@Override
	protected void addForall() throws IOException {
		Set<Integer> forall = new HashSet<>();
		int id;
		for (Place p : pg.getNet().getPlaces()) {
			for (int i = 1; i <= pg.getN(); ++i) {
				id = createVariable(p.getId() + "." + i + "." + true);
				forall.add(id);
				id = createVariable(p.getId() + "." + i + "." + false);
				forall.add(id);
			}
		}
		writer.write(writeForall(forall));
		writer.write("output(1)" + QBFSolver.linebreak); // 1 = \phi
		makeThreeValuedLogic();
	}
	
	private void makeThreeValuedLogic() throws IOException {
		int top;
		int bot;
		int id;
		for (Place p : pg.getNet().getPlaces()) {
			for (int i = 1; i <= pg.getN(); ++i) {
				top = getVarNr(p.getId() + "." + i + "." + true, true);
				bot = getVarNr(p.getId() + "." + i + "." + false, true);
				
				id = createVariable(p.getId() + "." + i + "." + "safe");
				writer.write(id + " = and(" + top + "," + "-" + bot + ")" + QBFSolver.linebreak);
				
				id = createVariable(p.getId() + "." + i + "." + "unsafe");
				writer.write(id + " = and(" + "-" + top + "," + bot + ")" + QBFSolver.linebreak);
				
				id = createVariable(p.getId() + "." + i + "." + "empty");
				writer.write(id + " = and(" + "-" + top + "," + "-" + bot + ")" + QBFSolver.linebreak);
			}
		}
	}
	
	protected int getOneUnsafeFlowChain(Place p, Transition t, int i) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("oneUNSAFEFlowChain" + p.getId() + "." + t.getId() + "." + i);
		if (result.getFirst()) {
			Set<Integer> or = new HashSet<>();
			for (Pair<Place, Place> pair : pg.getFl().get(t)) {
				if (pair.getSecond().equals(p)) {
					or.add(getVarNr(pair.getFirst().getId() + "." + i + "." + "unsafe", true));
				}
			}
			writer.write(result.getSecond() + " = " + writeOr(or));
		}
		return result.getSecond();
	}
	
	protected int getAllSafeFlowChain(Place p, Transition t, int i) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("allSAFEFlowChain" + p.getId() + "." + t.getId() + "." + i);
		if (result.getFirst()) {
			Set<Integer> and = new HashSet<>();
			for (Pair<Place, Place> pair : pg.getFl().get(t)) {
				if (pair.getSecond().equals(p)) {
					and.add(getVarNr(pair.getFirst().getId() + "." + i + "." + "safe", true));
				}
			}
			writer.write(result.getSecond() + " = " + writeAnd(and));
		}
		return result.getSecond();
	}
	
	@Override
	protected void writeDeadlockSubFormulas(int s, int e) throws IOException {
		Transition t;
		Set<Integer> or = new HashSet<>();
		int number;
		int strat;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				t = transitions[j];
				or.clear();
				for (Place p : t.getPreset()) {
					or.add(getVarNr(p.getId() + "." + i + "." + "empty", true)); // "p.i.empty"
					strat = addSysStrategy(p, t);
					if (strat != 0) {
						or.add(-strat);
					}
				}
				number = createUniqueID();
				writer.write(number + " = " + writeOr(or));
				deadlockSubFormulas[pn.getTransitions().size() * (i - 1) + j] = number;
			}
		}
	}
	
	@Override
	protected void writeTerminatingSubFormulas(int s, int e) throws IOException {
		Set<Integer> or = new HashSet<>();
		Set<Place> pre;
		Transition t;
		int key;
		for (int i = s; i <= e; ++i) {
			for (int j = 0; j < pn.getTransitions().size(); ++j) {
				t = transitions[j];
				pre = t.getPreset();
				or.clear();
				for (Place p : pre) {
					or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
				}
				key = createUniqueID();
				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[pn.getTransitions().size() * (i - 1) + j] = key;
			}
		}
	}
	
	@Override
	protected int writeOneMissingPre(Transition t1, Transition t2, int i) throws IOException {
		Set<Integer> or = new HashSet<>();
		int strat;
		for (Place p : t1.getPreset()) {
			or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
			strat = addSysStrategy(p, t1);
			if (strat != 0)
				or.add(-strat);
		}
		for (Place p : t2.getPreset()) {
			or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
			strat = addSysStrategy(p, t2);
			if (strat != 0)
				or.add(-strat);
		}

		int number = createUniqueID();
		writer.write(number + " = " + writeOr(or));
		return number;
	}
	
	@Override
	public String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(additionalSystemName)) {
						int p_i_safe = getVarNr(p.getId() + "." + i + "." + "safe", true);
						int p_j_safe = getVarNr(p.getId() + "." + j + "." + "safe", true);
						and.add(writeImplication(p_i_safe, p_j_safe));
						and.add(writeImplication(p_j_safe, p_i_safe));
						int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "unsafe", true);
						int p_j_unsafe = getVarNr(p.getId() + "." + j + "." + "unsafe", true);
						and.add(writeImplication(p_i_unsafe, p_j_unsafe));
						and.add(writeImplication(p_j_unsafe, p_i_unsafe));
						int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
						int p_j_empty = getVarNr(p.getId() + "." + j + "." + "empty", true);
						and.add(writeImplication(p_i_empty, p_j_empty));
						and.add(writeImplication(p_j_empty, p_i_empty));
					}
				}
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				or.add(andNumber);
			}
		}
		return writeOr(or);
	}
	
	// TODO unclear whether this helps
	protected int valid() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= pg.getN(); ++i) {
			for (Place p : pn.getPlaces()) {
				and.clear();
				and.add(getVarNr(p.getId() + "." + i + "." + true, true));
				and.add(getVarNr(p.getId() + "." + i + "." + false, true));
				int id = createUniqueID();
				writer.write(id + " = " + writeAnd(and));
				or.add(id);
			}
		}
		int returnValue = createUniqueID();
		writer.write(returnValue + " = " + writeOr(or));
		return returnValue;
	}
	
	@Override
	protected PetriNet calculateStrategy() throws NoStrategyExistentException {
		if (existsWinningStrategy()) {
			for (String outputCAQE_line : outputQBFsolver.split("\n")) {
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
								PGSimplifier.simplifyPG(pg, true, false);
								strategy = pg.copy("strategy");
								strategy_winCon = new Safety();
								strategy_winCon.buffer(strategy);
								return pg.getNet();
							}
						}
					}
				}
			}
			// There were no decision points for the system, thus the previous loop did not leave the method
			PGSimplifier.simplifyPG(pg, true, false);
			strategy = pg.copy("strategy");
			strategy_winCon = new Safety();
			strategy_winCon.buffer(strategy);
			return pg.getNet();
		}
		throw new NoStrategyExistentException();
	}
}