package expressionsimplifier;

import java.util.Collections;
import java.util.Set;

public final class Constants {
    public static final String NEGATIVE_SIGN = "-";
    public static final String LEFT_PAREN = "(";
    public static final String RIGHT_PAREN = ")";
    public static final Set<String> OPERATOR_TOKENS = Collections.unmodifiableSet(Operator.getOperatorTokens());
    public static final String NEGATIVE_ONE = "-1";
    public static final String ADD = "+";
    public static final String SUB = "-";
    public static final String MUL = "*";
    public static final String DIV = "/";
    public static final String POW = "^";

    private Constants() {
    }
}
