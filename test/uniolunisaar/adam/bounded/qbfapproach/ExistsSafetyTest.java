package uniolunisaar.adam.bounded.qbfapproach;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFExistsSafetySolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.tools.Tools;

@Test
public class ExistsSafetyTest {

	@BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
        	System.setProperty("examplesfolder", "examples");
        }
    }
	
	@Test(timeOut = 1800 * 1000) // 30 min
	public void test() throws Exception {
		exp1emptyFL("jhh/myexample1", 3, 0, true);
		exp1trivialFL("jhh/myexample1", 3, 0, false);
		exp2trivialFL("jhh/myexample2", 3, 0, false);
		exp2choiceFL("jhh/myexample2", 3, 0, true);
	}
	
	private void exp1emptyFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/existssafety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        for (Transition t : pn.getTransitions()) {
        	fl.put(t, new HashSet<>());
        }
        oneTest(pn, n, b, result, fl);
	}
	
	private void exp1trivialFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/existssafety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        for (Transition t : pn.getTransitions()) {
        	Set<Pair<Place, Place>> set = new HashSet<>();
        	set.add(new Pair<>(pn.getPlace("E1"), pn.getPlace("E2")));
        	fl.put(t, set);
        }
        oneTest(pn, n, b, result, fl);
	}
	
	private void exp2trivialFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/existssafety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        
        Set<Pair<Place, Place>> set1 = new HashSet<>();
        set1.add(new Pair<>(pn.getPlace("E1"), pn.getPlace("E2")));
        fl.put(pn.getTransition("t1"), set1);
        
        Set<Pair<Place, Place>> set2 = new HashSet<>();
        set2.add(new Pair<>(pn.getPlace("E1"), pn.getPlace("E3")));
        fl.put(pn.getTransition("t2"), set2);
        
        oneTest(pn, n, b, result, fl);
	}
	
	private void exp2choiceFL(String str, int n, int b, boolean result) throws Exception {
		final String path = System.getProperty("examplesfolder") + "/existssafety/" + str + ".apt";
        PetriNet pn = Tools.getPetriNet(path);
        Map<Transition, Set<Pair<Place, Place>>> fl = new HashMap<>();
        Set<Pair<Place, Place>> set1 = new HashSet<>();
        set1.add(new Pair<>(pn.getPlace("E1"), pn.getPlace("E2")));
        fl.put(pn.getTransition("t1"), set1);
        Set<Pair<Place, Place>> set2 = new HashSet<>();
        
        fl.put(pn.getTransition("t2"), set2);
        oneTest(pn, n, b, result, fl);
	}
	
	private void oneTest(PetriNet pn, int n, int b, boolean result, Map<Transition, Set<Pair<Place, Place>>> fl) throws Exception {
        QBFExistsSafetySolver sol = new QBFExistsSafetySolver(pn, new Safety(), new QBFSolverOptions(n, b));
        sol.pg.setFl(fl);
        sol.existsWinningStrategy();	// calculate first, then output games, and then check for correctness
		// TODO put this to an appropriate place in code
		Tools.savePN2PDF("originalGame", sol.game.getNet(), false);
		Tools.savePN2PDF("unfolding", sol.unfolding.getNet(), false);
		if (sol.existsWinningStrategy()) {
			Tools.savePN2PDF("strategy", sol.getStrategy(), false);
		}
		Assert.assertEquals(sol.existsWinningStrategy(), result);
	}
}
