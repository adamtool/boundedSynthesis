package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.analysis.exception.UnboundedException;
import uniolunisaar.adam.ds.exceptions.CouldNotFindSuitableWinningConditionException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 *
 * @author Jesko Hecking-Harbusch
 *
 * Requires the executables "bc2cnf" and "depqbf" from BCPackage and DepQBF as
 * well as the Petri Game in .apt format at the same position as the executable.
 */
public class QBFPetriGame extends PetriGame {

    int n = 0;
    int b = 0;

    public QBFPetriGame(PetriNet pn) throws UnboundedPGException {
        super(pn);
    }

    public QBFPetriGame(PetriNet pn, int n, int b) throws UnboundedPGException {
        super(pn);
        this.n = n;
        this.b = b;
    }

    public QBFPetriGame(PetriNet pn, String string, String string2) throws UnboundedException, CouldNotFindSuitableWinningConditionException {
        super(pn);
        n = Integer.parseInt(string);
        b = Integer.parseInt(string2);
    }
}
