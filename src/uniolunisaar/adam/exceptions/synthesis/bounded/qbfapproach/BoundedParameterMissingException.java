package uniolunisaar.adam.exceptions.synthesis.bounded.qbfapproach;

import uniolunisaar.adam.exceptions.synthesis.pgwt.ParameterMissingException;

/**
 *
 * @author Manuel Gieseking
 */
public class BoundedParameterMissingException extends ParameterMissingException {

    private static final long serialVersionUID = 1L;

    public BoundedParameterMissingException(int n, int b) {
        super("No suitable parametes found: n=" + n + ", b=" + b);
    }

}
