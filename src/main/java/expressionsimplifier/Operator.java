package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.BinaryOperator;

/**
 * @author Moussa
 */
public enum Operator {
    // Operators must be ordered by decreasing precedence.
    POW(Constants.POW, 2, Operator::pow),
    MUL(Constants.MUL, 1, BigDecimal::multiply),
    DIV(Constants.DIV, 1, BigDecimal::divide),
    ADD(Constants.ADD, 0, BigDecimal::add),
    SUB(Constants.SUB, 0, BigDecimal::subtract);
    public final String token;
    public final int precedence;
    public final BinaryOperator<BigDecimal> function;

    Operator(String token, int precedence, BinaryOperator<BigDecimal> function) {
        this.token = token;
        this.precedence = precedence;
        this.function = function;
    }

    private static @NotNull BigDecimal pow(@NotNull BigDecimal a, @NotNull BigDecimal b) {
        try {
            BigInteger bigIntegerA = a.toBigIntegerExact();
            BigInteger bigIntegerB = b.toBigIntegerExact();
            double pow = Math.pow(bigIntegerA.doubleValue(), bigIntegerB.doubleValue());
            return BigDecimal.valueOf((long) pow);
        } catch (ArithmeticException e) {
            double pow = Math.pow(a.doubleValue(), b.doubleValue());
            return BigDecimal.valueOf(pow);
        }
    }

    public static BinaryOperator<BigDecimal> getFunction(String token) {
        for (Operator op : Operator.values()) {
            if (op.token.equals(token)) {
                return op.function;
            }
        }

        throw new IllegalArgumentException("Invalid operator token: " + token);
    }

    public static int getPrecedence(String token) {
        for (Operator op : Operator.values()) {
            if (op.token.equals(token)) {
                return op.precedence;
            }
        }

        return -1;
    }

    public static @NotNull Set<String> getOperatorTokens() {
        Set<String> tokens = new HashSet<>();
        for (Operator op : Operator.values()) {
            tokens.add(op.token);
        }

        return tokens;
    }

    public static @NotNull Collection<Set<String>> tokensGroupedByPrecedence() {
        LinkedHashMap<Integer, Set<String>> precedenceToTokens = new LinkedHashMap<>();
        for (Operator op : Operator.values()) {
            int precedence = op.precedence;
            precedenceToTokens.putIfAbsent(precedence, new HashSet<>());
            String token = op.token;
            precedenceToTokens.get(precedence).add(token);
        }

        return precedenceToTokens.values();
    }
}
