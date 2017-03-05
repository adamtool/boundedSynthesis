package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.LinkedTransferQueue;

import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import uniolunisaar.adam.ds.exceptions.CouldNotFindSuitableWinningConditionException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.util.Tools;

public class QBFSafetySolver extends QBFSolver<Safety> {
    // Results

    boolean solvable = false;
    boolean error = false;
    PetriNet strat = null;

    // Parameters
    boolean orversion = true;		// true -> mostly BEST performance
    boolean newLoop = true;			// true -> way to go

    PetriGame pg;
    boolean bunfolded = false;
    int copycounter = 0;
    int b = 1;
    Map<Integer, String> strategy = new HashMap<>();
    Map<Place, Transition> copyCausing = new HashMap<>();		// for each copied place, which transition was changed
    Map<String, Set<Place>> copyStore = new HashMap<>();

    public QBFSafetySolver(PetriNet net) throws UnboundedPGException {
        this(net, 0, 0);
    }

    public QBFSafetySolver(PetriNet net, int n, int b2) throws UnboundedPGException {
        super(new QBFPetriGame(net, n, b2), new Safety());
        this.pg = super.getGame();
        for (Place p : pg.getNet().getPlaces()) {
            if (p.getPreset().size() == 1) {
                copyCausing.put(p, p.getPreset().iterator().next());
            }
        }

        // TODO try solving
        try {
            transformIntoBC(n, b);
        } catch (NetNotSafeException | IOException | InterruptedException | NoSuitableDistributionFoundException e) {
            e.printStackTrace();
        }
    }

    public void createBUnfolding(int b) throws NetNotSafeException,
            NoSuitableDistributionFoundException {
        Map<String, Integer> bvalues = new HashMap<>();
        for (Place p : pg.getNet().getPlaces()) {
            bvalues.put(p.getId(), b);
        }
        this.b = b;
        createBUnfolding(bvalues);
    }

    public void createBUnfolding(Map<String, Integer> b)
            throws NetNotSafeException, NoSuitableDistributionFoundException {

        Queue<Place> openIn = new LinkedTransferQueue<>();
        Queue<Place> open = new LinkedTransferQueue<>();
        Set<Place> closed = new HashSet<>();

        // Start with initial places of the system
        Marking in = pg.getNet().getInitialMarking();
        for (Place p : pg.getNet().getPlaces()) {
            if (in.getToken(p).getValue() == 1) {
                openIn.add(p);
            }
        }
        Set<Place> badPlaces = getWinningCondition().getBadPlaces();
        while (!openIn.isEmpty()) {
            Place p = openIn.poll();
            if (!p.getPreset().isEmpty()) {					// We should copy
                if (b.get(p.getId()) > 1) {					// We can copy
                    // Copy place
                    Place newP = pg.getNet().createPlace(p.getId() + "__" + b.get(p.getId()));
                    b.put(p.getId(), b.get(p.getId()) - 1);

                    // Copy also whether env and bad
                    if (badPlaces.contains(p)) {
                        newP.putExtension("bad", true);
                        badPlaces.add(newP);
                    }
                    if (pg.getEnvPlaces().contains(p)) {
                        newP.putExtension("env", true);
                        pg.getEnvPlaces().add(newP);
                    }
                    // TODO old copyStore; do I need this?
                    Set<Place> added = new HashSet<>();
                    added.add(p);
                    added.add(newP);
                    copyStore.put(p.getId(), added);

                    open.add(newP);

                    // Change incoming arrows
                    for (Transition pre : p.getPreset()) {
                        pg.getNet().removeFlow(pre, p);
                        pg.getNet().createFlow(pre, newP);
                        copyCausing.put(newP, pre);
                    }

                    // Copy outgoing arrows
                    for (Transition post : p.getPostset()) {
                        Transition newT = pg.getNet().createTransition(post.getId()
                                + "__" + (++copycounter));
                        for (Place c : post.getPreset()) {
                            if (c.equals(p)) {
                                pg.getNet().createFlow(newP, newT);
                            } else {
                                pg.getNet().createFlow(c, newT);
                            }
                        }
                        for (Place c : post.getPostset()) {
                            if (c.equals(p)) {				// remove self transitions
                                pg.getNet().createFlow(newT, newP);
                                // newP is already added
                            } else {
                                pg.getNet().createFlow(newT, c);
                                open.add(c);
                            }
                        }
                    }
                }
            }
            closed.add(p);
            for (Transition t : p.getPostset()) {
                for (Place post : t.getPostset()) {
                    if (!closed.contains(post)) {
                        open.add(post);
                    }
                }
            }
        }

        while (!open.isEmpty()) {
            Place p = open.poll();
            String id = p.getId();
            int index = id.indexOf("__");
            if (index != -1) {
                id = id.substring(0, index);
            }
            if (p.getPreset().size() > 1 && p.getPostset().size() > 0) {	// We should copy
                if (b.get(id) > 1) {										// We can copy
                    // Copy place
                    Place newP = pg.getNet().createPlace(id + "__" + b.get(id));
                    b.put(id, b.get(id) - 1);

                    // Copy also env and bad
                    if (badPlaces.contains(p)) {
                        newP.putExtension("bad", true);
                        badPlaces.add(newP);
                    }
                    if (pg.getEnvPlaces().contains(p)) {
                        newP.putExtension("env", true);
                        pg.getEnvPlaces().add(newP);
                    }

                    // TODO copyStore
                    if (copyStore.get(id) != null) {
                        Set<Place> s = copyStore.get(id);
                        s.add(newP);
                        copyStore.put(id, s);
                    } else {
                        Set<Place> added = new HashSet<>();
                        added.add(p);
                        added.add(newP);
                        copyStore.put(id, added);
                    }

                    open.add(newP);

                    int i = 0;
                    int n = p.getPreset().size();
                    boolean skip = true;
                    for (Transition pre : p.getPreset()) {	// Change incoming arrows
                        i++;
                        if ((skip && i == n) || (copyCausing.get(p) != null && copyCausing.get(p).equals(pre))) {
                            skip = false;
                        } else {
                            pg.getNet().removeFlow(pre, p);
                            pg.getNet().createFlow(pre, newP);
                            copyCausing.put(newP, pre);
                        }
                    }
                    for (Transition post : p.getPostset()) {// Copy outgoing arrows
                        // Get original ID of transition if we copy a copied transition again 
                        String trans_id = post.getId();
                        int trans_index = trans_id.indexOf("__");
                        if (index != -1) {
                            trans_id = trans_id.substring(0, trans_index);
                        }

                        Transition newT = pg.getNet().createTransition(trans_id
                                + "__" + (++copycounter));
                        for (Place c : post.getPreset()) {
                            if (c.equals(p)) {
                                pg.getNet().createFlow(newP, newT);
                            } else {
                                pg.getNet().createFlow(c, newT);
                            }
                        }
                        for (Place c : post.getPostset()) {
                            if (c.equals(p)) // remove self transitions
                            {
                                pg.getNet().createFlow(newT, newP);
                            } else {
                                pg.getNet().createFlow(newT, c);
                                open.add(c);
                            }
                        }
                    }
                } else {
                    String p_id = p.getId();
                    int p_int = p.getId().indexOf("__");
                    if (p_int != -1) {
                        p_id = p.getId().substring(0, p_int);
                    }
                    if (b.get(p_id) == 1) {
                        if (pg.getEnvPlaces().contains(p)) {

                            b.put(p_id, 0);
                        } else {
                            if (this.b > 1 && copyStore.get(p_id) != null) {
                                for (Transition t : p.getPreset()) {
                                    if (!t.getPreset().contains(p)) {
                                        for (Place copy : copyStore.get(p_id)) {
                                            String trans_id = t.getId();
                                            int trans_index = trans_id.indexOf("__");
                                            if (trans_index != -1) {
                                                trans_id = trans_id.substring(0, trans_index);
                                            }
                                            Transition newT = pg.getNet().createTransition(trans_id
                                                    + "__" + (++copycounter));

                                            for (Place pre : t.getPreset()) {
                                                pg.getNet().createFlow(pre, newT);
                                            }
                                            for (Place post : t.getPostset()) {

                                                String post_id = post.getId();
                                                int post_int = post.getId().indexOf("__");
                                                if (post_int != -1) {
                                                    post_id = post.getId().substring(0, post_int);
                                                }

                                                if (post_id.equals(p_id)) {
                                                    pg.getNet().createFlow(newT, copy);
                                                } else {
                                                    pg.getNet().createFlow(newT, post);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            b.put(p_id, 0);
                        }
                    }
                }
            }
            closed.add(p);
            for (Transition t : p.getPostset()) {
                for (Place post : t.getPostset()) {
                    if (!closed.contains(post)) {
                        open.add(post);
                    }
                }
            }
        }

        // old
        /*Queue<Place> openIn = new LinkedTransferQueue<Place> ();
         Queue<Place> open   = new LinkedTransferQueue<Place> ();
         Set<Place> closed   = new HashSet<Place> ();
         Marking in = pg.getNet().getInitialMarkingCopy();
         // Start with initial places of the system
         for (Place p : pg.getNet().getPlaces()) {
         if (in.getToken(p).getValue() == 1)
         openIn.add(p);
         }
		
         while(!openIn.isEmpty()) {
         Place p = openIn.poll();
         if (!p.getPreset().isEmpty()) {					// We must copy
         if (b.get(p.getId()) > 1) {					// We can copy
					
         Place newP = pg.getNet().createPlace(p.getId() + "__" + b.get(p.getId()));
         Set<Place> added = new HashSet<Place> ();
         added.add(newP);
         copyStore.put(p.getId(), added);
         if (pg.getBadPlaces().contains(p))
         newP.putExtension("bad", true);
         b.put(p.getId(), b.get(p.getId()) - 1);
					
         for (Transition pre : p.getPreset()) {	// Change incoming arrows
         pg.getNet().removeFlow(pre, p);
         pg.getNet().createFlow(pre, newP);
         copyCausing.put(newP, pre);
         }
         for (Transition post : p.getPostset()) {// Copy outgoing arrows
         Transition newT = pg.getNet().createTransition(post.getId()
         + "__" + (++copycounter));
         for (Place c : post.getPreset()) {
         if (c.equals(p))
         pg.getNet().createFlow(newP, newT);
         else
         pg.getNet().createFlow(c, newT);
         }
         for (Place c : post.getPostset()) {
         if (c.equals(p))				// remove self transitions
         pg.getNet().createFlow(newT, newP);
         else {
         pg.getNet().createFlow(newT, c);
         open.add(c);
         }
         }
         }
         closed.add(p);
         open.add(newP);
         }
         } else {
         for (Transition t : p.getPostset()) {
         for (Place p2 : t.getPostset()) {
         if (p2.getPreset().size() > 1)
         open.add(p2);
         }
         }
         }
         }
		
         while(!open.isEmpty()) {
         Place p = open.poll();
         String id = p.getId();
         int index = id.indexOf("__");
         if (index != -1) {
         id = id.substring(0, index);
         }
			
         if (p.getPreset().size() > 1 && p.getPostset().size() > 0) {				// We must copy and should copy
         if (b.get(id) > 1) {					// We can copy
         Place newP = pg.getNet().createPlace(id + "__" + b.get(id));
         if (pg.getBadPlaces().contains(p))
         newP.putExtension("bad", true);
         b.put(id, b.get(id) - 1);
					
         if (copyStore.get(id) != null) {
         Set<Place> added = copyStore.get(id);
         added.add(newP);
         copyStore.put(id, added);
         }
         int i = 0;
         int n = p.getPreset().size();
         boolean skip = true;
         for (Transition pre : p.getPreset()) {	// Change incoming arrows
         i++;
         if ((skip && i == n) || (copyCausing.get(p) != null && copyCausing.get(p).equals(pre))) {
         skip = false;
         } else {
         pg.getNet().removeFlow(pre, p);
         pg.getNet().createFlow(pre, newP);
         copyCausing.put(newP, pre);
         }
         }
         for (Transition post : p.getPostset()) {// Copy outgoing arrows
         Transition newT = pg.getNet().createTransition(post.getId()
         + "__" + (++copycounter));
         for (Place c : post.getPreset()) {
         if (c.equals(p))
         pg.getNet().createFlow(newP, newT);
         else
         pg.getNet().createFlow(c, newT);
         }
         for (Place c : post.getPostset()) {
         if (c.equals(p))				// remove self transitions
         pg.getNet().createFlow(newT, newP);
         else {
         pg.getNet().createFlow(newT, c);
         open.add(c);
         }
         }
         }
         closed.add(p);
         open.add(newP);
         } else {
         if (this.b > 1 && copyStore.get(p) != null) {
         for (Transition t : p.getPreset()) {
         for (Place copy : copyStore.get(p)) {
         Transition newT = pg.getNet().createTransition(t.getId()
         + "__" + (++copycounter));
								
         int p_int = p.getId().indexOf("__");
         String p_id = p.getId().substring(0, p_int);
								
         for (Place pre : t.getPreset()) {
         pg.getNet().createFlow(pre, newT);
         }
         for (Place post : t.getPostset()) {
         int post_int = post.getId().indexOf("__");
         String post_id = post.getId().substring(0, post_int);
         if (post_id.equals(p_id)){
         pg.getNet().createFlow(newT, copy);
         } else {
         pg.getNet().createFlow(newT,post);
         }
         }
         }
         }
         }
         }
         }
         }*/
        // oldest
        /*if (bunfolded) {
         System.out.println("Error: only unfold once");
         } else {
         bunfolded = true;
         Queue<String> recheck = new LinkedTransferQueue<String>();
         for (String p : b.keySet()) {
         recheck.add(p);
         }
         while (!recheck.isEmpty()) {
         String p = recheck.poll();
         if (p.contains("__")) {
         int index = p.lastIndexOf("__");
         String old = p;
         p = p.substring(0, index);
         if (pg.getNet().getPlace(p).getPostset().size() > 0
         && b.get(p) > 1) {
         boolean first = false; // no need to copy first
         // occurrence
         for (Transition t : pg.getNet().getTransitions()) {
         if (t.getPostset().contains(
         pg.getNet().getPlace(old))
         && b.get(p) > 1) {
         if (first) {
         Place newP = pg.getNet().createPlace(
         old + "__" + b.get(p));
         if (pg.getBadPlaces().contains(
         pg.getNet().getPlace(old)))
         newP.putExtension("bad", true);
         pg.getNet().removeFlow(t.getId(), old);
         pg.getNet().createFlow(t, newP);
         for (Transition trans : pg.getNet()
         .getTransitions()) {
         if (trans.getPreset().contains(
         pg.getNet().getPlace(p))) {
         Transition newT = pg
         .getNet()
         .createTransition(
         trans.getId()
         + (++copycounter));
         for (Place place : trans
         .getPostset()) {
         pg.getNet().createFlow(newT,
         place);
         if (!pg.getEnvPlaces()
         .contains(place))
         recheck.add(place.getId());
         }
         for (Place place : trans
         .getPreset()) {
         if (place.getId().equals(p))
         pg.getNet().createFlow(
         newP, newT);
         else
         pg.getNet().createFlow(
         place, newT);
         }
         }
         }
         b.put(p, b.get(p) - 1);
         } else {
         first = true;
         }
         }
         }
         }
         continue;
         }
         if (pg.getNet().getPlace(p).getPostset().size() > 0
         && b.get(p) > 1) {
         boolean first = false; // no need to copy first occurrence
         for (Transition t : pg.getNet().getTransitions()) {
         if (t.getPostset().contains(pg.getNet().getPlace(p))
         && b.get(p) > 1) {
         if (first) {
         Place newP = pg.getNet().createPlace(
         p + "__" + b.get(p));
         if (pg.getBadPlaces().contains(
         pg.getNet().getPlace(p)))
         newP.putExtension("bad", true);
         pg.getNet().removeFlow(t.getId(), p);
         pg.getNet().createFlow(t, newP);
         for (Transition trans : pg.getNet()
         .getTransitions()) {
         if (trans.getPreset().contains(
         pg.getNet().getPlace(p))) {
         Transition newT = pg
         .getNet()
         .createTransition(
         trans.getId()
         + (++copycounter));
         for (Place place : trans.getPostset()) {
         pg.getNet().createFlow(newT, place);
         if (!pg.getEnvPlaces().contains(
         place))
         recheck.add(place.getId());
         }
         for (Place place : trans.getPreset()) {
         if (place.getId().equals(p))
         pg.getNet().createFlow(newP,
         newT);
         else
         pg.getNet().createFlow(place,
         newT);
         }
         }
         }
         b.put(p, b.get(p) - 1);
         } else {
         first = true;
         }
         }
         }
         }
         }
         }*/
    }

    public String getInitial() throws NetNotSafeException {
        String initial;
        Marking initialMarking = pg.getNet().getInitialMarking();
        if (pg.getNet().getPlaces().size() >= 2) {
            // At least two places
            initial = "AND(";
            for (Place p : pg.getNet().getPlaces()) {
                if (initialMarking.getToken(p).getValue() > 1) {
                    throw new NetNotSafeException(p.toString(), "initially");
                }
                if (initialMarking.getToken(p).getValue() == 1) {
                    // First element has no "," in front.
                    if (initial.length() > 4) {
                        initial += "," + p.getId() + 1;
                    } else {
                        initial += p.getId() + 1;
                    }
                } else if (initial.length() > 4) {
                    initial += ",!" + p.getId() + 1;
                } else {
                    initial += "!" + p.getId() + 1;
                }
            }
            initial += ")";
            return initial;
        } else if (pg.getNet().getPlaces().size() == 1) { // no AND(...) around
            // single place
            // Exactly one place
            for (Place p : pg.getNet().getPlaces()) {
                if (initialMarking.getToken(p).getValue() > 1) {
                    throw new NetNotSafeException(p.toString(), "initially");
                }
                if (initialMarking.getToken(p).getValue() == 1) {
                    initial = p.getId() + 1;
                } else {
                    initial = "!" + p.getId() + 1;
                }
                return initial;
            }
        }
        // No places
        return ""; // TODO throw an error for Petri game without places?!
    }

    public String[] getDeadlock(int n) {
        String formulalist = "";
        String[] deadlock = new String[n + 1];
        for (int i = 1; i <= n; ++i) {
            if (pg.getNet().getTransitions().size() >= 2) {
                deadlock[i] = "AND(";
            } else {
                deadlock[i] = "";
            }
            for (Transition t : pg.getNet().getTransitions()) {
                Set<Place> pre = t.getPreset();
                if (pre.size() >= 2
                        || (pre.size() == 1 && !pg.getEnvPlaces().contains(
                                pre.toArray()[0]))) // Synchronous transition or local transition enabled by a
                // single system player
                {
                    formulalist = "OR(";
                } else {
                    formulalist = "";
                }
                for (Place p : pre) {
                    if (formulalist.length() > 3) {
                        formulalist += ",!" + p.getId() + i;
                    } else {
                        formulalist += "!" + p.getId() + i;
                    }
                    if (!pg.getEnvPlaces().contains(p)) { // TODO expensive
                        // maybe
                        formulalist += ",!" + p.getId() + ".." + t.getId(); // System
                        // decision
                        // for
                        // place
                        // p
                        // and
                        // transition
                        // t
                        // labeled
                        // by
                        // "p..t"
                    }
                }
                if (pre.size() >= 2
                        || (pre.size() == 1 && !pg.getEnvPlaces().contains(
                                pre.toArray()[0]))) {
                    formulalist += ")";
                }
                if (deadlock[i].length() > 4) {
                    deadlock[i] += "," + formulalist;
                } else {
                    deadlock[i] += formulalist;
                }

            }
            if (pg.getNet().getTransitions().size() >= 2) {
                deadlock[i] += ")";
            }
            // System.out.println(deadlock[i]);
        }
        return deadlock;
    }

    private String getOneTransition(Transition t, int i) {
        String formula = "AND(";
        for (Place p : t.getPreset()) {
            if (formula.length() > 4) {
                formula += "," + p.getId() + i;
            } else {
                formula += p.getId() + i;
            }
            if (!pg.getEnvPlaces().contains(p)) { // TODO expensive
                formula += "," + p.getId() + ".." + t.getId();
            }
        }
        for (Place p : t.getPostset()) {
            if (formula.length() > 4) {
                formula += "," + p.getId() + (i + 1);
            } else {
                formula += p.getId() + (i + 1);
            }
        }
        for (Place p : pg.getNet().getPlaces()) {
            if (!t.getPreset().contains(p) && !t.getPostset().contains(p)) {
                if (formula.length() > 4) {
                    formula += "," + p.getId() + (i) + "=>" + p.getId()
                            + (i + 1) + "," + p.getId() + (i + 1) + "=>"
                            + p.getId() + (i);
                } else {
                    formula += p.getId() + (i) + "=>" + p.getId() + (i + 1)
                            + "," + p.getId() + (i + 1) + "=>" + p.getId()
                            + (i);
                }
            }
        }
        Set<Place> preMinusPost = new HashSet<Place>(t.getPreset());
        preMinusPost.removeAll(t.getPostset());
        for (Place p : preMinusPost) {
            if (formula.length() > 4) {
                formula += ",!" + p.getId() + (i + 1);
            } else {
                formula += "!" + p.getId() + (i + 1);
            }
        }
        formula += ")";
        return formula;
    }

    public String[] getFlow(int n) {
        if (orversion) {
            // OR-version: FAST computation but POOR qbf performance
            String[] flow = new String[n + 1];
            for (int i = 1; i <= n; ++i) {
                flow[i] = "OR(";
                for (Transition t : pg.getNet().getTransitions()) {
                    if (flow[i].length() > 3) {
                        flow[i] += "," + getOneTransition(t, i);
                    } else {
                        flow[i] += getOneTransition(t, i);
                    }
                }
                flow[i] += ")";
            }
            return flow;
        } else {
            // XOR version MEDIUM computation but GOOD qbf performance
            String[] flow = new String[n + 1];
            for (int i = 1; i <= n; ++i) {
                if (pg.getNet().getTransitions().size() >= 2) {
                    flow[i] = "OR(";
                    String formulalist[] = new String[pg.getNet().getTransitions()
                            .size()];
                    int j = 0;
                    for (Transition t : pg.getNet().getTransitions()) {
                        formulalist[j++] = getOneTransition(t, i);
                    }
                    for (String s1 : formulalist) {
                        for (String s2 : formulalist) {
                            if (!s1.equals(s2)) {
                                if (flow[i].length() > 3) {
                                    flow[i] += ",AND(" + s1 + ",!" + s2 + "),AND(!"
                                            + s1 + "," + s2 + ")";
                                } else {
                                    flow[i] += "AND(" + s1 + ",!" + s2 + "),AND(!"
                                            + s1 + "," + s2 + ")";
                                }
                            }
                        }
                    }
                    flow[i] += ")";
                    // System.out.println("case 3: " + flow[i]);
                } else if (pg.getNet().getTransitions().size() == 1) {
                    Transition[] test = new Transition[1];
                    Transition[] arr = pg.getNet().getTransitions().toArray(test);
                    flow[i] = getOneTransition(arr[0], i);
                    // System.out.println("case 1: " + flow[i]);
                } else {
                    flow[i] = "";
                    // System.out.println("case 2: " + flow[i]);
                }
            }
            return flow;
        }
    }

    public String[] getNobadmarking(int n) {
        String[] nobadmarking = new String[n + 1];
        Set<Place> badplaces = getWinningCondition().getBadPlaces();
        for (int i = 1; i <= n; ++i) {
            if (!badplaces.isEmpty()) {
                if (badplaces.size() >= 2) {
                    nobadmarking[i] = "AND(";
                } else {
                    nobadmarking[i] = "";
                }
                for (Place p : badplaces) {
                    if (nobadmarking[i].length() > 4) {
                        nobadmarking[i] += ",!" + p.getId() + i;
                    } else {
                        nobadmarking[i] += "!" + p.getId() + i;
                    }
                }
                if (badplaces.size() >= 2) {
                    nobadmarking[i] += ")";
                }
            } else {
                nobadmarking[i] = "";
            }
            // System.out.println(nobadmarking[i]);
        }
        return nobadmarking;
    }

    public String[] getTerminating(int n) {
        String[] terminating = new String[n + 1];
        boolean first = true;
        for (int i = 1; i <= n; ++i) {
            if (pg.getNet().getTransitions().size() >= 2) {
                terminating[i] = "AND(";
            } else {
                terminating[i] = "";
            }
            for (Transition t : pg.getNet().getTransitions()) {
                Set<Place> pre = t.getPreset();
                if (pre.size() >= 2) {
                    if (terminating[i].length() > 4) {
                        terminating[i] += ",OR(";
                    } else {
                        terminating[i] += "OR(";
                    }
                } else if (terminating[i].length() > 4) {
                    terminating[i] += ",";
                }
                first = true;
                for (Place p : pre) {
                    if (!first) {
                        terminating[i] += ",!" + p.getId() + i;
                    } else {
                        terminating[i] += "!" + p.getId() + i;
                        first = false;
                    }
                }
                if (pre.size() >= 2) {
                    terminating[i] += ")";
                }
            }
            if (pg.getNet().getTransitions().size() >= 2) {
                terminating[i] += ")";
            }
            // System.out.println(terminating[i]);
        }
        return terminating;
    }

    public String[] getDeterministic(int n) {
        String[] deterministic = new String[n + 1];
        for (int i = 1; i <= n; ++i) {
            boolean added = false;
            if (pg.getNet().getTransitions().size() >= 2) {
                deterministic[i] = "AND(";
                for (Transition t1 : pg.getNet().getTransitions()) {
                    for (Transition t2 : pg.getNet().getTransitions()) {
                        if (!t1.getId().equals(t2.getId())) {
                            Set<Place> pre1 = t1.getPreset();
                            Set<Place> pre2 = t2.getPreset();
                            Set<Place> preIntersect = new HashSet<>(pre1);
                            preIntersect.retainAll(pre2);
                            Set<Place> systemplaces = new HashSet<>(pg
                                    .getNet().getPlaces());
                            systemplaces.removeAll(pg.getEnvPlaces());
                            preIntersect.retainAll(systemplaces);
                            if (!preIntersect.isEmpty()) {
                                Set<Place> preUnion = new HashSet<>(pre1);
                                preUnion.addAll(pre2);
                                String orlist = "OR(";
                                for (Place p : pre1) {
                                    if (orlist.length() > 3) {
                                        orlist += ",!" + p.getId() + i;
                                    } else {
                                        orlist += "!" + p.getId() + i;
                                    }
                                    if (!pg.getEnvPlaces().contains(p)) { // TODO
                                        // expensive
                                        orlist += ",!" + p.getId() + ".."
                                                + t1.getId();
                                        if (pre2.contains(p)) {
                                            orlist += ",!" + p.getId() + ".."
                                                    + t2.getId();
                                        }
                                    }
                                }
                                for (Place p : pre2) {
                                    if (!pre1.contains(p)) {
                                        if (orlist.length() > 3) {
                                            orlist += ",!" + p.getId() + i;
                                        } else {
                                            orlist += "!" + p.getId() + i;
                                        }
                                        if (!pg.getEnvPlaces().contains(p)) { // TODO
                                            // expensive
                                            orlist += ",!" + p.getId() + ".."
                                                    + t2.getId();
                                        }
                                    }
                                }
                                orlist += ")";
                                if (deterministic[i].length() > 4) {
                                    deterministic[i] += "," + orlist;
                                } else {
                                    deterministic[i] += orlist;
                                }
                                added = true;
                            }
                        }
                    }
                }
                deterministic[i] += ")";
            } else {
                deterministic[i] = "";
            }
            if (!added) {
                deterministic[i] = "";
            }
            // System.out.println(deterministic[i]);
        }
        return deterministic;
    }

    public String getLoop(int n) {
        if (newLoop) {
            if (n <= 1) {
                return "F";
            }
            String loop = "OR(";
            Set<Place> places = pg.getNet().getPlaces();
            boolean first = true;
            for (int i = 1; i <= n; ++i) {
                for (int j = 1; j <= n; ++j) {
                    if (i != j) {
                        String andlist = "AND(";
                        for (Place p : places) {
                            if (andlist.length() > 4) {
                                andlist += "," + p.getId() + i + "=>" + p.getId() + j + ","
                                        + p.getId() + j + "=>" + p.getId() + i;
                            } else {
                                andlist += p.getId() + i + "=>" + p.getId() + j + ","
                                        + p.getId() + j + "=>" + p.getId() + i;
                            }
                        }
                        andlist += ")";
                        if (first) {
                            loop += andlist;
                            first = false;
                        } else {
                            loop += "," + andlist;
                        }
                    }
                }
            }
            loop += ")";
            // System.out.println(loop);
            return loop;
        } else {
            //OLD version
            if (n <= 1) {
                return "F";
            }
            String loop = "OR(";
            Set<Place> places = pg.getNet().getPlaces();
            for (int i = 1; i < n; ++i) {
                String andlist = "AND(";
                for (Place p : places) {
                    if (andlist.length() > 4) {
                        andlist += "," + p.getId() + i + "=>" + p.getId() + n + ","
                                + p.getId() + n + "=>" + p.getId() + i;
                    } else {
                        andlist += p.getId() + i + "=>" + p.getId() + n + ","
                                + p.getId() + n + "=>" + p.getId() + i;
                    }
                }
                andlist += ")";
                if (i == 1) {
                    loop += andlist;
                } else {
                    loop += "," + andlist;
                }
            }
            loop += ")";
            // System.out.println(loop);
            return loop;
        }
    }

    public String restrictStrategy() {
        Set<Place> sysPlaces = new HashSet<>(pg.getNet().getPlaces());
        sysPlaces.removeAll(pg.getEnvPlaces());
        String formula;
        if (sysPlaces.size() >= 2) {
            formula = "AND(";
        } else {
            formula = "";
        }
        // OR-variant
        // for (Place p : sysPlaces) {
        // if (p.getPostset().size() > 0) {
        // if (formula.length() > 4)
        // formula += ",OR(";
        // else
        // formula += "OR(";
        // boolean first = true;
        // for (Transition t : p.getPostset()) {
        // if (!first) {
        // formula += "," + p.getId() + ".." + t.getId();
        // } else {
        // formula += p.getId() + ".." + t.getId();
        // first = false;
        // }
        // }
        // formula += ")";
        // }
        // }

        // XOR-variant
        for (Place p : sysPlaces) {
            for (Transition t1 : p.getPostset()) {
                if (p.getPostset().size() >= 2) {
                    if (formula.length() > 4) {
                        formula += ",OR(";
                    } else {
                        formula += "OR(";
                    }
                    boolean first = true;
                    for (Transition t2 : p.getPostset()) {
                        if (!t1.getId().equals(t2.getId())) {
                            String dec1 = p.getId() + ".." + t1.getId();
                            String dec2 = p.getId() + ".." + t2.getId();
                            if (!first) {
                                formula += "," + "AND(" + dec1 + ",!" + dec2
                                        + "),AND(!" + dec1 + "," + dec2 + ")";
                            } else {
                                formula += "AND(" + dec1 + ",!" + dec2
                                        + "),AND(!" + dec1 + "," + dec2 + ")";
                                first = false;
                            }
                        }
                    }
                    formula += ")";
                } else if (formula.length() > 4) {
                    formula += "," + p.getId() + ".." + t1.getId();
                } else {
                    formula += p.getId() + ".." + t1.getId();
                }
            }
        }

        if (sysPlaces.size() >= 2) {
            formula += ")";
        }
        //System.out.println(formula);
        return formula;
    }

    public void removeTransitionRecursively(Transition t) {
        if (pg.getNet().getTransitions().contains(t)) {
            Set<Place> next = t.getPostset();
            Marking in = pg.getNet().getInitialMarking();
            // System.out.println("Recur remove transition " + t.getId());
            pg.getNet().removeTransition(t);
            for (Place p : next) {
                // remove place p if all transition leading to it are removed or all
                // incoming transitions are also outgoing from p
                // don't remove place if part of initial marking
                if (in.getToken(p).getValue() == 0
                        && (p.getPreset().isEmpty() || p.getPostset().containsAll(
                                p.getPreset()))) {
                    removePlaceRecursively(p);
                }
            }
        }
    }

    public void removePlaceRecursively(Place p) {
        Set<Transition> next = p.getPostset();
        // System.out.println("Recur remove place " + p.getId());
        pg.getNet().removePlace(p);
        for (Transition t : next) {
            // remove transition t as soon as one place in pre(t) is removed
            removeTransitionRecursively(t);
        }
    }

    /*
     * Only safe nets
     */
    public boolean transformIntoBC(int n, int b) throws NetNotSafeException,
            IOException, InterruptedException,
            NoSuitableDistributionFoundException {
        long startTimeFile = System.currentTimeMillis();
        File result = new File("Result/");
        result.mkdir();
        Tools.savePN2Dot("Result/before", pg.getNet(), false);
        createBUnfolding(b);
        Tools.savePN2Dot("Result/after", pg.getNet(), false);

        String initial = getInitial();
        String[] deadlock = getDeadlock(n);
        String[] flow = getFlow(n);

        String[] sequence = new String[n + 1];
        for (int i = 1; i <= n; ++i) {
            sequence[i] = "AND(" + initial;
            for (int j = 1; j <= i - 1; ++j) {
                sequence[i] += ",!(" + deadlock[j] + ")," + flow[j];
            }
            sequence[i] += ")";

        }

        String[] nobadmarking = getNobadmarking(n);
        String[] terminating = getTerminating(n);
        String[] deterministic = getDeterministic(n);

        String loop = getLoop(n);

        String[] deadlocksterm = new String[n + 1];
        for (int i = 1; i <= n; ++i) {
            deadlocksterm[i] = "(" + deadlock[i] + ")=>(" + terminating[i]
                    + ")";
        }

        String[] winning = new String[n + 1];
        for (int i = 1; i <= n; ++i) {
            winning[i] = "AND(";
            // if (!restrict.equals(""))
            // winning[i] += restrict + ",";
            if (!nobadmarking[i].equals("")) {
                winning[i] += nobadmarking[i] + ",";
            }
            if (!deadlocksterm[i].equals("")) {
                winning[i] += deadlocksterm[i] + ",";
            }
            if (!deterministic[i].equals("")) {
                winning[i] += deterministic[i] + ",";
            }
            if (winning[i].endsWith(",")) {
                winning[i] = winning[i].substring(0, winning[i].length() - 1);
            }
            winning[i] += ")";
        }

        String formula = "!AND(";
        for (int i = 1; i <= n - 1; ++i) { // slightly optimized
            if (formula.length() > 5) {
                formula += ",(" + sequence[i] + ")=>(" + winning[i] + ")";
            } else {
                formula += "(" + sequence[i] + ")=>(" + winning[i] + ")";
            }
        }
        if (formula.length() > 5) {
            formula += ",(" + sequence[n] + ")=>(AND(" + winning[n] + ","
                    + loop + "))";
        } else {
            formula += "(" + sequence[n] + ")=>(AND(" + winning[n] + "," + loop
                    + "))";
        }
        formula += ")";

        long stopTimeFile = System.currentTimeMillis();
        long elapsedTimeFile = stopTimeFile - startTimeFile;
        System.out.println("Elapsed time for creating BC: " + 1.0
                * elapsedTimeFile / 1000 + "s");

        long startTimeFileWrite = System.currentTimeMillis();
        File input = new File("Input/");
        input.mkdir();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("Input/" + pg.getNet().getName() + n
                        + ".txt"), "utf-8"), 32768);
        writer.write("BC1.1\n");
        writer.write("f := \n");
        writer.write(formula);
        writer.write(";\n");
        writer.write("ASSIGN f;");
        writer.close();

        long stopTimeFileWrite = System.currentTimeMillis();
        long elapsedTimeFileWrite = stopTimeFileWrite - startTimeFileWrite;

        System.out.println("Elapsed time for writing BC to file: " + 1.0
                * elapsedTimeFileWrite / 1000 + "s");

        File outputF = new File("Output/");
        outputF.mkdir();
        long startTimeBC = System.currentTimeMillis();
        Runtime rt = Runtime.getRuntime();
        Process pr = null;
        try {
            pr = rt
                    .exec("./bc2cnf -nosimplify Input/"
                            + pg.getNet().getName() + n + ".txt Output/output" + n
                            + ".txt");
        } catch (IOException ex) {
            System.out.println("You have to put the executable \"bc2cnf\" of BCPackage at the same location "
                    + "where you are running the tool. (tested with version 0.40 of BCPackage)");
            System.exit(0);
        }
        pr.waitFor();
        long stopTimeBC = System.currentTimeMillis();
        long elapsedTimeBC = stopTimeBC - startTimeBC;
        System.out.println("Elapsed time for BC2CNF: " + 1.0 * elapsedTimeBC
                / 1000 + "s");
        long startTimePrep = System.currentTimeMillis();
        // adding exists and forall
        int counter = 0;
        boolean nextWord = false;
        boolean nextNumber = false;
        boolean dontadd = false;
        List<Integer> exists = new ArrayList<>();
        List<Integer> forall = new ArrayList<>();
        List<Integer> aider = new ArrayList<>();
        String word = "";
        int number = 0;
        Scanner s = null;
        try {
            s = new Scanner(new BufferedReader(new FileReader("Output/output"
                    + n + ".txt")));
            String next = "";
            Set<Integer> helperVarList = new HashSet<>();
            while (s.hasNext()) {
                next = s.next();
                if (next.equals("c") && counter < 4) {
                    ++counter;
                } else if (next.equals("c") && counter >= 4) {
                    nextWord = true;
                    if (!dontadd) {
                        dontadd = false;
                        helperVarList.add(number);
                        if (word.contains("..")) {
                            exists.add(number);
                            strategy.put(number, word);
                        } else {
                            forall.add(number);
                        }
                    }
                } else if (nextWord) {
                    nextWord = false;
                    word = next;
                } else if (next.equals("<->")) {
                    nextNumber = true;
                } else if (nextNumber) {
                    nextNumber = false;
                    if (!next.equals("F") && !next.equals("T")) {
                        number = Integer.parseInt(next);
                    } else {
                        dontadd = true;
                    }
                } else if (next.equals("p")) {
                    // result of last iteration
                    if (!word.equals("") && !dontadd) {
                        dontadd = false;
                        helperVarList.add(number);
                        if (word.contains("..")) {
                            exists.add(number);
                            strategy.put(number, word);
                        } else {
                            forall.add(number);
                        }
                    }
                    // get number of variables
                    next = s.next();
                    next = s.next();
                    int max = Integer.parseInt(next);
                    // add Hilfsvariablen
                    for (int i = 1; i <= max; ++i) {
                        if (!helperVarList.contains(i)) {
                            aider.add(i);
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Datei!");
        } finally {
            try {
                s.close();
            } catch (Exception e) {
            }
        }
        String e = "a "; // "e 1 "
        String a = "e "; // "a "
        String e2 = "e "; // "e "
        for (int ex : exists) {
            e += ex + " ";
        }
        for (int al : forall) {
            if (al != 1) {
                a += al + " ";
            }
        }
        for (int ex : aider) {
            e2 += ex + " ";
        }
        e += "0";
        a += "0";
        e2 += "0";
        if (exists.isEmpty()) {
            e = "";
        }
        if (forall.isEmpty()) {
            a = "";
        }
        if (aider.isEmpty()) {
            e2 = "";
        }

        BufferedWriter w = null;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader("Output/output" + n + ".txt"));
            w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    "Output/output" + n + "_ea.txt"), "utf-8"));
            String line = "";
            while ((line = r.readLine()) != null) {
                if (line.startsWith("p")) {
                    w.write(line + "\n");
                    w.write(e + "\n");
                    w.write(a + "\n");
                    w.write(e2 + "\n");
                } else {
                    w.write(line + "\n");
                }
            }
        } finally {
            r.close();
            w.close();
        }
        long stopTimePrep = System.currentTimeMillis();
        long elapsedTimePrep = stopTimePrep - startTimePrep;
        System.out.println("Elapsed time for preparation for QBF: " + 1.0
                * elapsedTimePrep / 1000 + "s");
        long startTimeQBF = System.currentTimeMillis();
        Process pr2 = null;
        try {
            pr2 = rt.exec("./depqbf --qdo Output/output" + n
                    + "_ea.txt");
        } catch (IOException ex) {
            System.out.println("You have to put the executable \"depqbf\" of depQBF at the same location "
                    + "where you are running the tool. (tested with version 4.01 of depQBF)");
            System.exit(0);
        }

        // Getting console output for strategy construction
        BufferedReader is = new BufferedReader(new InputStreamReader(
                pr2.getInputStream()));
        String line;
        String output = "";
        while ((line = is.readLine()) != null) {
            output += line + " ";
        }

        int exitval = pr2.waitFor();
        long stopTimeQBF = System.currentTimeMillis();
        long elapsedTimeQBF = stopTimeQBF - startTimeQBF;
        System.out.println("Elapsed time for QBF solving: " + 1.0
                * elapsedTimeQBF / 1000 + "s");
        System.out
                .println("Elapsed time in total: "
                        + (elapsedTimeQBF + elapsedTimePrep + elapsedTimeBC + elapsedTimeFile)
                        / 1000.0 + "s");
        if (exitval == 10) {
            System.out.println("UNSAT");
        } else if (exitval == 20) {
            // Parsing console output and generation of strategy
            String[] parts = output.split(" ");
            boolean read = false;
            int num;
            for (int i = 0; i < parts.length; ++i) {
                if (parts[i].equals("V")) {
                    read = true;
                } else if (read) {
                    read = false;
                    num = Integer.parseInt(parts[i]);
                    if (num > 0) {
                        //System.out.println("ALLOW " + strategy.get(num));
                    } else if (num < 0) {
                        String remove = strategy.get(num * (-1));
                        //System.out.println("DISALLOW " + remove);
                        int in = remove.indexOf("..");
                        String rem = remove.substring(in + 2, remove.length());
                        // Transition might be already removed by recursion
                        for (Transition t : pg.getNet().getTransitions()) {
                            if (t.getId().equals(rem)) {
                                removeTransitionRecursively(t);
                            }
                        }
                    } else {
                        System.out.println("ERROR: num should not be zero?!");
                    }
                }
            }
            Tools.savePN2Dot("Result/strategy", pg.getNet(), false);
            System.out.println("SAT");
            solvable = true;
            strat = pg.getNet();
        } else {
            System.out.println("ERROR " + exitval);
            error = true;
        }
        return true;
    }

    @Override
    protected boolean exWinStrat() {
        return this.solvable;
    }

    @Override
    protected PetriNet calculateStrategy() throws NoStrategyExistentException {
        return this.strat;
    }
}
