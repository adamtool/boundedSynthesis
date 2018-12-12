package uniolunisaar.adam.bounded.qbfapproach.solver;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.bounded.qbfapproach.exceptions.BoundedParameterMissingException;
import uniolunisaar.adam.bounded.qbfapproach.petrigame.QbfSolvingObject;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.solver.SolverOptions;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public abstract class SolverQbfAndQbfCon<W extends WinningCondition, SOP extends SolverOptions> extends Solver<QbfSolvingObject<W>, SOP>{

	// steps of solving
	public QbfSolvingObject<W> originalSolvingObject;
	public PetriGame originalGame;
	public PetriGame unfolding;
	public PetriGame strategy;
	
	// variable to store keys of calculated components for later use (shared among all solvers)
	protected int in;
	protected int l;
	protected int u;
	protected int[] fl;
	protected int[] det;
	protected int[] dlt;
	protected int[] dl;
	protected int[] term;
	protected int[] seq;
	protected int[] win;
	protected int[] seqImpliesWin;
	
	// keys
	protected Map<Integer, String> exists_transitions = new HashMap<>();
	
	// storing QBF result for strategy generation
	protected String outputQBFsolver;
	
	// caches
	protected Transition[] transitions;
	protected File file;
	
	// solving
	protected BufferedWriter writer;
	protected int variablesCounter = 1; // 1 reserved for phi
	protected Map<String, Integer> numbersForVariables = new HashMap<>(); // map for storing keys and the corresponding value


	protected SolverQbfAndQbfCon(QbfSolvingObject<W> solverObject, SOP options) {
		super(solverObject, options);
		
		originalSolvingObject = getSolvingObject().getCopy();
		
	}
	
	protected void initializeNandB(int n, int b) throws BoundedParameterMissingException {
		if (n == -1) {
			if (getSolvingObject().hasBoundedParameterNinExtension()) {
				n = getSolvingObject().getBoundedParameterNFromExtension();
			}
		}
		if (b == -1) {
			if (getSolvingObject().hasBoundedParameterBinExtension()) {
				b = getSolvingObject().getBoundedParameterBFromExtension();
			}
		}
		if (n == -1 || b == -1) {
			throw new BoundedParameterMissingException(n, b);
		}
		getSolvingObject().setN(n);
		getSolvingObject().setB(b);
	}
}
