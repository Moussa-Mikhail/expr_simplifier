package expressionsimplifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Predicate;

public final class ExpressionLexer {

    private final @NotNull String expr;

    private @NotNull String token = "";

    private @Nullable TokenType prevTokenType = null;

    private int idx = 0;

    final ArrayList<LexNode> lexNodes = new ArrayList<>();

    public ExpressionLexer(@NotNull String expr) {

        this.expr = expr;

    }

    void lexExpression() throws InvalidExpressionException {

        while (idx < expr.length()) {

            char chr = expr.charAt(idx);

            if (chr == '(') {

                handleOpenParen();

            } else if (chr == ')') {

                throw new InvalidExpressionException("Unmatched closing parenthesis");

            } else if (chr == '-' && (prevTokenType == TokenType.OPERATOR || prevTokenType == null)) {

                handleNegativeSign(chr);

                continue;

            } else if (Operator.getOperatorTokens().contains(String.valueOf(chr))) {

                handleOperator(chr);

            } else if (Character.isDigit(chr)) {

                handleNumber();

            } else if (Character.isAlphabetic(chr)) {

                handleVariable();

            } else {
                throw new InvalidExpressionException("Invalid character: " + chr);
            }

            LexNode lexNode = new LexNode(token, prevTokenType);

            lexNodes.add(lexNode);

            token = "";

        }

    }

    private void handleVariable() {

        if (prevTokenType == TokenType.NUMBER) {

            insertMultiplicationOp();
        }

        int endIdx = findEndOfVariable(idx);

        String variableStr = expr.substring(idx, endIdx + 1);

        token = token.concat(variableStr);

        prevTokenType = TokenType.VARIABLE;

        idx = endIdx + 1;
    }

    private void handleNumber() {

        int endIdx = findEndOfNumber(idx);

        String numberStr = expr.substring(idx, endIdx + 1);

        token = token.concat(numberStr);

        prevTokenType = TokenType.NUMBER;

        idx = endIdx + 1;
    }

    private void handleOperator(char chr) {

        token = String.valueOf(chr);

        prevTokenType = TokenType.OPERATOR;

        idx++;
    }

    private void handleNegativeSign(char chr) {

        token = String.valueOf(chr);

        prevTokenType = TokenType.NUMBER;

        idx++;
    }

    private void handleOpenParen() throws InvalidExpressionException {

        if (prevTokenType == TokenType.SUBEXPR || prevTokenType == TokenType.NUMBER) {

            insertMultiplicationOp();
        }

        int endIdx = findClosingParen(idx);

        if (endIdx == -1) {
            throw new InvalidExpressionException("Unmatched opening parenthesis");
        }

        token = expr.substring(idx, endIdx + 1);

        prevTokenType = TokenType.SUBEXPR;

        idx = endIdx + 1;
    }

    private void insertMultiplicationOp() {
        prevTokenType = TokenType.OPERATOR;

        lexNodes.add(new LexNode("*", prevTokenType));
    }

    private int findClosingParen(int startIdx) {
//        This function assumes that the first character at startIdx is an opening parenthesis.
//        It returns the index of the closing parenthesis. -1 if it is unmatched.

        int parenCount = 0;

        for (int idx_ = startIdx; idx_ < expr.length(); idx_++) {

            char chr = expr.charAt(idx_);

            if (chr == '(') {
                parenCount++;
            } else if (chr == ')') {
                parenCount--;
            }
            if (parenCount == 0) {
                return idx_;
            }
        }
        return -1;
    }

    private int findEndOfNumber(int startIdx) {

        return findEndOfExprComponent(startIdx, chr -> Character.isDigit(chr) || chr == '.');
    }

    private int findEndOfVariable(int startIdx) {

        return findEndOfExprComponent(startIdx, chr -> Character.isAlphabetic(chr) || Character.isDigit(chr));
    }

    private int findEndOfExprComponent(int startIdx, @NotNull Predicate<Character> predicate) {

    	for (int idx_ = startIdx; idx_ < expr.length(); idx_++) {

    		char chr = expr.charAt(idx_);

    		if (!predicate.test(chr)) {
    			return idx_ - 1;
    		}
    	}

    	return expr.length() - 1;
    }


}
