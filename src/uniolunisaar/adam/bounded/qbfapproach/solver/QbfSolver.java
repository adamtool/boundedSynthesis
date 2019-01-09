package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.QbfControl;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Condition;

/**
 *
 * @author Jesko Hecking-Harbusch
 *
 * @param <W>
 */
public abstract class QbfSolver<W extends Condition> extends SolverQbfAndQbfCon<W, QbfSolverOptions> {

	// caches for getOneTransition()
	private Map<Transition, Set<Place>> restCache = new HashMap<>(); // proven to be slightly useful in terms of performance
	private Map<Transition, Set<Place>> preMinusPostCache = new HashMap<>();

	public QbfSolver(PetriGame game, W winCon, QbfSolverOptions so) throws SolvingException {
		super(new QbfSolvingObject<>(game, winCon), so);
		
		// initializing bounded parameters n and b
		initializeNandB(so.getN(), so.getB());
		
		// initializing arrays for storing variable IDs
		initializeBeforeUnfolding(getSolvingObject().getN());
	}

	protected void writeFlow() throws IOException {
		String[] flow = getFlow();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			fl[i] = createUniqueID();
			writer.write(fl[i] + " = " + flow[i]);
		}

	}

	protected String[] getFlow() throws IOException {
		String[] flow = new String[getSolvingObject().getN() + 1];
		Set<Integer> or = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			or.clear();
			for (int j = 0; j < getSolvingObject().getGame().getTransitions().size(); ++j) {
				or.add(getOneTransition(transitions[j], i));
			}
			flow[i] = writeOr(or);
		}
		return flow;
	}

	protected int getOneTransition(Transition t, int i) throws IOException {
		if (oneTransitionFormulas[transitionKeys.get(t)][i] == 0) {
			Set<Integer> and = new HashSet<>();
			//old alternative:
			/*int strat;
			for (Place p : t.getPreset()) {
				and.add(getVarNr(p.getId() + "." + i, true));
				strat = addSysStrategy(p, t);
				if (strat != 0) {
					and.add(strat);
				}
			}
			*/
			//new alternative:
			and.add(-deadlockSubFormulas[transitionKeys.get(t)][i]);
			
			for (Place p : t.getPostset()) {
				and.add(getVarNr(p.getId() + "." + (i + 1), true));
			}
			// Slight performance gain by using these caches
			Set<Place> rest = restCache.get(t);
			if (rest == null) {
				rest = new HashSet<>(getSolvingObject().getGame().getPlaces());
				rest.removeAll(t.getPreset());
				rest.removeAll(t.getPostset());
				restCache.put(t, rest);
			}

			for (Place p : rest) {
				int p_i = getVarNr(p.getId() + "." + i, true);
				int p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
				and.add(writeImplication(p_i, p_iSucc));
				and.add(writeImplication(p_iSucc, p_i));
			}

			Set<Place> preMinusPost = preMinusPostCache.get(t);
			if (preMinusPost == null) {
				preMinusPost = new HashSet<>(t.getPreset());
				preMinusPost.removeAll(t.getPostset());
				preMinusPostCache.put(t, preMinusPost);
			}
			for (Place p : preMinusPost) {
				and.add(-getVarNr(p.getId() + "." + (i + 1), true));
			}
			int number = createUniqueID();
			writer.write(number + " = " + writeAnd(and));
			oneTransitionFormulas[transitionKeys.get(t)][i] = number;
			return number;
		} else {
			return oneTransitionFormulas[transitionKeys.get(t)][i];
		}

	}

	protected void writeSequence() throws IOException {
		Set<Integer> and = new HashSet<>();
		for (int i = 1; i <= getSolvingObject().getN(); ++i) {
			and.clear();
			and.add(in);
			for (int j = 1; j <= i - 1; ++j) {
				// and.add(-dl[j]); // performance evaluation showed that leaving this out makes program faster as it is redundant
				and.add(fl[j]);
			}
			seq[i] = createUniqueID();
			writer.write(seq[i] + " = " + writeAnd(and));
		}
	}

	protected void writeUnfair() throws IOException {
		String unfair = getUnfair();
		u = createUniqueID();
		writer.write(u + " = " + unfair);
	}

	protected String getUnfair() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (int j = i + 2; j <= getSolvingObject().getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
						int p_i = getVarNr(p.getId() + "." + i, true);
						int p_j = getVarNr(p.getId() + "." + j, true);
						and.add(writeImplication(p_i, p_j));
						and.add(writeImplication(p_j, p_i));
					}
				}
				Set<Integer> or = new HashSet<>();
				for (Transition t : getSolvingObject().getGame().getTransitions()) {
					Set<Integer> innerAnd = new HashSet<>();
					for (int k = i; k < j; ++k) {
						for (Place p : t.getPreset()) {
							innerAnd.add(getVarNr(p.getId() + "." + k, true));
							int sysDecision = addSysStrategy(p, t);
							if (sysDecision != 0) {
								innerAnd.add(sysDecision);
							}
							if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
								for (Transition tt : p.getPostset()) {
									innerAnd.add(-getOneTransition(tt, k));
								}
							}
						}
					}
					int innerAndNumber = createUniqueID();
					writer.write(innerAndNumber + " = " + writeAnd(innerAnd));
					or.add(innerAndNumber);
				}
				int orNumber = createUniqueID();
				writer.write(orNumber + " = " + writeOr(or));
				and.add(orNumber);
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				outerOr.add(andNumber);
			}
		}
		return writeOr(outerOr);
	}

	// this has one more quantifier alternation, it should therefore be slower
	protected String getUnfairMoreQuantifierAlternation() throws IOException {
		Set<Integer> outerOr = new HashSet<>();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			for (int j = i + 2; j <= getSolvingObject().getN(); ++j) {
				Set<Integer> and = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					// additional system places cannot leave their places, they always loop
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
						int p_i = getVarNr(p.getId() + "." + i, true);
						int p_j = getVarNr(p.getId() + "." + j, true);
						and.add(writeImplication(p_i, p_j));
						and.add(writeImplication(p_j, p_i));
					}
				}
				Set<Integer> or = new HashSet<>();
				for (Place p : getSolvingObject().getGame().getPlaces()) {
					// additional system places are not responsible for unfair loops, exclude them
					if (!p.getId().startsWith(QbfControl.additionalSystemName)) {
						Set<Integer> outerAnd = new HashSet<>();
						for (int k = i; k < j; ++k) {
							outerAnd.add(getVarNr(p.getId() + "." + k, true));
							Set<Integer> innerOr = new HashSet<>();
							for (Transition t : p.getPostset()) {
								Set<Integer> innerAnd = new HashSet<>();
								for (Place place : t.getPreset()) {
									innerAnd.add(getVarNr(place.getId() + "." + k, true));
									int strat = addSysStrategy(place, t);
									if (strat != 0) {
										innerAnd.add(strat);
									}
								}
								int innerAndNumber = createUniqueID();
								writer.write(innerAndNumber + " = " + writeAnd(innerAnd));
								innerOr.add(innerAndNumber);

								// outerAnd.add(-getOneTransition(t, k)); // with necessary expensive extension, this is redundant, but makes it faster
								// TODO this makes it correct but also expensive
								for (Place pp : t.getPreset()) {
									for (Transition tt : pp.getPostset()) {
										outerAnd.add(-getOneTransition(tt, k));
									}
								}
							}
							int innerOrNumber = createUniqueID();
							writer.write(innerOrNumber + " = " + writeOr(innerOr));
							outerAnd.add(innerOrNumber);
						}
						int outerAndNumber = createUniqueID();
						writer.write(outerAndNumber + " = " + writeAnd(outerAnd));
						or.add(outerAndNumber);
					}
				}
				int orNumber = createUniqueID();
				writer.write(orNumber + " = " + writeOr(or));
				and.add(orNumber);
				int andNumber = createUniqueID();
				writer.write(andNumber + " = " + writeAnd(and));
				outerOr.add(andNumber);
			}
		}
		return writeOr(outerOr);
	}

	protected void addExists() throws IOException {
		Set<Integer> exists = new HashSet<>();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			if (!getSolvingObject().getGame().isEnvironment(p)) {
				if (p.getId().startsWith(QbfControl.additionalSystemName)) {
					for (Transition t : p.getPostset()) {
						int number = createVariable(p.getId() + ".." + t.getId());
						exists.add(number);
						// System.out.println(number + " = " + p.getId() + ".." + t.getId());
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
							// System.out.println(number + " = " + p.getId() + ".." + truncatedID);
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
				// System.out.println(number + " = " + p.getId() + "." + i);
			}
		}
		writer.write(writeForall(forall));
		writer.write("output(1)" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak);
	}
}
