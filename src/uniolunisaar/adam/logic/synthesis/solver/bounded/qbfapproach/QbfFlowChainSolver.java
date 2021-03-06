package uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach;

import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolverOptions;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolvingObject;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.exceptions.CalculationInterruptedException;
import uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.QbfControl;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 */

public abstract class QbfFlowChainSolver<W extends Condition<W>> extends QbfSolver<W> {

	protected QbfFlowChainSolver(QbfSolvingObject<W> solObj, QbfSolverOptions options) throws SolvingException {
		super(solObj, options);
	}

	protected void setTokenFlow() {
		Map<Transition, Set<Pair<Place, Place>>> tfl = new HashMap<>();
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			Collection<Transit> list = getSolvingObject().getGame().getTransits(t);
			Set<Pair<Place, Place>> set = new HashSet<>();
			for (Transit tf : list) {
//				for (Place pre : tf.getPreset()) {
                    Place pre = tf.getPresetPlace();
                    if (pre != null) {
	                    for (Place post : tf.getPostset()) {
							set.add(new Pair<>(pre, post));
						}
                    }
//				}
			}
			tfl.put(t, set);
		}
		getSolvingObject().setFl(tfl);
	}

	@Override
	protected void addForall() throws IOException {
		Set<Integer> forall = new HashSet<>();
		int id;
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			for (Place p : getSolvingObject().getGame().getPlaces()) {
				id = createVariable(p.getId() + "." + i + "." + true);
				forall.add(id);
				id = createVariable(p.getId() + "." + i + "." + false);
				forall.add(id);
			}
		}
		writer.write(writeForall(forall));
		writer.write("output(1)" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak);
		makeThreeValuedLogic();
	}

	private void makeThreeValuedLogic() throws IOException {
		int top, bot, id;
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			for (Place p : getSolvingObject().getGame().getPlaces()) {
				top = getVarNr(p.getId() + "." + i + "." + true, true);
				bot = getVarNr(p.getId() + "." + i + "." + false, true);

				id = createVariable(p.getId() + "." + i + "." + "objective");
				writer.write(id + " = and(" + top + "," + "-" + bot + ")" + QbfControl.linebreak);
				// System.out.println(p.getId() + "." + i + "." + "objective" + " -> " + "and(" + top + "," + "-" + bot + ")");

				id = createVariable(p.getId() + "." + i + "." + "notobjective");
				writer.write(id + " = and(" + "-" + top + "," + bot + ")" + QbfControl.linebreak);
				// System.out.println(p.getId() + "." + i + "." + "notobjective" + " -> " + "and(" + -top + "," + bot + ")");

				id = createVariable(p.getId() + "." + i + "." + "empty");
				writer.write(id + " = and(" + "-" + top + "," + "-" + bot + ")" + QbfControl.linebreak);
				// System.out.println(p.getId() + "." + i + "." + "empty" + " -> " + "and(" + -top + "," + -bot + ")");
			}
		}
	}

	protected Set<Place> getIncomingTokenFlow(Transition t, Place p) {
		Set<Place> result = new HashSet<>();
		Map<Transition, Set<Pair<Place, Place>>> map = getSolvingObject().getFl();
		for (Pair<Place, Place> pair : map.get(t)) {
			if (pair.getSecond().equals(p)) {
				if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
					result.add(pair.getFirst());
				}
			}
		}
		return result;
	}

	protected Set<Place> getOutgoingTokenFlow(Place p, Transition t) {
		Set<Place> result = new HashSet<>();
        Map<Transition, Set<Pair<Place, Place>>> map = getSolvingObject().getFl();
		for (Pair<Place, Place> pair : map.get(t)) {
			if (pair.getFirst().equals(p)) {
				if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
					result.add(pair.getSecond());
				}
			}
		}
		return result;
	}

	protected int getAllObjectiveFlowChain(Place p, Transition t, int i, Set<Place> tokenFlow) throws IOException {
		Pair<Boolean, Integer> result = getVarNrWithResult("allOBJECTIVEFlowChain" + p.getId() + "." + t.getId() + "." + i);
		if (result.getFirst()) {
			Collection<Transit> list = getSolvingObject().getGame().getTransits(t);
			for (Transit tfl : list) {
				if (tfl.getPostset().contains(p) && tfl.isInitial()) {
					Pair<Boolean, Integer> or = getVarNrWithResult("or()");
					if (or.getFirst()) {
						writer.write(or.getSecond() + " = or()" + QbfControl.linebreak);
					}
					return or.getSecond();
				}
			}

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
			Collection<Transit> list = getSolvingObject().getGame().getTransits(t);
			for (Transit tfl : list) {
				if (tfl.getPostset().contains(p) && tfl.isInitial()) {
					Pair<Boolean, Integer> and = getVarNrWithResult("and()");
					if (and.getFirst()) {
						writer.write(and.getSecond() + " = and()" + QbfControl.linebreak);
					}
					return and.getSecond();
				}
			}

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
		Set<Integer> or = new HashSet<>();
		int number;
		int strat;
		for (int i = s; i <= e; ++i) {
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
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
				deadlockSubFormulas[transitionKeys.get(t)][i] = number;
			}
		}
	}

	@Override
	protected void writeTerminatingSubFormulas(int s, int e) throws IOException {
		Set<Integer> or = new HashSet<>();
		Set<Place> pre;
		int key;
		for (int i = s; i <= e; ++i) {
			for (Transition t : getSolvingObject().getGame().getTransitions()) {
				pre = t.getPreset();
				or.clear();
				for (Place p : pre) {
					or.add(getVarNr(p.getId() + "." + i + "." + "empty", true));
				}
				key = createUniqueID();
				writer.write(key + " = " + writeOr(or));
				terminatingSubFormulas[transitionKeys.get(t)][i] = key;
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

		for (int i = 1; i < getSolvingObject().getN() - 1; ++i) {
			for (int j = i + 2; j <= getSolvingObject().getN(); ++j) {
				outerAnd.clear();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
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
				for (Transition t : getSolvingObject().getGame().getTransitions()) {
					innerAnd.clear();
					// search for transition enabled the whole time but never fired
					for (int k = i; k < j; ++k) {
						for (Place p : t.getPreset()) {
							int id = createUniqueID();
							writer.write(id + " = or(" + getVarNr(p.getId() + "." + k + "." + "objective", true) + "," + getVarNr(p.getId() + "." + k + "." + "notobjective", true) + ")" + QbfControl.linebreak);
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
	protected void writeLoop() throws IOException {
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (int j = i + 1; j <= getSolvingObject().getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
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
		l = createUniqueID();
		writeOr(l, or);
	}

	// TODO unclear whether this helps
	protected int valid() throws IOException {
		Set<Integer> and = new HashSet<>();
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			for (Place p : getSolvingObject().getGame().getPlaces()) {
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
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			for (Place p : t.getPostset()) {
				if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
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
		for (Transition t : getSolvingObject().getGame().getTransitions()) {
			for (Place p : t.getPreset()) {
				if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
					if (getOutgoingTokenFlow(p, t).isEmpty()) {
						result.add(t);
					}
				}
			}
		}
		return result;
	}

	@Override
	protected PetriGameWithTransits calculateStrategy() throws NoStrategyExistentException, CalculationInterruptedException {
		return calculateStrategy(false);
	}
}
