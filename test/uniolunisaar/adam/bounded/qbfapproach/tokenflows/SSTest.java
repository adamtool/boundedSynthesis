package uniolunisaar.adam.bounded.qbfapproach.tokenflows;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach.QbfSolver;
import uniolunisaar.adam.logic.synthesis.solver.bounded.qbfapproach.QbfSolverFactory;
import uniolunisaar.adam.ds.synthesis.solver.bounded.qbfapproach.QbfSolverOptions;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.generators.pgwt.SecuritySystem;
import uniolunisaar.adam.util.PNWTTools;
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
    public void testSecuritySystem(int intrudingPoints, boolean hasStrategy) throws Exception {
        final String path = outputDir;
        String name = intrudingPoints + "_secSystem";
        File f = new File(path);
        f.mkdir();
        System.out.println("Generate security system ...");
        PetriGameWithTransits pn = SecuritySystem.createSafetyVersionForBounded(intrudingPoints, true);
        PNWTTools.savePnwt2PDF(path + name, pn, false);
        QbfSolver<? extends Condition> solv = QbfSolverFactory.getInstance().getSolver(pn, new QbfSolverOptions(7, 3, false));
        if (hasStrategy) {
            Assert.assertTrue(solv.existsWinningStrategy());
            PNWTTools.savePnwt2PDF(path + name + "_pg", solv.getStrategy(), true);
        } else {
            Assert.assertFalse(solv.existsWinningStrategy());
        }
    }
}
