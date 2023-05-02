package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
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
    public final @NotNull String token;
    public final int precedence;
    public final @NotNull BinaryOperator<BigDecimal> function;

    Operator(String token, int precedence, BinaryOperator<BigDecimal> function) {
        this.token = token;
        this.precedence = precedence;
        this.function = function;
    }

    private static @NotNull BigDecimal pow(BigDecimal a, BigDecimal b) {
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

    public static @NotNull BinaryOperator<BigDecimal> getFunction(String token) {
        for (var op : Operator.values()) {
            if (op.token.equals(token)) {
                return op.function;
            }
        }

        throw new IllegalArgumentException("Invalid operator token: " + token);
    }

    public static int getPrecedence(String token) {
        for (var op : Operator.values()) {
            if (op.token.equals(token)) {
                return op.precedence;
            }
        }

        return -1;
    }

    public static @NotNull Set<@NotNull String> getOperatorTokens() {
        Set<String> tokens = new HashSet<>();
        for (var op : Operator.values()) {
            tokens.add(op.token);
        }

        return tokens;
    }

    public static @NotNull Collection<@NotNull Set<@NotNull String>> tokensGroupedByPrecedence() {
        LinkedHashMap<Integer, Set<String>> precedenceToTokensSet = new LinkedHashMap<>();
        for (var op : Operator.values()) {
            int precedence = op.precedence;
            precedenceToTokensSet.putIfAbsent(precedence, new HashSet<>());
            String token = op.token;
            precedenceToTokensSet.get(precedence).add(token);
        }

        return precedenceToTokensSet.values();
    }
}
