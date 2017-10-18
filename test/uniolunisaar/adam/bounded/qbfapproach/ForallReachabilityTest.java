package uniolunisaar.adam.bounded.qbfapproach;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFForallReachabilitySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.petrigame.TokenFlow;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.ds.winningconditions.Reachability;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.PetriGameAnnotator;
import uniolunisaar.adam.tools.Tools;

@Test
public class ForallReachabilityTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void test() throws Exception {
		//exp1emptyFL("jhh/myexample1", 3, 0, false);
		//exp1trivialFL("jhh/myexample1", 3, 0, true);
		//exp2emptyFL("jhh/myexample2", 3, 0, false);
		//exp2choiceFL("jhh/myexample2", 3, 0, true);
		/*burglar2("burglar/burglar", 10, 0, false);
		burglar2("burglar/burglar", 10, 2, true);
		burglar2("burglar/burglar1", 10, 0, false);
		burglar2("burglar/burglar1", 10, 2, true);
		burglar2("burglar/burglar2", 10, 0, false);
		burglar2("burglar/burglar2", 10, 2, false);*/
		//burglar2("burglar/burglarDirectlyWon", 10, 0, true);
		infiniteFlowChain("toyexamples/infiniteFlowChains", 20, 0, true);
	}
	
	private void exp1emptyFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        for (Transition t : pn.getTransitions()) {
        	fl.put(t, new HashSet<>());
        }
        oneTest(pn, n, b, result, fl);
	}
	
	private void exp1trivialFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        for (Transition t : pn.getTransitions()) {
        	Set<Pair<Place, Place>> set = new HashSet<>();
        	set.add(new Pair<>(pn.getPlace("E1"), pn.getPlace("E2")));
        	fl.put(t, set);
        }
        oneTest(pn, n, b, result, fl);
	}
	
	private void exp2emptyFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        for (Transition t : pn.getTransitions()) {
        	Set<Pair<Place, Place>> set = new HashSet<>();
        	fl.put(t, set);
        }
        oneTest(pn, n, b, result, fl);
	}
	
	private void exp2choiceFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        Set<Pair<Place, Place>> set1 = new HashSet<>();
        set1.add(new Pair<>(pn.getPlace("E1"), pn.getPlace("E2")));
        fl.put(pn.getTransition("t1"), set1);
        Set<Pair<Place, Place>> set2 = new HashSet<>();
        
        fl.put(pn.getTransition("t2"), set2);
        oneTest(pn, n, b, result, fl);
	}
	
	private void burglar2(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        PetriGameAnnotator.parseNetOptionsAndAnnotate(pn);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
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
        	fl.put(t, set);
        }
        oneTest(pn, n, b, result, fl);
	}
	
	private void infiniteFlowChain(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/forallreachability/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        PetriGameAnnotator.parseNetOptionsAndAnnotate(pn);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
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
        	fl.put(t, set);
        }
        oneTest(pn, n, b, result, fl);
	}
	
	private void oneTest(PetriNet pn, int n, int b, boolean result, Map<Transition, Set<Pair<Place, Place>>> fl) throws Exception {
        QBFForallReachabilitySolver sol = new QBFForallReachabilitySolver(new QBFPetriGame(pn), new Reachability(), new QBFSolverOptions(n, b));
        sol.pg.setFl(fl);
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		// TODO put this to an appropriate place in code
		AdamTools.savePG2PDF("originalGame", sol.game.getNet(), false);
		AdamTools.savePG2PDF("unfolding", sol.unfolding.getNet(), false);
		if (sol.existsWinningStrategy()) {
			AdamTools.savePG2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
