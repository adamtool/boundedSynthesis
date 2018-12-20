package uniolunisaar.adam.bounded.qbfapproach;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

import uniol.apt.io.parser.ParseException;
import uniol.apt.module.exception.ModuleException;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.exceptions.CouldNotFindSuitableWinningConditionException;
import uniolunisaar.adam.logic.exceptions.NetNotConcurrencyPreservingException;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.logic.exceptions.ParameterMissingException;
import uniolunisaar.adam.ds.exceptions.SolverDontFitPetriGameException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.exceptions.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.generators.synthesis.SecuritySystem;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
//@Test
public class SSTest {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/securitySystem/";

    private static final int countSecuritySystems = 7;

    @BeforeClass
    public void createFolder() {
        Logger.getInstance().setVerbose(false);
        (new File(outputDir)).mkdirs();
    }

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Container terminal (more than one env place)
    @DataProvider(name = "secSystem")
    public static Object[][] securitySystem() {
        Object[][] out = new Object[countSecuritySystems][2];
        for (int i = 0; i < countSecuritySystems; i++) {
            out[i][0] = i + 2;
            out[i][1] = true;
        }
        return out;
    }

    //@Test(dataProvider = "secSystem")
    public void testSecuritySystem(int intrudingPoints, boolean hasStrategy) throws NetNotSafeException, NetNotConcurrencyPreservingException, NoStrategyExistentException, IOException, InterruptedException, FileNotFoundException, ModuleException, NoSuitableDistributionFoundException, SolverDontFitPetriGameException, NotSupportedGameException, CouldNotFindSuitableWinningConditionException, ParameterMissingException, ParseException, SolvingException {
        final String path = outputDir;
        String name = intrudingPoints + "_secSystem";
        File f = new File(path);
        f.mkdir();
        System.out.println("Generate security system ...");
        PetriGame pn = SecuritySystem.createSafetyVersionForBounded(intrudingPoints, true);
        AdamTools.savePG2PDF(path + name, pn, false);
        QbfSolver<? extends Condition> solv = QbfSolverFactory.getInstance().getSolver(pn, false, new QbfSolverOptions(7, 3));
        if (hasStrategy) {
            Assert.assertTrue(solv.existsWinningStrategy());
            AdamTools.savePG2PDF(path + name + "_pg", solv.getStrategy(), true);
        } else {
            Assert.assertFalse(solv.existsWinningStrategy());
        }
    }
}
