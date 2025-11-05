package expressionsimplifier;

import java.util.Map;

public enum ExpressionType {
    NUMBER,
    VARIABLE,
    // Univariate Polynomials
    UNIPOLY,
    EXP,
    // Catchall type for expressions that are not simple
    COMPLEX;

    public static final Map<String, ExpressionType> OPERATOR_TO_EXPRESSION_TYPE = Map.ofEntries(
            Map.entry(Constants.POW, UNIPOLY)
    );
}
