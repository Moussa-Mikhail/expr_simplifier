package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


/**
 * @author Moussa
 */
public final class ExpressionLexer {

    private static final char MINUS_SIGN = '-';

    private static final char OPEN_PAREN = '(';

    private static final char CLOSE_PAREN = ')';
    private final @NotNull List<LexNode> lexNodes = new ArrayList<>();
    private final @NotNull String expr;
    private @NotNull String token = "";
    private @Nullable TokenType prevTokenType;

    /**
     *
     */
    private int currPos;

    public ExpressionLexer(@NotNull String expr) {

        this.expr = expr;

    }

    public @NotNull List<LexNode> getLexNodes() {
        return lexNodes;
    }

    public void lexExpression() throws InvalidExpressionException {

        while (currPos < expr.length()) {

            final char chr = expr.charAt(currPos);

            final boolean isAtBeginning = prevTokenType == null;

            final boolean isPrevOperator = prevTokenType == TokenType.OPERATOR;

            if (chr == OPEN_PAREN) {

                lexSubExpr();

            } else if (chr == CLOSE_PAREN) {

                throw new InvalidExpressionException("Unmatched closing parenthesis");

            } else //noinspection AlibabaAvoidComplexCondition
                if (chr == MINUS_SIGN && (isPrevOperator || isAtBeginning)) {

                    token = String.valueOf(chr);

                    currPos++;

                    continue;

                } else if (Operator.getOperatorTokens().contains(String.valueOf(chr))) {

                    lexOperator(chr);

                } else if (Character.isDigit(chr)) {

                    lexNumber();

                } else if (Character.isAlphabetic(chr)) {

                    lexVariable();

                } else {
                    throw new InvalidExpressionException("Invalid character: " + chr);
                }

            lexNodes.add(new LexNode(token, prevTokenType));

            token = "";

        }

    }

    private void lexSubExpr() throws InvalidExpressionException {

        if (prevTokenType == TokenType.SUBEXPR || prevTokenType == TokenType.NUMBER) {

            insertMultiplicationOp();

        } else if (token.equals(String.valueOf(MINUS_SIGN))) {

            lexNodes.add(new LexNode("-1", TokenType.NUMBER));

            token = "";

            insertMultiplicationOp();

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

    private void insertMultiplicationOp() {
        prevTokenType = TokenType.OPERATOR;

        lexNodes.add(new LexNode("*", prevTokenType));
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

    private void lexOperator(char chr) {

        token = String.valueOf(chr);

        prevTokenType = TokenType.OPERATOR;

        currPos++;
    }

    private void lexNumber() {

        final int endIdx = findEndOfNumber(currPos);

        final String numberStr = expr.substring(currPos, endIdx + 1);

        if (token.equals(String.valueOf(MINUS_SIGN))) {

            token = MINUS_SIGN + numberStr;

        } else {

            token = numberStr;
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

            insertMultiplicationOp();
        }

        final int endIdx = findEndOfVariable(currPos);

        final String variableStr = expr.substring(currPos, endIdx + 1);

        token = token.concat(variableStr);

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
