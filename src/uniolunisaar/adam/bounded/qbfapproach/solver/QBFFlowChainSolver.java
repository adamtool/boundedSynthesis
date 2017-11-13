package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.CouldNotFindSuitableWinningConditionException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.TokenFlow;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.logic.util.PetriGameAnnotator;

public abstract class QBFFlowChainSolver<W extends WinningCondition> extends QBFSolver<W> {

	protected QBFFlowChainSolver(QBFPetriGame game, W winCon, QBFSolverOptions options) throws BoundedParameterMissingException {
		super(game, winCon, options);
	}
	
	protected void setTokenFlow () throws CouldNotFindSuitableWinningConditionException, ParseException {
		PetriGameAnnotator.parseAndAnnotateTokenflow(pn);
		Map<Transition, Set<Pair<Place, Place>>> tfl = new HashMap<>();
        for (Transition t : pn.getTransitions()) {
            List<TokenFlow> list = AdamExtensions.getTokenFlow(t);
            Set<Pair<Place, Place>> set = new HashSet<>();
            for (TokenFlow tf : list) {
            	for (Place pre : tf.getPreset()) {
            		for (Place post : tf.getPostset()) {
            			set.add(new Pair<>(pre, post));
            		}
            	}
            }
        	tfl.put(t, set);
        }
        pg.setFl(tfl);
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
		int top, bot, id;
		for (Place p : pg.getNet().getPlaces()) {
			for (int i = 1; i <= pg.getN(); ++i) {
				top = getVarNr(p.getId() + "." + i + "." + true, true);
				bot = getVarNr(p.getId() + "." + i + "." + false, true);
				
				id = createVariable(p.getId() + "." + i + "." + "objective");
				writer.write(id + " = and("       + top + "," + "-" + bot + ")" + QBFSolver.linebreak);
				
				id = createVariable(p.getId() + "." + i + "." + "notobjective");
				writer.write(id + " = and(" + "-" + top + ","       + bot + ")" + QBFSolver.linebreak);
				
				id = createVariable(p.getId() + "." + i + "." + "empty");
				writer.write(id + " = and(" + "-" + top + "," + "-" + bot + ")" + QBFSolver.linebreak);
			}
		}
	}
	
	protected Set<Place> getIncomingTokenFlow(Transition t, Place p) {
		Set<Place> result = new HashSet<> ();
		for (Pair<Place, Place> pair : pg.getFl().get(t)) {
			if (pair.getSecond().equals(p)) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					result.add(pair.getFirst());
				}
			}
		}
		return result;
	}
	
	protected Set<Place> getOutgoingTokenFlow(Place p, Transition t) {
		Set<Place> result = new HashSet<> ();
		for (Pair<Place, Place> pair : pg.getFl().get(t)) {
			if (pair.getFirst().equals(p)) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					result.add(pair.getSecond());
				}
			}
		}
		return result;
	}
	
	protected int getAllObjectiveFlowChain(Place p, Transition t, int i, Set<Place> tokenFlow) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("allOBJECTIVEFlowChain" + p.getId() + "." + t.getId() + "." + i);
		if (result.getFirst()) {
			Set<Integer> and = new HashSet<>();
			for (Place pre : tokenFlow) {
				and.add(getVarNr(pre.getId() + "." + i + "." + "objective", true));
			}
			writer.write(result.getSecond() + " = " + writeAnd(and));
		}
		return result.getSecond();
	}
	
	protected int getOneNotObjectiveFlowChain(Place p, Transition t, int i, Set<Place> tokenflow) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("oneNOTOBJECTIVEFlowChain" + p.getId() + "." + t.getId() + "." + i);
		if (result.getFirst()) {
			Set<Integer> or = new HashSet<>();
			for (Place pre : tokenflow) {
				or.add(getVarNr(pre.getId() + "." + i + "." + "notobjective", true));
			}
			writer.write(result.getSecond() + " = " + writeOr(or));
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
	protected String getUnfair() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		Set<Integer> outerAnd = new HashSet<>();
		Set<Integer> innerOr = new HashSet<>();
		Set<Integer> innerAnd = new HashSet<>();
		
		for (int i = 1; i < pg.getN() - 1; ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				outerAnd.clear();
				for (Place p : pn.getPlaces()) {
					if (!p.getId().startsWith(additionalSystemName)) {
						int p_i_safe = getVarNr(p.getId() + "." + i + "." + "objective", true);
						int p_j_safe = getVarNr(p.getId() + "." + j + "." + "objective", true);
						outerAnd.add(writeImplication(p_i_safe, p_j_safe));
						outerAnd.add(writeImplication(p_j_safe, p_i_safe));
						
						int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "notobjective", true);
						int p_j_unsafe = getVarNr(p.getId() + "." + j + "." + "notobjective", true);
						outerAnd.add(writeImplication(p_i_unsafe, p_j_unsafe));
						outerAnd.add(writeImplication(p_j_unsafe, p_i_unsafe));
						
						int p_i_empty = getVarNr(p.getId() + "." + i + "." + "empty", true);
						int p_j_empty = getVarNr(p.getId() + "." + j + "." + "empty", true);
						outerAnd.add(writeImplication(p_i_empty, p_j_empty));
						outerAnd.add(writeImplication(p_j_empty, p_i_empty));
					}
				}
				innerOr.clear();
				for (Transition t : pn.getTransitions()) {
					innerAnd.clear();
					for (int k = i; k < j; ++k){
						for (Place p : t.getPreset()) {
							int id = createUniqueID();
							writer.write(id + " = or(" + getVarNr(p.getId() + "." + k + "." + "objective", true) + "," + getVarNr(p.getId() + "." + k + "." + "notobjective", true) + ")" + QBFSolver.linebreak);
							innerAnd.add(id);
							int strategy = addSysStrategy(p, t);
							if (strategy != 0) {
								innerAnd.add(strategy);
							}
							for (Transition tt : p.getPostset()) {
								innerAnd.add(-getOneTransition(tt, k));
							}
						}
					}
					int id = createUniqueID();
					writer.write(id + " = " + writeAnd(innerAnd));
					innerOr.add(id);
				}
				int id = createUniqueID();
				writer.write(id + " = " + writeOr(innerOr));
				outerAnd.add(id);
				id = createUniqueID();
				writer.write(id + " = " + writeAnd(outerAnd));
				outerOr.add(id);
			}
		}
		return writeOr(outerOr);
	}
	
	@Override
	protected String getLoopIJ() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < pg.getN(); ++i) {
			for (int j = i + 1; j <= pg.getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : pn.getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(additionalSystemName)) {
						int p_i_safe = getVarNr(p.getId() + "." + i + "." + "objective", true);
						int p_j_safe = getVarNr(p.getId() + "." + j + "." + "objective", true);
						and.add(writeImplication(p_i_safe, p_j_safe));
						and.add(writeImplication(p_j_safe, p_i_safe));
						int p_i_unsafe = getVarNr(p.getId() + "." + i + "." + "notobjective", true);
						int p_j_unsafe = getVarNr(p.getId() + "." + j + "." + "notobjective", true);
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
				or.clear();
				or.add(-getVarNr(p.getId() + "." + i + "." + true, true));
				or.add(-getVarNr(p.getId() + "." + i + "." + false, true));
				int id = createUniqueID();
				writer.write(id + " = " + writeOr(or));
				and.add(id);
			}
		}
		int returnValue = createUniqueID();
		writer.write(returnValue + " = " + writeAnd(and));
		return returnValue;
	}
	
	protected Set<Transition> getTransitionCreatingTokenFlow() {
		Set<Transition> result = new HashSet<>();
		for (Transition t : pn.getTransitions()) {
			for (Place p : t.getPostset()) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					if (getIncomingTokenFlow(t, p).isEmpty()) {
						result.add(t);
					}
				}
			}
		}
		return result;
	}
	protected Set<Transition> getTransitionFinishingTokenFlow() {
		Set<Transition> result = new HashSet<>();
		for (Transition t : pn.getTransitions()) {
			for (Place p : t.getPreset()) {
				if (!p.getId().startsWith(QBFSolver.additionalSystemName)) {
					if (getOutgoingTokenFlow(p, t).isEmpty()) {
						result.add(t);
					}
				}
			}
		}
		return result;
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
								strategy = new PetriGame(pg);
								return pg.getNet();
							}
						}
					}
				}
			}
			// There were no decision points for the system, thus the previous loop did not leave the method
			PGSimplifier.simplifyPG(pg, true, false);
			strategy = new PetriGame(pg);
			return pg.getNet();
		}
		throw new NoStrategyExistentException();
	}
}
