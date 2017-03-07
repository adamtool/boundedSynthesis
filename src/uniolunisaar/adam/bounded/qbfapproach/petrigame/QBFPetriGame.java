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

    public QBFPetriGame(PetriNet pn) throws UnboundedPGException {
        super(pn);
    }
}
