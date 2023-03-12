package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static expressionsimplifier.Constants.*;

/**
 * @author Moussa
 */
public final class ExpressionLexer {
    private final @NotNull List<LexNode> lexNodes = new ArrayList<>();
    private final @NotNull String expr;
    private @NotNull String token = "";
    private @Nullable TokenType prevTokenType;
    private int currPos;

    public ExpressionLexer(String expr) {
        this.expr = expr.replaceAll("\\s+", "");
    }

    public @NotNull List<LexNode> getLexNodes() {
        return lexNodes;
    }

    private @NotNull String charAt(int index) {
        return String.valueOf(expr.charAt(index));
    }

    @SuppressWarnings("AlibabaAvoidComplexCondition")
    public void lexExpression() throws InvalidExpressionException {
        while (currPos < expr.length()) {
            String chr = charAt(currPos);
            boolean isAtBeginning = prevTokenType == null;
            boolean isPrevOperator = prevTokenType == TokenType.OPERATOR;

            if (chr.equals(LEFT_PAREN)) {
                lexSubExpr();
            } else if (chr.equals(RIGHT_PAREN)) {
                throw new InvalidExpressionException("Unmatched closing parenthesis");
            } else if (chr.equals(NEGATIVE_SIGN) && (isPrevOperator || isAtBeginning)) {
                handleNegativeSign(chr);
            } else if (OPERATOR_TOKENS.contains(chr)) {
                if (prevTokenType == TokenType.OPERATOR) {
                    throw new InvalidExpressionException("Two operators in a row");
                }

                lexOperator(chr);
            } else if (Character.isDigit(chr.charAt(0))) {
                lexNumber();
            } else if (Character.isAlphabetic(chr.charAt(0))) {
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

        int endIdx = findClosingParen(currPos);
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
        lexNodes.add(new LexNode(MUL, prevTokenType));
    }

    @Contract(pure = true)
    private int findClosingParen(int startIdx) {
        int parenCount = 0;
        for (int idx = startIdx; idx < expr.length(); idx++) {
            String chr = charAt(idx);
            if (LEFT_PAREN.equals(chr)) {
                parenCount++;
            } else if (RIGHT_PAREN.equals(chr)) {
                parenCount--;
            }

            if (parenCount == 0) {
                return idx;
            }
        }

        return -1;
    }

    private void handleNegativeSign(String chr) {
        boolean isNextTokenNumber = Character.isDigit(expr.charAt(currPos + 1));
        currPos++;
        if (isNextTokenNumber) {
            token = chr;
            lexNumber();
        } else {
            // Implicit multiplication case
            token = "-1";
            prevTokenType = TokenType.NUMBER;
        }
    }

    private void lexOperator(String chr) {
        token = chr;
        prevTokenType = TokenType.OPERATOR;
        currPos++;
    }

    private void lexNumber() {
        int endIdx = findEndOfNumber(currPos);
        String numStr = expr.substring(currPos, endIdx + 1);
        if (token.equals(NEGATIVE_SIGN)) {
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

        int endIdx = findEndOfVariable(currPos);
        token = expr.substring(currPos, endIdx + 1);
        prevTokenType = TokenType.VARIABLE;
        currPos = endIdx + 1;
    }

    @Contract(pure = true)
    private int findEndOfVariable(int startIdx) {
        return findEndOfExprComponent(startIdx, chr -> Character.isAlphabetic(chr) || Character.isDigit(chr));
    }

    @Contract(pure = true)
    private int findEndOfExprComponent(int startIdx, Predicate<Character> predicate) {
        for (int idx = startIdx; idx < expr.length(); idx++) {
            char chr = expr.charAt(idx);
            if (!predicate.test(chr)) {
                return idx - 1;
            }
        }

        return expr.length() - 1;
    }
}
