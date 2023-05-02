package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class Constants {
    public static final @NotNull Set<String> OPERATOR_TOKENS = Collections.unmodifiableSet(Operator.getOperatorTokens());
    public static final @NotNull List<@NotNull Set<String>> OPERATOR_TOKENS_GROUPED_BY_PRECEDENCE = List.copyOf(Operator.tokensGroupedByPrecedence());
    public static final String NEGATIVE_ONE = "-1";
    public static final String NEGATIVE_SIGN = "-";
    public static final String LEFT_PAREN = "(";
    public static final String RIGHT_PAREN = ")";
    public static final String ADD = "+";
    public static final String SUB = "-";
    public static final String MUL = "*";
    public static final String DIV = "/";
    public static final String POW = "^";

    private Constants() {
    }
}
