package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public class McMillianUnfolder extends Unfolder {

	// unfolded result Petri game and Petri net
	public QBFSolvingObject<? extends WinningCondition> unfolding;
	public PetriNet unfoldingNet;

	// Pair<Place, Transition>
	// Pair<Transition, Set<Place>>

	Set<Pair<String, String>> conflicts = new HashSet<>(); // TODO never added

	Set<Pair<Transition, Set<Place>>> closed = new HashSet<>();

	int counter = 0;

	public McMillianUnfolder(QBFSolvingObject<? extends WinningCondition> petriGame, Map<String, Integer> max) throws NotSupportedGameException {
		super(petriGame, max);

		unfoldingNet = new PetriNet(pn.getName() + "_unfolding");
		unfolding = new QBFSolvingObject<>(new PetriGame(unfoldingNet), petriGame.getWinCon()); // todo MG: funzt so?

		Marking initial = pn.getInitialMarking();
		for (Place p : pn.getPlaces()) {
			if (initial.getToken(p).getValue() == 1) {
				Place newP = unfoldingNet.createPlace(p.getId() + "_P_EMPTY");
				newP.setInitialToken(1);
				if (pg.getGame().getEnvPlaces().contains(p)) {
					unfolding.getGame().getEnvPlaces().add(newP);
				}
				for (Pair<String, Object> pair : p.getExtensions()) {
					newP.putExtension(pair.getFirst(), pair.getSecond());
				}
			} else if (initial.getToken(p).getValue() > 1) {
				throw new NetNotSafeException(p.getId(), "initial");
			}
		}
	}

	@Override
	public void createUnfolding() throws NetNotSafeException, NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		Set<Pair<Transition, Set<Place>>> possibleExtensions = possibleExtensions();
		while (true) {
			Pair<Transition, Set<Place>> extension = possibleExtensions.iterator().next();
			possibleExtensions.remove(extension);
			closed.add(extension);

			Transition newT = unfoldingNet.createTransition(extension.getFirst().getId() + "_T_" + counter++);
			for (Place pre : extension.getSecond()) {
				unfoldingNet.createFlow(pre, newT);
			}
			for (Place post : extension.getFirst().getPostset()) {
				Place newPost = unfoldingNet.createPlace(post.getId() + "_P_" + newT.getId());
				if (pg.getGame().getEnvPlaces().contains(post)) {
					unfolding.getGame().getEnvPlaces().add(newPost);
				}
				for (Pair<String, Object> pair : post.getExtensions()) {
					newPost.putExtension(pair.getFirst(), pair.getSecond());
				}
				unfoldingNet.createFlow(newT, newPost);
			}
			if (possibleExtensions.isEmpty()) {
				// POWERSET CANT HANDLE LARGER SETS8
				// if (unfoldingNet.getPlaces().size() <= 20) {
				possibleExtensions = possibleExtensions();
				// }
				if (possibleExtensions.isEmpty()) {
					break;
				}
			}
		}

		unfolding.setN(pg.getN());
		unfolding.setB(pg.getB());
		pg = unfolding;
		pn = unfoldingNet;
	}

	// TODO NAIVE AND STUPID implementation
	private Set<Pair<Transition, Set<Place>>> possibleExtensions() {
		Set<Pair<Transition, Set<Place>>> possibleExtensions = new HashSet<Pair<Transition, Set<Place>>>();
		// System.out.println(unfoldingNet.getPlaces().size());
		Set<Place> places = new HashSet<>(unfoldingNet.getPlaces());

		for (Place p : unfoldingNet.getPlaces()) {
			if (pn.getPlace(getOriginalPlaceId(p.getId())).getPostset().isEmpty()) {
				places.remove(p);
			}
		}

		for (Set<Place> element : myPowerset(places)) {
			Set<String> idPlaces = new HashSet<>();
			Set<String> preTrans = new HashSet<>();
			for (Place p : element) {
				// same place more than once in set
				if (idPlaces.contains(getOriginalPlaceId(p.getId()))) {
					idPlaces = null;
					break;
				}
				idPlaces.add(getOriginalPlaceId(p.getId()));
				preTrans.add(getPreTransitionForPlace(p.getId()));
			}
			if (idPlaces == null) {
				continue;
			}
			for (String id1 : preTrans) {
				for (String id2 : preTrans) {
					if (conflicts.contains(new Pair<>(getOriginalTransitionId(id1), getOriginalTransitionId(id2)))) {
						idPlaces = null;
						break;
					}
				}
				if (idPlaces == null) {
					break;
				}
			}
			if (idPlaces == null) {
				continue;
			}
			for (Transition t : pn.getTransitions()) {
				Set<String> prePlaces = new HashSet<>();
				for (Place pre : t.getPreset()) {
					prePlaces.add(pre.getId());
				}
				if (idPlaces.equals(prePlaces)) {
					if (!closed.contains(new Pair<>(t, element))) {
						possibleExtensions.add(new Pair<>(t, element));
					}
				}
			}
		}
		return possibleExtensions;
	}

	public static int max = 2;

	public static <T> Set<Set<T>> myPowerset(Set<T> originalSet) {
		Set<Set<T>> result = new HashSet<>();
		result.add(new HashSet<>());
		for (int i = 1; i <= max; ++i) {
			for (Set<T> elementResult : result) {
				Set<Set<T>> addList = new HashSet<>();
				for (T element : originalSet) {
					Set<T> add = new HashSet<>(elementResult);
					add.add(element);
					addList.add(add);
				}
				addList.addAll(result);
				result = addList;
			}
		}
		// System.out.println("REDUCED POWERSET SIZE: " + result.size());
		return result;
	}

	public static <T> Set<Set<T>> powerset(Set<T> originalSet) {
		Set<Set<T>> sets = new HashSet<Set<T>>();
		if (originalSet.isEmpty()) {
			sets.add(new HashSet<T>());
			return sets;
		}
		List<T> list = new ArrayList<T>(originalSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
		for (Set<T> set : powerset(rest)) {
			Set<T> newSet = new HashSet<T>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

	public static String getOriginalPlaceId(String id) {
		int index = id.indexOf("_P_");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}

	public static String getOriginalTransitionId(String id) {
		int index = id.indexOf("_T_");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}

	public static String getPreTransitionForPlace(String id) {
		int index = id.indexOf("_P_");
		if (index != -1) {
			id = id.substring(index + 3, id.length());
		}
		return id;
	}
}
