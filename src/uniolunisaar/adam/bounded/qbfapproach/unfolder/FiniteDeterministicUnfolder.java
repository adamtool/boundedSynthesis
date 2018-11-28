package uniolunisaar.adam.bounded.qbfapproach.unfolder;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFSolvingObject;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

/**
 * 
 * 27.11.2018 ONLY WORKS FOR FINITE NETS AND NEEDS QBFCONTROL MCMILLIAN ENABLED
 * copy original net and then build net in place new
 * Misc note: solvingObj -> PG -> PN
 * 
 * @author JHH
 *
 */

public class FiniteDeterministicUnfolder extends Unfolder {
	
	private QBFSolvingObject<? extends WinningCondition> originalSolvingObj;
	private PetriGame originalGame;
	
	public Queue<Pair<Marking, Integer>> queue = new LinkedList<>();
	public int counter = 0;
	
	public FiniteDeterministicUnfolder(QBFSolvingObject<? extends WinningCondition> petriGame, Map<String, Integer> max) throws NotSupportedGameException {
		super(petriGame, max);
		
		originalSolvingObj = new QBFSolvingObject<>(petriGame);
		originalGame = originalSolvingObj.getGame();
		
		for (Transition t : new HashSet<>(pn.getTransitions())) {
			pn.removeTransition(t);
		}
		
		for (Place p : new HashSet<>(pn.getPlaces())) {
			pn.removePlace(p);
		}
		
		Marking initial = originalGame.getInitialMarking();
		Marking initial_unfolding = new Marking(pn);
		for (Place p : originalGame.getPlaces()) {
			if (initial.getToken(p).getValue() == 1) {
				Place newP = pg.getGame().createPlace(p.getId() + "__" + counter++);
				newP.setInitialToken(1);
				if (originalGame.getEnvPlaces().contains(p)) {
					pg.getGame().getEnvPlaces().add(newP);
				}
				for (Pair<String, Object> pair : p.getExtensions()) {
					newP.putExtension(pair.getFirst(), pair.getSecond());
				}
				initial_unfolding.addToken(newP, 1);
			}
		}
		queue.add(new Pair<>(initial_unfolding, 1));
	}

	@Override
	protected void createUnfolding() throws NoSuitableDistributionFoundException, UnboundedException, FileNotFoundException {
		Pair<Marking, Integer> currentPair;
		while ((currentPair = queue.poll()) != null){
			Marking marking = currentPair.getFirst();
			int i = currentPair.getSecond();
			Set<String> ids = new HashSet<>();
			for (Place p : pn.getPlaces()) {
				if (marking.getToken(p).getValue() > 0) {
					ids.add(getOriginalPlaceId(p.getId()));
				}
			}
			
			for (Transition t : originalGame.getTransitions()) {
				Set<String> preset_ids = new HashSet<>();
				for (Place pre : t.getPreset()) {
					preset_ids.add(pre.getId());
				}
				if (ids.containsAll(preset_ids)) { // preset subseteq marking -> transition is enabled
					// fire existing transition
					boolean fired = false;
					for (Transition post : pn.getTransitions()) {
						if (getOriginalTransitionId(post.getId()).equals(t.getId()) && post.isFireable(marking)) {
							Marking nextMarking = new Marking(marking);
							nextMarking.fire(post);
							if (i + 1 < pg.getN()) {
								queue.add(new Pair<Marking, Integer>(nextMarking, i + 1));
							}
							fired = true;
						}
					}
					if (!fired) {
						// add copy of transition if not yet existent
						Set<Place> preset = new HashSet<>();
						for (Place pre : pn.getPlaces()) {
							if (marking.getToken(pre).getValue() > 0) {
								if (preset_ids.contains(getOriginalPlaceId(pre.getId()))) {
									preset.add(pre);
								}
							}
						}
						
						Transition newT = pg.getGame().createTransition(t.getId() + "__" + counter++);
						for (Place pre : preset) {
							pn.createFlow(pre, newT);
						}
						for (Place post : t.getPostset()) {
							Place newPost = pg.getGame().createPlace(post.getId() + "__" + counter++);
							if (originalGame.getEnvPlaces().contains(post)) {
								pg.getGame().getEnvPlaces().add(newPost);
							}
							for (Pair<String, Object> pair : post.getExtensions()) {
								newPost.putExtension(pair.getFirst(), pair.getSecond());
							}
							pn.createFlow(newT, newPost);
						}
						
						Marking nextMarking = new Marking(marking);
						nextMarking.fire(newT);
						if (i + 1 < pg.getN()) {
							queue.add(new Pair<Marking, Integer>(nextMarking, i + 1));
						}
					}
				}
			}
		}
	}
	
	public static String getOriginalPlaceId(String id) {
		int index = id.indexOf("__");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}
	
	public static String getOriginalTransitionId(String id) {
		int index = id.indexOf("__");
		if (index != -1) {
			id = id.substring(0, index);
		}
		return id;
	}
}
