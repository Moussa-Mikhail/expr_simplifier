package expressionsimplifier;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionSimplifierTest {
    public static @NotNull Stream<Arguments> arithmeticExpressions() {
        return Stream.of(
                Arguments.of("1", "1"),
                Arguments.of("1+1", "2"),
                Arguments.of("0.1+0.1+0.1", "0.3"),
                Arguments.of("0.1+(-0.1)", "0.0"),
                Arguments.of("1+(-1)", "0"),
                Arguments.of("-1*(-1)", "1"),
                Arguments.of("-1*-1", "1"),
                Arguments.of("1+2*3-(-3)*(-1)+(-1)(-1^5-2^3)", "13"),
                Arguments.of("(-1)^3", "-1"),
                Arguments.of("3*0.1+(-0.1)", "0.2")
        );
    }

    public static @NotNull Stream<Arguments> algebraExpressionsWithValues() {
        return Stream.of(
                Arguments.of("x", List.of("x=1"), "1"),
                Arguments.of("-x", List.of("x=1"), "-1"),
                Arguments.of("x", List.of("x=-1"), "-1"),
                Arguments.of("2x-(-3)*4+x*x", List.of("x=2"), "20")
        );
    }

    public static @NotNull Stream<Arguments> expressions() {
        return Stream.of(
                Arguments.of("x"),
                Arguments.of("-x"),
                Arguments.of("2*x"),
                Arguments.of("2*x-3"),
                Arguments.of("2*x-(-3)*4"),
                Arguments.of("-1*x*x*(-1)"),
                Arguments.of("x*y+x-y/y-2(x+y)")
        );
    }

    public static @NotNull Stream<Arguments> expressionsToBeParenthesized() {
        return Stream.of(
                Arguments.of("1", "1"),
                Arguments.of("1+1", "2"),
                Arguments.of("-1", "-1"),
                Arguments.of("x", "x"),
                Arguments.of("x+y", "x + y"),
                Arguments.of("x+1", "x + 1"),
                Arguments.of("(-1+2)+x", "1 + x"),
                Arguments.of("-x", "-x"),
                Arguments.of("x-y", "x - y"),
                Arguments.of("x+y+1", "x + y + 1"),
                Arguments.of("(x+y)*(x-y)", "(x + y)(x - y)"),
                Arguments.of("(x+y)(x-y)", "(x + y)(x - y)"),
                Arguments.of("2*x", "2x"),
                Arguments.of("2x", "2x"),
                Arguments.of("2*(x+y)", "2(x + y)"),
                Arguments.of("2(x+y)", "2(x + y)"),
                Arguments.of("x*y*z", "x*y*z"),
                Arguments.of("2*-x", "-2x"),
                Arguments.of("1+(2*x)", "1 + 2x")
        );
    }

    public static @NotNull Stream<Arguments> expressionsToBeStandardized() {
        return Stream.of(
                Arguments.of("1", "1"),
                Arguments.of("1+x", "x + 1"),
                Arguments.of("x+1", "x + 1"),
                Arguments.of("x+x^2", "x^2 + x"),
                Arguments.of("x^2+x", "x^2 + x"),
                Arguments.of("x^2+x^3", "x^3 + x^2"),
                Arguments.of("x*2", "2*x"),
                Arguments.of("2*x", "2*x"),
                Arguments.of("x*2*y", "2*x*y")
        );
    }

    @ParameterizedTest
    @MethodSource("arithmeticExpressions")
    void simplifyArithmeticExpressionsTest(@NotNull String expr, @NotNull String expected) throws InvalidExpressionException {
        String actual = ExpressionSimplifier.simplifyExpr(expr);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("expressionsToBeParenthesized")
    void simplifyExpressionsWithCorrectParensTest(@NotNull String expr, @NotNull String expected) throws InvalidExpressionException {
        String actual = ExpressionSimplifier.simplifyExpr(expr);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("expressionsToBeStandardized")
    void standardizeExpressionsTest(@NotNull String expr, @NotNull String expected) throws InvalidExpressionException {
        String actual = ExpressionSimplifier.simplifyExpr(expr);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("algebraExpressionsWithValues")
    void evaluateAlgebraTest(@NotNull String expr, @NotNull List<String> variableValues, String expected) throws InvalidExpressionException {
        String actual = ExpressionSimplifier.simplifyExpr(expr, variableValues.toArray(String[]::new));
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("expressions")
    void idempotentTest(@NotNull String expr) throws InvalidExpressionException {
        String simplifiedExpr = ExpressionSimplifier.simplifyExpr(expr);
        String reSimplifiedExpr = ExpressionSimplifier.simplifyExpr(simplifiedExpr);
        assertEquals(simplifiedExpr, reSimplifiedExpr);
    }
}