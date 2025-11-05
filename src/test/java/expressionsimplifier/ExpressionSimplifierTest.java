package expressionsimplifier;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionSimplifierTest {
    public static @NotNull Stream<Arguments> expressions() {
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
                Arguments.of("3*0.1+(-0.1)", "0.2"),
                Arguments.of("1", "1"),
                Arguments.of("1+1", "2"),
                Arguments.of("-1", "-1"),
                Arguments.of("x", "x"),
                Arguments.of("x+y", "x + y"),
                Arguments.of("x+1", "x + 1"),
                Arguments.of("(-1+2)+x", "x + 1"),
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
                Arguments.of("1+(2*x)", "2x + 1"),
                Arguments.of("1", "1"),
                Arguments.of("1+x", "x + 1"),
                Arguments.of("x+1", "x + 1"),
                Arguments.of("x+x^2", "x^2 + x"),
                Arguments.of("x^2+x", "x^2 + x"),
                Arguments.of("x^2+x^3", "x^3 + x^2"),
                Arguments.of("x*2", "2x"),
                Arguments.of("2*x", "2x"),
                Arguments.of("x*2*y", "2x*y"),
                Arguments.of("1*x", "x"),
                Arguments.of("x*1", "x"),
                Arguments.of("x+0", "x"),
                Arguments.of("(x+y^2)-(x+y^2)", "0"),
                Arguments.of("x^2-x^2", "0"),
                Arguments.of("x*0", "0"),
                Arguments.of("0*(1+2x+3x^2)", "0"),
                Arguments.of("x/x", "1"),
                Arguments.of("x^2/x^2", "1"),
                Arguments.of("(x-1+y*2+z)/(x-1+y*2+z)", "1"),
                Arguments.of("x^1", "x"),
                Arguments.of("x^0", "1"),
                Arguments.of("(2x+3y-1z)^1", "2x + 3y - z"),
                Arguments.of("(2x+3y-1z)^0", "1"),
                Arguments.of("0^0", "1"),
                Arguments.of("0^2", "0"),
                Arguments.of("-(-2)(x+y)", "2(x + y)")
        );
    }

    public static @NotNull Stream<Arguments> expressionsWithAssignedVariables() {
        return Stream.of(
                Arguments.of("1+2+3", List.of("x=1"), "6"),
                Arguments.of("x", List.of("x=1"), "1"),
                Arguments.of("2x-(-3)*4+x*x", List.of("x=2"), "20"),
                Arguments.of("x*y", List.of("x=1"), "y"),
                Arguments.of("x*y", List.of("x=2", "y=-3.2"), "-6.4"),
                Arguments.of("x*y", List.of("z=1"), "x*y")
        );
    }

    public static @NotNull Stream<Arguments> invalidExpressions() {
        return Stream.of(
                Arguments.of("."),
                Arguments.of("--"),
                Arguments.of("1+"),
                Arguments.of("1+1+"),
                Arguments.of("1++"),
                Arguments.of("1*+1"),
                Arguments.of("1//2"),
                Arguments.of("1/0"),
                Arguments.of("1/0.0"),
                Arguments.of("0^(-1)"),
                Arguments.of("0^(-0.1)"),
                Arguments.of("(-1)^(1/2)")
        );
    }

    @ParameterizedTest
    @MethodSource("expressions")
    void simplifyExpressionsTest(String expr, String expected) throws InvalidExpressionException {
        String actual = ExpressionSimplifier.simplifyExpr(expr);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("expressionsWithAssignedVariables")
    void evaluateAlgebraTest(String expr, List<String> variableValues, String expected) throws InvalidExpressionException {
        String actual = ExpressionSimplifier.simplifyExpr(expr, variableValues);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("expressions")
    void idempotentTest(String expr) throws InvalidExpressionException {
        String simplifiedExpr = ExpressionSimplifier.simplifyExpr(expr);
        String reSimplifiedExpr = ExpressionSimplifier.simplifyExpr(simplifiedExpr);
        assertEquals(simplifiedExpr, reSimplifiedExpr);
    }

    @ParameterizedTest
    @MethodSource("invalidExpressions")
    void invalidExpressionsTest(String expr) {
        assertThrows(InvalidExpressionException.class, () -> ExpressionSimplifier.simplifyExpr(expr));
    }
}