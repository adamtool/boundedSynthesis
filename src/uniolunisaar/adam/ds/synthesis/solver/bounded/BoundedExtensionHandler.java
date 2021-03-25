package uniolunisaar.adam.ds.synthesis.solver.bounded;

import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.util.AdamBoundedExtensions;
import uniolunisaar.adam.util.ExtensionManagement;

/**
 * Used to handle the extensions for the bounded approach.
 *
 * @author Manuel Gieseking
 */
public class BoundedExtensionHandler {

    // register the Extensions for the bounded approach
    static {
        ExtensionManagement.getInstance().registerExtensions(true, AdamBoundedExtensions.values());
    }

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% NET EXTENSIONS
    public static void setBoundN(PetriGameWithTransits game, Integer n) {
        ExtensionManagement.getInstance().putExtension(game, AdamBoundedExtensions.n, n);
    }

    public static Integer getBoundN(PetriGameWithTransits game) {
        return ExtensionManagement.getInstance().getExtension(game, AdamBoundedExtensions.n, Integer.class);
    }

    public static boolean hasBoundN(PetriGameWithTransits game) {
        return ExtensionManagement.getInstance().hasExtension(game, AdamBoundedExtensions.n);
    }

    public static void setBoundB(PetriGameWithTransits game, Integer n) {
        ExtensionManagement.getInstance().putExtension(game, AdamBoundedExtensions.b, n);
    }

    public static Integer getBoundB(PetriGameWithTransits game) {
        return ExtensionManagement.getInstance().getExtension(game, AdamBoundedExtensions.b, Integer.class);
    }

    public static boolean hasBoundB(PetriGameWithTransits game) {
        return ExtensionManagement.getInstance().hasExtension(game, AdamBoundedExtensions.b);
    }

}
