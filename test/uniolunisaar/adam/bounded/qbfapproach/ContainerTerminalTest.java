package uniolunisaar.adam.bounded.qbfapproach;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.module.exception.ModuleException;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QBFSolverOptions;
import uniolunisaar.adam.ds.exceptions.NetNotConcurrencyPreservingException;
import uniolunisaar.adam.ds.exceptions.NetNotSafeException;
import uniolunisaar.adam.ds.exceptions.NoStrategyExistentException;
import uniolunisaar.adam.ds.exceptions.CouldNotFindSuitableWinningConditionException;
import uniolunisaar.adam.ds.exceptions.NoSuitableDistributionFoundException;
import uniolunisaar.adam.ds.exceptions.SolverDontFitPetriGameException;
import uniolunisaar.adam.ds.exceptions.UnboundedPGException;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;
import uniolunisaar.adam.generators.ContainerTerminal;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class ContainerTerminalTest {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/containerTerminal/";

    private static final int countContainerPlaces = 1;

    @BeforeClass
    public void createFolder() {
        Logger.getInstance().setVerbose(false);
        (new File(outputDir)).mkdirs();
    }

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Container terminal (more than one env place)
    @DataProvider(name = "conTerminal")
    public static Object[][] containerTerminal() {
        Object[][] out = new Object[countContainerPlaces][2];
        for (int i = 0; i < countContainerPlaces; i++) {
            out[i][0] = i + 2;
            out[i][1] = true;
        }
        return out;
    }

    @Test(dataProvider = "conTerminal")
    public void testContainerTerminal(int containerPlaces, boolean hasStrategy) throws NetNotSafeException, NetNotConcurrencyPreservingException, NoStrategyExistentException, IOException, InterruptedException, FileNotFoundException, ModuleException, NoSuitableDistributionFoundException, SolverDontFitPetriGameException, UnboundedPGException, CouldNotFindSuitableWinningConditionException {
        final String path = outputDir;
        String name = containerPlaces + "_conTerminal";
        File f = new File(path);
        f.mkdir();
        System.out.println("Generate container terminal...");
        PetriNet pn = ContainerTerminal.createSafetyVersion(containerPlaces, true);
        Tools.savePN2PDF(path + name, pn, false);
        QBFSolver<? extends WinningCondition> solv = QBFSolverFactory.getInstance().getSolver(pn, false, new QBFSolverOptions(15, 3));
        if (hasStrategy) {
            Assert.assertTrue(solv.existsWinningStrategy());
            Tools.savePN2PDF(path + name + "_pg", solv.getStrategy(), true);
        } else {
            Assert.assertFalse(solv.existsWinningStrategy());
        }
    }
}
