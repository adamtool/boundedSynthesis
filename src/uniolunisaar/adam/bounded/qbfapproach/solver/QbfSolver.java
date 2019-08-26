package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gnu.trove.set.hash.TIntHashSet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
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
	
	int[][] iff;

	public QbfSolver(PetriGame game, W winCon, QbfSolverOptions so) throws SolvingException {
		super(new QbfSolvingObject<>(game, winCon, false), so);
		
		// initializing bounded parameters n and b
		initializeNandB(so.getN(), so.getB());
		
		// initializing arrays for storing variable IDs
		initializeBeforeUnfolding(getSolvingObject().getN());
	}
	
	// TODO new alternative
	protected void writeFlowALTERNATIVE() throws IOException {
		writeIFF(1, getSolvingObject().getN());
		writeFired(1, getSolvingObject().getN());
		TIntHashSet or = new TIntHashSet();
		int[] fireOne = new int[getSolvingObject().getN() + 1];
		
		int size = getSolvingObject().getGame().getTransitions().size();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			or.clear();
			for (int j = 0; j < size; ++j) {
				or.add(oneTransitionFormulas[j][i]);
			}
			fireOne[i] = createUniqueID();
			writeOr(fireOne[i], or);
		}
		
		int orId;
		Set<Place> places = getSolvingObject().getGame().getPlaces();
		TIntHashSet and = new TIntHashSet();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			and.clear();
			for (Place p : places) {
				or.clear();
				for (Transition pre : p.getPreset()) {
					or.add(oneTransitionFormulas[transitionKeys.get(pre)][i]);
				}
				for (Transition post : p.getPostset()) {
					or.add(oneTransitionFormulas[transitionKeys.get(post)][i]);
				}
				
				or.add(iff[placeKeys.get(p)][i]);
				
				orId = createUniqueID();
				writeOr(orId, or);
				and.add(orId);
			}
			and.add(fireOne[i]);
			fl[i] = createUniqueID();
			writeAnd(fl[i], and);
		}
	}

	protected void writeFired(int s, int e) throws IOException {
		int size = getSolvingObject().getGame().getTransitions().size();
		Transition t;
		for (int j = 0; j < size; ++j) {
			t = transitions[j];
			
			TIntHashSet[] and = new TIntHashSet[e];
			for (int i = s; i < e; ++i) {
				and[i] = new TIntHashSet();
			}
			Set<Place> preset = t.getPreset();
			Set<Place> postset = t.getPostset();

			// pre(t)
			for (Place p : preset) {
				for (int i = s; i < e; ++i) {
					and[i].add(getVarNr(p.getId() + "." + i, true));
					int strat = addSysStrategy(p, t);
					if (strat != 0) {
						and[i].add(strat);
					}
				}
			}
			
			// post(t)
			for (Place p : postset) {
				for (int i = s; i < e; ++i) {
					and[i].add(getVarNr(p.getId() + "." + (i + 1), true));
				}
			}

			// pre(t) \ post(t)
			for (Place p : preset) {
				if (!t.getPostset().contains(p)) {
					for (int i = s; i < e; ++i) {
						and[i].add(-getVarNr(p.getId() + "." + (i + 1), true));
					}
				}
			}
			
			// post(post(pre(t))) \ post(t)
			Set<Place> pre = t.getPreset();
			Set<Place> post = t.getPostset();
			for (Place tpre : t.getPreset()) {
				for (Transition tprepost : tpre.getPostset()) {
					for (Place tprepostpost : tprepost.getPostset()) {
						if (!pre .contains(tprepostpost)) {
							if (!post.contains(tprepostpost)) {
								for (int i = s; i < e; ++i) {
									and[i].add(iff[placeKeys.get(tprepostpost)][i]);
								}
							}
						}
					}
				}
			}
			
			for (int i = s; i < e; ++i) {
				oneTransitionFormulas[j][i] = createUniqueID();
				writeAnd(oneTransitionFormulas[j][i], and[i]);
			}
		}
	}
	
	protected void writeIFF (int s, int e) throws IOException {
		iff = new int[getSolvingObject().getGame().getPlaces().size()][getSolvingObject().getN()];
		TIntHashSet and = new TIntHashSet();
		int p_i, p_iSucc, j;
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			for (int i = s; i < e; ++i) {
				and.clear();
				p_i = getVarNr(p.getId() + "." + i, true);
				p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
				and.add(writeImplication(p_i, p_iSucc));
				and.add(writeImplication(p_iSucc, p_i));
				j = placeKeys.get(p);
				iff[j][i] = createUniqueID();
				writeAnd(iff[j][i], and);
			}
		}
	}
	
	// TODO re-implement to iterate over places fewer times
	// TODO old implementation which is quadratic as P * T
	protected void writeFlow() throws IOException {
		writeTransitions(1, getSolvingObject().getN());
		TIntHashSet or = new TIntHashSet();
		Set<Place> places = getSolvingObject().getGame().getPlaces();
		
		int size = getSolvingObject().getGame().getTransitions().size();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			or.clear();
			for (int j = 0; j < size; ++j) {
				or.add(getOneTransition(transitions[j], i, places));
			}
			fl[i] = createUniqueID();
			writeOr(fl[i], or);
		}
		
		
		
		/*String[] flow = getFlow();
		for (int i = 1; i < getSolvingObject().getN(); ++i) {
			fl[i] = createUniqueID();
			writer.write(fl[i] + " = " + flow[i]);
		}*/

	}

	protected void writeTransitions(int s, int e) throws IOException {
		int size = getSolvingObject().getGame().getTransitions().size();
		Set<Place> places = getSolvingObject().getGame().getPlaces();
		Transition t;
		for (int j = 0; j < size; ++j) {
			t = transitions[j];
			
			TIntHashSet[] and = new TIntHashSet[e];
			for (int i = s; i < e; ++i) {
				and[i] = new TIntHashSet();
			}
			Set<Place> preset = t.getPreset();
			Set<Place> postset = t.getPostset();

			for (int i = s; i < e; ++i) {
				and[i].add(-deadlockSubFormulas[j][i]);
			}
			
			// post(t)
			for (Place p : postset) {
				for (int i = s; i < e; ++i) {
					and[i].add(getVarNr(p.getId() + "." + (i + 1), true));
				}
			}
			
			// places \ (pre(t) U post(t))
			int p_i;
			int p_iSucc;
			for (Place p : places) {
				if (!preset.contains(p) && !postset.contains(p)) {
					for (int i = s; i < e; ++i) {
						p_i = getVarNr(p.getId() + "." + i, true);
						p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
						and[i].add(writeImplication(p_i, p_iSucc));
						and[i].add(writeImplication(p_iSucc, p_i));
					}
				}
			}

			// pre(t) \ post(t)
			for (Place p : preset) {
				if (!t.getPostset().contains(p)) {
					for (int i = s; i < e; ++i) {
						and[i].add(-getVarNr(p.getId() + "." + (i + 1), true));
					}
				}
			}
			for (int i = s; i < e; ++i) {
				oneTransitionFormulas[j][i] = createUniqueID();
				writeAnd(oneTransitionFormulas[j][i], and[i]);
			}
		}
	}
	
	/*protected String[] getFlow() throws IOException {
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
	}*/
	
	protected int getOneTransition(Transition t, int i) throws IOException {
		return getOneTransition(t, i, getSolvingObject().getGame().getPlaces());
	}

	protected int getOneTransition(Transition t, int i, Set<Place> places) throws IOException {
		int transitionKey = transitionKeys.get(t);
		if (oneTransitionFormulas[transitionKeys.get(t)][i] == 0) {
			TIntHashSet and = new TIntHashSet();
			Set<Place> preset = t.getPreset();
			Set<Place> postset = t.getPostset();
			
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
			and.add(-deadlockSubFormulas[transitionKey][i]);
			
			// post(t)
			for (Place p : postset) {
				and.add(getVarNr(p.getId() + "." + (i + 1), true));
			}
			
			// places \ (pre(t) U post(t))
			int p_i;
			int p_iSucc;
			for (Place p : places) {
				if (!preset.contains(p) && !postset.contains(p)) {
					p_i = getVarNr(p.getId() + "." + i, true);
					p_iSucc = getVarNr(p.getId() + "." + (i + 1), true);
					and.add(writeImplication(p_i, p_iSucc));
					and.add(writeImplication(p_iSucc, p_i));
				}
			}

			// pre(t) \ post(t)
			for (Place p : preset) {
				if (!t.getPostset().contains(p)) {
					and.add(-getVarNr(p.getId() + "." + (i + 1), true));
				}
			}
			
			oneTransitionFormulas[transitionKey][i] = createUniqueID();
			writeAnd(oneTransitionFormulas[transitionKey][i], and);
			return oneTransitionFormulas[transitionKey][i];
		} else {
			return oneTransitionFormulas[transitionKey][i];
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
		Map<Integer, Set<Place>> slices;
		if (QbfControl.binaryPlaceEncoding && (slices = sliceable()) != null) { 					// TODO remove QbfControl boolean? Explicitly encode NOT varnr
			for (int slice : slices.keySet()) {
				Set<Place> places = slices.get(slice);
				int vars = (int) Math.ceil(Math.log(places.size() + 1)/Math.log(2));
				for (int j = 1; j <= vars; ++j) {
					for (int i = 1; i <= getSolvingObject().getN(); ++i) {
						int number = createVariable(slice + "." + j + "." + i);			// slice number . binary position (vars to 1) . 1 to n
						forall.add(number);
						// System.out.println(number + " = " + p.getId() + "." + i);
					}
				}
			}
			writer.write(writeForall(forall));
			writer.write("output(1)" + QbfControl.replaceAfterwardsSpaces + QbfControl.linebreak);
			for (int sliceNumber : slices.keySet()) {
				Set<Place> placesOfSlice = slices.get(sliceNumber);
				int binaryMax = (int) Math.ceil(Math.log(placesOfSlice.size() + 1)/Math.log(2));
				int counter = 1;
				for (Place p : placesOfSlice) {
					String binaryEncoding = Integer.toBinaryString(counter);
					@SuppressWarnings("unchecked")
					Set<Integer>[] variables = new Set[getSolvingObject().getN() + 1];
					for (int k = 1; k < getSolvingObject().getN() + 1; ++k) {
						variables[k] = new HashSet<>();
					}
					for (int j = 0; j < (binaryMax - binaryEncoding.length()); ++j) {
						for (int k = 1; k < getSolvingObject().getN() + 1; ++k) {
							variables[k].add(-getVarNr(sliceNumber + "." + (j + 1) + "." + k, true));
						}
					}
					for (int j = binaryMax - binaryEncoding.length(); j < binaryMax; ++j) {
						for (int k = 1; k < getSolvingObject().getN() + 1; ++k) {
							if (Character.getNumericValue(binaryEncoding.charAt(j - (binaryMax - binaryEncoding.length()))) > 0) {
								variables[k].add(getVarNr(sliceNumber + "." + (j + 1) + "." + k, true));
							} else {
								variables[k].add(-getVarNr(sliceNumber + "." + (j + 1) + "." + k, true));
							}
						}
					}
					
					for (int k = 1; k < getSolvingObject().getN() + 1; ++k) {
						int number = createVariable(p.getId() + "." + k);
						writer.write(number + " = " + writeAnd(variables[k]));
					}
					
					counter++;
				}
			}
		} else {
			for (Place p : getSolvingObject().getGame().getPlaces()) {
				for (int i = 1; i <= getSolvingObject().getN(); ++i) {
					int number = createVariable(p.getId() + "." + i);
					forall.add(number);
					// System.out.println(number + " = " + p.getId() + "." + i);
				}
			}
			writer.write(writeForall(forall));
		}
	}
	
	private Map<Integer, Set<Place>> sliceable() {
		Map<Integer, Set<Place>> slices = new HashMap<> ();
		for (Place p : getSolvingObject().getGame().getPlaces()) {
			boolean found = false;
			for (Pair<String, Object> ext : p.getExtensions()) {
				if (ext.getFirst().matches("token")) {
					int index = (int) ext.getSecond();
					Set<Place> set = slices.get(index);
					if (set == null) {
						set = new HashSet<> ();
					}
					set.add(p);
					slices.put(index, set);
					found = true;
					break;
				}
			}
			if (!found) {
				return null;
			}
		}
		return slices;
	}
}
