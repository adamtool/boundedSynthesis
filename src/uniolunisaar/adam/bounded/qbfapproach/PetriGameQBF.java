package uniolunisaar.adam.bounded.qbfapproach;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.analysis.exception.UnboundedException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.winningconditions.Safety;
import uniolunisaar.adam.symbolic.bddapproach.exceptions.CouldNotFindSuitableWinningConditionException;

/**
 * 
 * @author Jesko Hecking-Harbusch
 *
 * Requires the executables "bc2cnf" and "depqbf" from BCPackage and DepQBF as well
 * as the Petri Game in .apt format at the same position as the executable.
 */
public class PetriGameQBF extends uniolunisaar.adam.ds.petrigame.PetriGame {

    BoundedSynthesis bound = null;
    int n = 0;
    int b = 0;

    public PetriGameQBF(PetriNet pn) throws UnboundedException, CouldNotFindSuitableWinningConditionException {
        super(pn, new Safety());
        bound = new BoundedSynthesis(this, n, b);
    }

    public PetriGameQBF(PetriNet pn, int n, int b) throws UnboundedException, CouldNotFindSuitableWinningConditionException {
        super(pn, new Safety());
        bound = new BoundedSynthesis(this, n, b);
    }

    public PetriGameQBF(PetriNet pn, String string, String string2) throws UnboundedException, CouldNotFindSuitableWinningConditionException {
        super(pn, new Safety());
        n = Integer.parseInt(string);
        b = Integer.parseInt(string2);
        bound = new BoundedSynthesis(this, n, b);

    }

    public boolean existsWinningStrategy() {
        return bound.solvable;
    }

    public PetriNet getStrategy() throws NoStrategyExistentException {
        return bound.strat;
    }
}
