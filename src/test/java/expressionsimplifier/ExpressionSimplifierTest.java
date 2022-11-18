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
                Arguments.of("1+(-1)", "0"),
                Arguments.of("-1*(-1)", "1"),
                Arguments.of("-1*-1", "1"),
                Arguments.of("1+2*3-(-3)*(-1)+(-1)(-1^5-2^3)", "13")
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

    @ParameterizedTest
    @MethodSource("arithmeticExpressions")
    void evaluateArithmeticTest(@NotNull String expr, @NotNull String expected) throws InvalidExpressionException {

        final var actual = ExpressionSimplifier.simplifyExpr(expr);

        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("algebraExpressionsWithValues")
    void evaluateAlgebraTest(@NotNull String expr, @NotNull List<String> variableValues, String expected) throws InvalidExpressionException {

        final var actual = ExpressionSimplifier.simplifyExpr(expr, variableValues.toArray(String[]::new));

        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("expressions")
    void idempotentTest(@NotNull String expr) throws InvalidExpressionException {

        final var simplifiedExpr = ExpressionSimplifier.simplifyExpr(expr);

        final var reSimplifiedExpr = ExpressionSimplifier.simplifyExpr(simplifiedExpr);

        assertEquals(simplifiedExpr, reSimplifiedExpr);
    }
}