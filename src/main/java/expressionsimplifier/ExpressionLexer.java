package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


/**
 * @author Moussa
 */
public final class ExpressionLexer {

    private static final String MULT_TOKEN = "*";
    private static final char NEGATIVE_SIGN = '-';
    private static final char OPEN_PAREN = '(';
    private static final char CLOSE_PAREN = ')';
    private static final @NotNull Set<String> OPERATOR_TOKENS = Operator.getOperatorTokens();
    private final @NotNull List<LexNode> lexNodes = new ArrayList<>();
    private final @NotNull String expr;
    private @NotNull String token = "";
    private @Nullable TokenType prevTokenType;
    private int currPos;

    public ExpressionLexer(@NotNull String expr) {

        this.expr = expr.replaceAll("\\s+", "");
    }

    public @NotNull List<LexNode> getLexNodes() {

        return new ArrayList<>(lexNodes);
    }

    @SuppressWarnings("AlibabaAvoidComplexCondition")
    public void lexExpression() throws InvalidExpressionException {

        while (currPos < expr.length()) {

            final char chr = expr.charAt(currPos);

            final boolean isAtBeginning = prevTokenType == null;

            final boolean isPrevOperator = prevTokenType == TokenType.OPERATOR;

            if (chr == OPEN_PAREN) {

                lexSubExpr();

            } else if (chr == CLOSE_PAREN) {

                throw new InvalidExpressionException("Unmatched closing parenthesis");

            } else if (chr == NEGATIVE_SIGN && (isPrevOperator || isAtBeginning)) {

                handleNegativeSign(chr);

            } else if (OPERATOR_TOKENS.contains(String.valueOf(chr))) {

                if (prevTokenType == TokenType.OPERATOR) {

                    throw new InvalidExpressionException("Two operators in a row");
                }

                lexOperator(chr);

            } else if (Character.isDigit(chr)) {

                lexNumber();

            } else if (Character.isAlphabetic(chr)) {

                lexVariable();

            } else {

                throw new InvalidExpressionException("Invalid character: " + chr);
            }

            assert prevTokenType != null;

            lexNodes.add(new LexNode(token, prevTokenType));

            token = "";

        }

    }

    private void lexSubExpr() throws InvalidExpressionException {

        if (hasImplicitMultiplication()) {

            appendMultiplicationOp();

        } else if (prevTokenType == TokenType.VARIABLE) {

            throw new InvalidExpressionException("Invalid expression.");
        }

        final int endIdx = findClosingParen(currPos);

        if (endIdx == -1) {

            throw new InvalidExpressionException("Unmatched opening parenthesis");
        }

        token = expr.substring(currPos, endIdx + 1);

        prevTokenType = TokenType.SUBEXPR;

        currPos = endIdx + 1;
    }

    private boolean hasImplicitMultiplication() {

        return prevTokenType == TokenType.SUBEXPR || prevTokenType == TokenType.NUMBER;
    }

    private void appendMultiplicationOp() {

        prevTokenType = TokenType.OPERATOR;

        lexNodes.add(new LexNode(MULT_TOKEN, prevTokenType));
    }

    @Contract(pure = true)
    private int findClosingParen(int startIdx) {

        int parenCount = 0;

        for (int idx = startIdx; idx < expr.length(); idx++) {

            final char chr = expr.charAt(idx);

            if (chr == OPEN_PAREN) {

                parenCount++;

            } else if (chr == CLOSE_PAREN) {

                parenCount--;
            }
            if (parenCount == 0) {

                return idx;
            }
        }

        return -1;
    }

    private void handleNegativeSign(char chr) {

        final boolean isNextTokenNumber = Character.isDigit(expr.charAt(currPos + 1));

        currPos++;

        if (isNextTokenNumber) {

            token = String.valueOf(chr);

            lexNumber();

        } else {
            // Implicit multiplication case

            token = "-1";

            prevTokenType = TokenType.NUMBER;

        }
    }

    private void lexOperator(char chr) {

        token = String.valueOf(chr);

        prevTokenType = TokenType.OPERATOR;

        currPos++;
    }

    private void lexNumber() {

        final int endIdx = findEndOfNumber(currPos);

        final String numStr = expr.substring(currPos, endIdx + 1);

        if (token.equals(String.valueOf(NEGATIVE_SIGN))) {

            token += numStr;

        } else {

            token = numStr;
        }

        prevTokenType = TokenType.NUMBER;

        currPos = endIdx + 1;
    }

    @Contract(pure = true)
    private int findEndOfNumber(int startIdx) {

        return findEndOfExprComponent(startIdx, chr -> Character.isDigit(chr) || chr == '.');
    }

    private void lexVariable() {

        if (prevTokenType == TokenType.NUMBER) {

            appendMultiplicationOp();
        }

        final int endIdx = findEndOfVariable(currPos);

        token = expr.substring(currPos, endIdx + 1);

        prevTokenType = TokenType.VARIABLE;

        currPos = endIdx + 1;
    }

    @Contract(pure = true)
    private int findEndOfVariable(int startIdx) {

        return findEndOfExprComponent(startIdx, chr -> Character.isAlphabetic(chr) || Character.isDigit(chr));
    }

    @Contract(pure = true)
    private int findEndOfExprComponent(int startIdx, @NotNull Predicate<Character> predicate) {

        for (int idx = startIdx; idx < expr.length(); idx++) {

            final char chr = expr.charAt(idx);

            if (!predicate.test(chr)) {
                return idx - 1;
            }
        }

        return expr.length() - 1;
    }
}
