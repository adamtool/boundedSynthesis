package uniolunisaar.adam.main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach.QbfSolver;
import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach.QbfSolverFactory;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolverOptions;
import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfconcurrent.QbfConSolver;
import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfconcurrent.QbfConSolverFactory;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfconcurrent.QbfConSolverOptions;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CalculationInterruptedException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.util.PGTools;

/**
 * TODO properly handle Out of Memory/Heap Space errors
 *
 * @author Manuel Gieseking
 */
public class AdamBounded {
	
	private static int bb;
	private static int n;
	/**
	 * 
	 * @param args  
	 * 				0 -> seq oder tc
	 * 				1 -> benchmark name or .apt file
	 *      		2 -> benchmark parameter or bound n
	 *      		3 -> bound n or bound b
	 * 
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 * @throws CouldNotFindSuitableConditionException
	 * @throws SolvingException
	 * @throws CalculationInterruptedException
	 * @throws NoStrategyExistentException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, ParseException, CouldNotFindSuitableConditionException, SolvingException, CalculationInterruptedException, InterruptedException, NoStrategyExistentException {
		if ((args.length != 4) && (args.length != 3))
			printHelpAndExit("Invalid number of arguments");
		String option = args[0];
		String benchmark = args[1];
		
        if (!(option.equals("tc"))&&!(option.equals("seq"))) {
        	System.out.println("Invalid solver option, tc for true concurrent and seq for sequential solving available");
        	return;
        }
        
        String filename = checkValidBenchmarkAndParameter(args, benchmark);
        
        PrintStream silentStream = new PrintStream(new OutputStream(){
            public void write(int b) {
                //Do nothing
            }
        });
        try {
        PrintStream originalStream = System.out;
        long start = System.nanoTime();
        if (option.equals("tc")) {
        	for (int i = 2; i <= n; ++i) {
        		System.setOut(silentStream);
        		QbfConSolverOptions options = new QbfConSolverOptions(i, bb, true);
                QbfConSolver<? extends Condition<?>> solver = QbfConSolverFactory.getInstance().getSolver(filename, (QbfConSolverOptions)options);
                boolean succ = solver.existsWinningStrategy();
                System.setOut(originalStream);
                String result = succ ? "SAT" : "UNSAT";
                System.out.println("Solver: tc; Benchmark/File: " + benchmark + "; Bound: " + i + "; Result: " + result);
                if (succ || i == n) {
                	long end = System.nanoTime();
                	System.out.println("Computation time: " + ((long) (end - start)/1000000000.0));
                }
                if (succ) {
                	PGTools.savePG2Dot("strategy",solver.getStrategy(), false);
                	System.out.println("Output strategy to strategy.dot");
                	System.exit(i);
                }   	
        	}
        	return;
        } else {
        	for (int i = 2; i <= n; ++i) {
        		System.setOut(silentStream);
        		QbfSolverOptions options = new QbfSolverOptions(i, bb, true);
                QbfSolver<? extends Condition> solver = QbfSolverFactory.getInstance().getSolver(filename, (QbfSolverOptions)options);
                boolean succ = solver.existsWinningStrategy();
                String result = succ ? "SAT" : "UNSAT";
                System.setOut(originalStream);
                System.out.println("Solver: seq; Benchmark/File: " + benchmark + "; Bound: " + i + "; Result: " + result);
                if (succ || i == n) {
                	long end = System.nanoTime();
                	System.out.println("Computation time: " + ((long) (end - start)/1000000000.0));
                }
                if (succ) {
                	PGTools.savePG2Dot("strategy",solver.getStrategy(), false);
                	System.out.println("Output strategy to strategy.dot");
                	System.exit(i);
                }
        	}
        }
    } catch (OutOfMemoryError E) {
    	System.out.println("Java out of Memory!");
    	System.exit(199);
    	}
    }
	
	/**
	 * Prints the error message, outputs the help and terminates in failure status.
	 * @param errorMessage
	 */
	private static void printHelpAndExit(String errorMessage) {
		String help = "Usage: ./adam_bounded  [tc|seq] [benchmark name | input file] [benchmark parameter | bound n] [bound n | bound b]\n\n"
					+ " arg 1:        tc for the true concurrent solver, seq for the sequential solver\n"
					+ " arg 2:        the name of the benchmark: AS, CA, DR, PL, DW or an input .apt file\n"
					+ " arg 3:        the parameter for the chosen benchmark or the bound n for the input file\n"
					+ " arg 4:        the maximal bound the solver is executed with or the bound b for the input file\n";
		System.out.println(errorMessage);
		System.out.println("");
		System.out.print(help);
		System.exit(1);
	}
	
	/**
	 * Returns the file name as string for the chosen benchmark and parameter.
	 * @param parameter
	 * @param benchmark
	 * @return
	 */
	private static String checkValidBenchmarkAndParameter(String[] args, String benchmark) {
		if (benchmark.endsWith(".apt")) {
			n = Integer.parseInt(args[2]);
			bb = Integer.parseInt(args[3]);
			return benchmark;
		}
		int parameter = Integer.parseInt(args[2]);
		n = Integer.parseInt(args[3]);
		switch (benchmark) { 
		case "AS":
			if (! (2 <= parameter && parameter <= 3))
				printHelpAndExit("AS only allows parameters 2 and 3");
			bb = parameter;
			return "resources/" + parameter + "_burglar.apt";
		case "CA":
			if (! (2 <= parameter && parameter <= 5))
				printHelpAndExit("CA only allows parameters 2 to 5");
			bb = 1;
			return "resources/" + parameter + "_IndependentNets.apt";
		case "DR":
			if (! (2 <= parameter && parameter <= 6))
				printHelpAndExit("DR only allows parameters 2 to 6");
			bb = 0;
			return "resources/" + parameter + "_DR.apt";
		case "PL":
			if (! (1 <= parameter && parameter <= 7))
				printHelpAndExit("PL only allows parameters 1 to 7");
			bb = parameter;
			return "resources/" + parameter + "_ProductionLine.apt";
		case "DW":
			if (! (1 <= parameter && parameter <= 13))
				printHelpAndExit("DW only allows parameters 1 to 13");
			return "resources/" + parameter + "_clerks.apt";
		default:
			printHelpAndExit("Invalid benchmark name! Only AS, CA, DR, PL and DW available");
			return null;
		}
	}

}
