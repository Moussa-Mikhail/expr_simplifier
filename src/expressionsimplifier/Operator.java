package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

public enum Operator {
    // Operators must be ordered by decreasing precedence.
    POW("^", 2, Math::pow),
    MUL("*", 1, (a, b) -> a * b),
    DIV("/", 1, (a, b) -> a / b),
    ADD("+", 0, Double::sum),
    SUB("-", 0, (a, b) -> a - b);

    private final String token;

    private final int precedence;

    private final DoubleBinaryOperator function;

    Operator(String token, int precedence, DoubleBinaryOperator function) {
        this.token = token;
        this.precedence = precedence;
        this.function = function;
    }

    public static DoubleBinaryOperator getFunction(String token) throws IllegalArgumentException {

        for (Operator op : Operator.values()) {

            if (op.isSameToken(token)) {

                return op.function;

            }
        }

        throw new IllegalArgumentException("Invalid operator token: " + token);
    }

    public static int getPrecedence(String token) throws IllegalArgumentException {

        for (Operator op : Operator.values()) {

            if (op.isSameToken(token)) {

                return op.precedence;

            }
        }

        throw new IllegalArgumentException("Invalid operator token: " + token);
    }

    public static @NotNull Set<String> getOperatorTokens() {
        Set<String> tokens = new HashSet<>();

        for (Operator op : Operator.values()) {
            tokens.add(op.token);
        }

        return tokens;
    }

    public static @NotNull Map<Integer, Set<String>> getPrecedenceToTokens () {

        LinkedHashMap<Integer, Set<String>> precedenceToTokens = new LinkedHashMap<>();

        for (Operator op : Operator.values()) {
            var precedence = op.precedence;

            String token = op.token;

            precedenceToTokens.putIfAbsent(precedence, new HashSet<>());

            precedenceToTokens.get(precedence).add(token);
        }

        return precedenceToTokens;
    }
    public boolean isSameToken(String token) {
        return this.token.equals(token);
    }
}
