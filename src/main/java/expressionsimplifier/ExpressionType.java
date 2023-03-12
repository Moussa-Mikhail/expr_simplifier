package expressionsimplifier;

import java.util.Map;

public enum ExpressionType {
    NUMBER,
    VARIABLE,
    SUM,
    DIFF,
    PROD,
    DIV,
    POW,
    EXP,
    // Catchall type for expressions that are not simple
    COMPLEX;
    public static final Map<String, ExpressionType> OPERATOR_TO_EXPRESSION_TYPE = Map.ofEntries(
            Map.entry(Constants.ADD, SUM),
            Map.entry(Constants.SUB, DIFF),
            Map.entry(Constants.MUL, PROD),
            Map.entry(Constants.DIV, DIV),
            Map.entry(Constants.POW, POW)
    );
}
