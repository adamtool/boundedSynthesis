package uniolunisaar.adam.bounded.qbfconcurrent.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.exception.UnboundedException;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.PGSimplifier;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.bounded.qbfapproach.unfolder.ForNonDeterministicUnfolder;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.tools.AdamProperties;

public class QBFConSafetySolver extends QBFConSolver<Safety> {

	 // variable to store keys of calculated components for later use
    private int in;
    private int[] fl;
    private int[] bad;
    private int[] term;
    private int[] det;
    private int l;
    private int[] dl;
    private int[] seq;
    private int[] dlt;
    private int[] win;
    private int[] seqImpliesWin;

    // results
    public Boolean solvable = null;
    public Boolean sat = null;
    public Boolean error = null;

    // keys
    public Map<Integer, String> exists_transitions = new HashMap<>();
    public Map<Integer, String> forall_places = new HashMap<>();

    private String outputCAQE = "";
    
    
	public QBFConSafetySolver(PetriGame game, Safety safe, QBFConSolverOptions so) throws NotSupportedGameException, BoundedParameterMissingException {
        super(game, safe, so);
        fl = new int[pg.getN() + 1];
        bad = new int[pg.getN() + 1];
        term = new int[pg.getN() + 1];
        det = new int[pg.getN() + 1];
        dl = new int[pg.getN() + 1];
        seq = new int[pg.getN() + 1];
        dlt = new int[pg.getN() + 1];
        win = new int[pg.getN() + 1];
    }

    private void writeInitial() throws IOException {
        in = createUniqueID();
        writer.write(in + " = " + getInitial());
    }

    private void writeDeadlock() throws IOException {
        String[] deadlock = getDeadlock();
        for (int i = 1; i <= pg.getN(); ++i) {
            dl[i] = createUniqueID();
            writer.write(dl[i] + " = " + deadlock[i]);
        }
    }

    private void writeFlow() throws IOException {
    	writer.write("# !!!!!!!!!!!!!!!!get Flow starts From Here!!!!!!!!!!!!!!!!!!!!!!!\n");
        String[] flow = getFlow();
        for (int i = 1; i < pg.getN(); ++i) {
            fl[i] = createUniqueID();
            writer.write(fl[i] + " = " + flow[i] + "#WriteFlow\n");
        }
        writer.write("# ((((((((((((((((((((((( write Flow ends Here )))))))))))))))))))))))\n");

    }

    private void writeSequence() throws IOException {
        Set<Integer> and = new HashSet<>();
        for (int i = 1; i <= pg.getN(); ++i) {
            and.clear();
            and.add(in);
            for (int j = 1; j <= i - 1; ++j) {
                //and.add(-dl[j]); // performance evaluation showed that leaving this out makes program faster as it is redundant
                and.add(fl[j]);
            }
            seq[i] = createUniqueID();
            writer.write(seq[i] + " = " + writeAnd(and) + "#WriteSequence\n");
        }
    }

    private void writeNoBadMarking() throws IOException {
        if (!getSolvingObject().getWinCon().getBadPlaces().isEmpty()) {
            String[] nobadmarking = getNobadmarking();
            if (!getSolvingObject().getWinCon().getBadPlaces().isEmpty()) {
                for (int i = 1; i <= pg.getN(); ++i) {
                    bad[i] = createUniqueID();
                    writer.write(bad[i] + " = " + nobadmarking[i]);
                }
            }
        }
    }

    public String[] getNobadmarking() {
        String[] nobadmarking = new String[pg.getN() + 1];
        Set<Integer> and = new HashSet<>();
        for (int i = 1; i <= pg.getN(); ++i) {
            and.clear();
            for (Place p : getSolvingObject().getWinCon().getBadPlaces()) {
                and.add(-getVarNr(p.getId() + "." + i, true));
            }
            nobadmarking[i] = writeAnd(and);
        }
        return nobadmarking;
    }

    private void writeTerminating() throws IOException {
        String[] terminating = new String[pg.getN() + 1];
        terminating = getTerminating();
        for (int i = 1; i <= pg.getN(); ++i) {
            term[i] = createUniqueID();
            writer.write(term[i] + " = " + terminating[i]);
        }
    }

    private void writeDeterministic() throws IOException {
        if (deterministicStrat) {
            String[] deterministic = getDeterministic();
            for (int i = 1; i <= pg.getN(); ++i) {
                det[i] = createUniqueID();
                writer.write(det[i] + " = " + deterministic[i]);
            }
        }
    }

    private void writeLoop() throws IOException {
        String loop = getLoopIJ();
        l = createUniqueID();
        writer.write(l + " = " + loop);
    }

    private void writeDeadlocksterm() throws IOException {
        Set<Integer> or = new HashSet<>();
        for (int i = 1; i <= pg.getN(); ++i) {
            or.clear();
            or.add(-dl[i]);
            or.add(term[i]);
            dlt[i] = createUniqueID();
            writer.write(dlt[i] + " = " + writeOr(or));
        }
    }

    private void writeWinning() throws IOException {
        Set<Integer> and = new HashSet<>();
        for (int i = 1; i <= pg.getN(); ++i) {
            and.clear();
            if (bad[i] != 0) {
                and.add(bad[i]);
            }
            if (dlt[i] != 0) {
                and.add(dlt[i]);
            }
            if (det[i] != 0) {
                and.add(det[i]);
            }
            win[i] = createUniqueID();
            writer.write(win[i] + " = " + writeAnd(and));
        }
    }

    public void addExists() throws IOException {
        Set<Integer> exists = new HashSet<>();
        for (Place p : pg.getGame().getPlaces()) {
            if (!pg.getGame().getEnvPlaces().contains(p)) {
                if (p.getId().startsWith(QBFConSolver.additionalSystemName)) {
                    for (Transition t : p.getPostset()) {
                        int number = createVariable(p.getId() + ".." + t.getId());
                        exists.add(number);
                        //System.out.println(number + " = " + p.getId() + ".." + t.getId());
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
                            //System.out.println(number + " = " + p.getId() + ".." + truncatedID);
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
        for (Place p : pg.getGame().getPlaces()) {
            for (int i = 1; i <= pg.getN(); ++i) {
                int number = createVariable(p.getId() + "." + i);
                //System.out.println(p.getId() + "." + i + " : number: " + number);
                forall.add(number);
                //System.out.println(number + " = " + p.getId() + "." + i);
                forall_places.put(number, p.getId() /* + "." + i */);
            }
        }
        writer.write(writeForall(forall));
    }

    // Additional information from nondeterministic unfolding is utilized:
    // for each place a set of transitions is given of which at least one has to
    // be activated by the strategy to be deadlock-avoiding
    public int enumerateStratForNonDetUnfold(Map<Place, Set<Transition>> additionalInfoForNonDetUnfl) throws IOException {
        if (additionalInfoForNonDetUnfl.keySet().isEmpty()) {
            return -1;
        }
        int index = createUniqueID();
        Set<Integer> and = new HashSet<>();
        Set<Transition> transitions;
        int or_index;
        Set<Integer> or = new HashSet<>();
        for (Place p : additionalInfoForNonDetUnfl.keySet()) {
            transitions = additionalInfoForNonDetUnfl.get(p);
            or_index = createUniqueID();
            or.clear();
            for (Transition t : transitions) {
                or.add(getVarNr(p.getId() + ".." + t.getId(), true));
            }
            writer.write(or_index + " = " + writeOr(or));
            and.add(or_index);
        }
        writer.write(index + " = " + writeAnd(and));
        return index;
    }

    @Override
    protected boolean exWinStrat() {
        ForNonDeterministicUnfolder unfolder = new ForNonDeterministicUnfolder(pg, null); // null forces unfolder to use b as bound for every place
    	/*McMillianUnfolder unfolder = null;
		try {
			unfolder = new McMillianUnfolder(pg, null);
		} catch (NotSupportedGameException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}*/
        try {
            unfolder.prepareUnfolding();
        } catch (UnboundedException | FileNotFoundException | NetNotSafeException | NoSuitableDistributionFoundException e1) {
            System.out.println("Error: The bounded unfolding of the game failed.");
            e1.printStackTrace();
        }
        
        getWinningCondition().buffer(pg.getGame());
        
        // TODO retest McMillian with new setting of bad places etc
        /*if (QBFSolver.mcmillian) {
	        this.pg = unfolder.pg;
	        this.pn = unfolder.pn;
	        
	        Set<Place> oldBad = new HashSet<>(getWinningCondition().getBadPlaces());
	        for (Place old : oldBad) {
	        	getWinningCondition().getBadPlaces().remove(old);
	        }
        }*/
        
        seqImpliesWin = new int[pg.getN() + 1];
        transitions = pn.getTransitions().toArray(new Transition[0]);
        flowSubFormulas = new int[pg.getN() * pn.getTransitions().size()];
        deadlockSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];
        terminatingSubFormulas = new int[(pg.getN() + 1) * pn.getTransitions().size()];
        int exitcode = -1;
        try {
            writer.write("#QCIR-G14          " + QBFConSolver.linebreak); // spaces left to add variable count in the end
            addExists();
            addForall();
            writer.write("output(1)" + QBFConSolver.linebreak); // 1 = \phi

            writeInitial();
            writer.write("# End of Initial\n");
            writeDeadlock();
            writer.write("# End of Deadlock\n");
            writeFlow();
            writeSequence();
            writeNoBadMarking();
            writer.write("# End of No Bad Marking\n");
            writeTerminating();
            writer.write("# End of terminating\n");
            writeDeterministic();
            writer.write("# End of Deterministic\n");
            writeLoop();

            writer.write("# End of Loop\n");
            writeDeadlocksterm();

            writer.write("# End of Deadlocksterm\n");
            writeWinning();

            writer.write("# End of Winning\n");

            Set<Integer> phi = new HashSet<>();
            // When unfolding non-deterministically we add system places to
            // ensure deterministic decision.
            // It is required that these decide for exactly one transition which
            // is directly encoded into the problem.
            int index_for_non_det_unfolding_info = enumerateStratForNonDetUnfold(unfolder.systemHasToDecideForAtLeastOne);
            if (index_for_non_det_unfolding_info != -1) {
                phi.add(index_for_non_det_unfolding_info);
            }

            for (int i = 1; i <= pg.getN() - 1; ++i) { // slightly optimized in the sense that winning and loop are put together for n
                seqImpliesWin[i] = createUniqueID();
                writer.write(seqImpliesWin[i] + " = " + "or(-" + seq[i] + "," + win[i] + ")" + QBFConSolver.linebreak);
                phi.add(seqImpliesWin[i]);
            }

            int wnandLoop = createUniqueID();
            Set<Integer> wnandLoopSet = new HashSet<>();
            wnandLoopSet.add(l);
            wnandLoopSet.add(win[pg.getN()]);
            writer.write(wnandLoop + " = " + writeAnd(wnandLoopSet));

            seqImpliesWin[pg.getN()] = createUniqueID();
            writer.write(seqImpliesWin[pg.getN()] + " = " + "or(-" + seq[pg.getN()] + "," + wnandLoop + ")" + QBFConSolver.linebreak);
            phi.add(seqImpliesWin[pg.getN()]);

            writer.write("1 = " + writeAnd(phi));
            writer.close();

            // Total number of gates is only calculated during encoding and added to the file afterwards
            if (variablesCounter < 999999999) { // added 9 blanks as more than 999.999.999 variables wont be solvable
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                for (int i = 0; i < 10; ++i) { // read "#QCIR-G14 "
                    raf.readByte();
                }
                String counter_str = Integer.toString(variablesCounter - 1); // has NEXT usabel counter in it
                char[] counter_char = counter_str.toCharArray();
                for (char c : counter_char) {
                    raf.writeByte(c);
                }
                raf.close();
            }

            // generating qcir benchmarks
            FileUtils.copyFile(file, new File(pg.getGame().getName() + ".qcir"));

            ProcessBuilder pb = null;
            // Run solver on problem
            System.out.println("You are using " + System.getProperty("os.name") + ".");
            String os = System.getProperty("os.name");
            if (os.startsWith("Mac")) {
                System.out.println("Your operation system is supported.");
                //pb = new ProcessBuilder("./" + solver + "_mac", "--partial-assignment", file.getAbsolutePath());
                pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_mac", "--partial-assignment", file.getAbsolutePath());
            } else if (os.startsWith("Linux")) {
                System.out.println("Your operation system is supported.");
                if (QbfSolver.edacc) {
                	// for use with EDACC
                	pb = new ProcessBuilder("./" + solver + "_unix", "--partial-assignment", file.getAbsolutePath());
                } else {
                	// for use with WEBSITE
                	pb = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + File.separator + solver + "_unix", "--partial-assignment", file.getAbsolutePath());
                }
            } else {
                System.out.println("Your operation system is not supported.");
                return false;
            }
            System.out.println("A temporary file is saved to \"" + file.getAbsolutePath() + "\".");

            Process pr = pb.start();
            // Read caqe's output
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line_read;
            outputCAQE = "";
            while ((line_read = inputReader.readLine()) != null) {
                outputCAQE += line_read + "\n";
            }

            exitcode = pr.waitFor();
            inputReader.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        // Storing results
        if (exitcode == 20) {
            solvable = true;
            sat = false;
            error = false;
            System.out.println("UNSAT ");
            return false;
        } else if (exitcode == 10) {
            solvable = true;
            sat = true;
            error = false;
            System.out.println("SAT");
            return true;
        } else {
            System.out.println("QCIR ERROR with FULL output:" + outputCAQE);
            solvable = false;
            sat = null;
            error = true;
            return false;
        }
    }

    @Override
    protected PetriGame calculateStrategy() throws NoStrategyExistentException {
        if (existsWinningStrategy()) {
            for (String outputCAQE_line : outputCAQE.split("\n")) {
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
                                    if (place.startsWith(QBFConSolver.additionalSystemName)) {
                                        // additional system place exactly removes transitions
                                        // Transition might already be removed by recursion
                                        Set<Transition> transitions = new HashSet<>(pg.getGame().getTransitions());
                                        for (Transition t : transitions) {
                                            if (t.getId().equals(transition)) {
                                                // System.out.println("starting " + t);
                                                pg.removeTransitionRecursively(t);
                                            }
                                        }
                                    } else {
                                        // original system place removes ALL transitions
                                        Set<Place> places = new HashSet<>(pg.getGame().getPlaces());
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
                                return pg.getGame();
                            }
                        }
                    }
                }
            }
        }
        throw new NoStrategyExistentException();
    }

}
