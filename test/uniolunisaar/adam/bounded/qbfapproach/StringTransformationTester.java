package uniolunisaar.adam.bounded.qbfapproach;

import uniolunisaar.adam.bounded.qbfapproach.petrigame.QBFPetriGame;
import com.sun.corba.se.impl.io.TypeMismatchException;
import java.io.IOException;
import java.text.ParseException;

import org.testng.annotations.Test;

import uniol.apt.adt.exception.StructureException;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.symbolic.bddapproach.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.symbolic.bddapproach.exceptions.CouldNotFindSuitableWinningConditionException;
import uniolunisaar.adam.util.Tools;

@Test
public class StringTransformationTester {

    @Test(enabled=false) // disabled because of fixed paths to nets which wasn't added to the repository
    public void test() throws
            NoSuitableDistributionFoundException,
            TypeMismatchException, StructureException,
            IOException, ParseException, InterruptedException, UnboundedException, NetNotSafeException, CouldNotFindSuitableWinningConditionException {

        // PetriNet pn = SelfOrganizingRobots.generate(2, 1, true, true);		// same problem as in example 5-7
        // PetriNet pn = Philosopher.generateGuided(5, true, true);				// no unfold: 2->4, 3->5, 4->6, 5->7 (270s XOR)<->(100s OR 5->8 6500s OR 5->7)
        // PetriNet pn = Workflow.generate(3, 1, true, true);					// 2 1 6 3->1s sollte test_1 und test_2 nach dem ersten Mal verboten sein 
        // PetriNet pn = RobotCell.generate(2, 0, true);						// 2 2 should be satisfiable -> smallest example 5
        // PetriNet pn = Clerks.generateCP(6, true, true);						// no unfold: 1->5, 2->7, ..., 5->13, 6->15 (167s OR), ..., 20->43 (to be tested) OR faster
        // PetriNet pn = ManufactorySystem.generate(2, true, true, true);		// 2 7 3->123s(XOR) 1,23s(OR)

        boolean one = false;
        boolean two = false;
        boolean three = false;
        boolean four = false;
        boolean five = false;
        boolean six = false;
        boolean seven = true;
        boolean eight = false;

        if (one) {
            PetriNet pn = Tools.getPetriNet("PetriGames/myexample1.apt");	// system cycle
            new QBFPetriGame(pn);
        }

        if (two) {
            PetriNet pn2 = Tools.getPetriNet("PetriGames/myexample2.apt"); // reason for ENV unfolding
            new QBFPetriGame(pn2, "10", "3");
        }

        if (three) {
            PetriNet pn3 = Tools.getPetriNet("PetriGames/myexample3.apt"); // system decision
            new QBFPetriGame(pn3, "10", "1");
        }

        if (four) {
            PetriNet pn4 = Tools.getPetriNet("PetriGames/myexample4.apt"); // not deadlock avoiding example
            new QBFPetriGame(pn4, "10", "3");
        }

        if (five) {
            PetriNet pn5 = Tools.getPetriNet("PetriGames/myexample5.apt"); // non-unique system decision
            new QBFPetriGame(pn5, "8", "1");
        }

        if (six) {
            PetriNet pn6 = Tools.getPetriNet("/Users/Jesko/Documents/workspace/Hiwi/PetriGames/myexample6.apt"); // unfolding
            new QBFPetriGame(pn6, "10", "2");
        }

        if (seven) {
            PetriNet pn7 = Tools.getPetriNet("PetriGames/myexample7.apt"); // unfolding
            new QBFPetriGame(pn7, "10", "3");
            // b > 2 -> copy not right TODO
        }

        if (eight) {
            PetriNet pn8 = Tools.getPetriNet("PetriGames/myexample8.apt"); // unfolding
            new QBFPetriGame(pn8, "10", "3");
        }
    }
}