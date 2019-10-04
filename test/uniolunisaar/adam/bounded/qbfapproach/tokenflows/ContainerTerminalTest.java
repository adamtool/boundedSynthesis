package uniolunisaar.adam.bounded.qbfapproach.tokenflows;

import java.io.File;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolver;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverFactory;
import uniolunisaar.adam.bounded.qbfapproach.solver.QbfSolverOptions;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinet.objectives.Condition;
import uniolunisaar.adam.generators.pg.ContainerTerminal;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.tools.Logger;

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

    @Test(dataProvider = "conTerminal", enabled = false)
    public void testContainerTerminal(int containerPlaces, boolean hasStrategy) throws Exception {
        final String path = outputDir;
        String name = containerPlaces + "_conTerminal";
        File f = new File(path);
        f.mkdir();
        System.out.println("Generate container terminal...");
        PetriGame pn = ContainerTerminal.createSafetyVersion(containerPlaces, true);
        PNWTTools.savePnwt2PDF(path + name, pn, false);
        QbfSolver<? extends Condition> solv = QbfSolverFactory.getInstance().getSolver(pn, false, new QbfSolverOptions(15, 3));
        if (hasStrategy) {
            Assert.assertTrue(solv.existsWinningStrategy());
            PNWTTools.savePnwt2PDF(path + name + "_pg", solv.getStrategy(), true);
        } else {
            Assert.assertFalse(solv.existsWinningStrategy());
        }
    }
}
