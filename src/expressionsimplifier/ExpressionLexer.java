package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ExpressionLexer {

    private ExpressionLexer() {
        throw new IllegalStateException("Utility class");
    }

    static LexNode @NotNull [] lexExpression(String expr) throws InvalidExpressionException {

        expr = expr.replaceAll("\\s+", "");

        ArrayList<LexNode> lexNodes = new ArrayList<>();

        String tokenExpr = "";

        TokenType tokenType = null;

        int idx = 0;

        while (idx < expr.length()) {

            char chr = expr.charAt(idx);

            if (chr == '(') {

                int endIdx = findClosingParen(expr, idx);

                if (endIdx == -1) {
                    throw new InvalidExpressionException("Unmatched opening parenthesis");
                }

                tokenExpr = expr.substring(idx, endIdx + 1);

                tokenType = TokenType.SUBEXPR;

                idx = endIdx + 1;

            } else if (chr == ')') {

                throw new InvalidExpressionException("Unmatched closing parenthesis");

            } else if (chr == '-' && (tokenType == TokenType.OPERATOR || tokenType == null)) {

                tokenExpr = String.valueOf(chr);

                tokenType = TokenType.NUMBER;

                idx++;

                continue;

            } else if (chr == '+' || chr == '*' || chr == '/' || chr == '-') {

                tokenExpr = String.valueOf(chr);

                tokenType = TokenType.OPERATOR;

                idx++;

            } else if (Character.isDigit(chr)) {

                int endIdx = findEndOfNumber(expr, idx);

                String numberStr = expr.substring(idx, endIdx + 1);

                tokenExpr = tokenExpr.concat(numberStr);

                tokenType = TokenType.NUMBER;

                idx = endIdx + 1;

            } else if (Character.isAlphabetic(chr)) {

                int endIdx = findEndOfVariable(expr, idx);

                String variableStr = expr.substring(idx, endIdx + 1);

                tokenExpr = tokenExpr.concat(variableStr);

                tokenType = TokenType.VARIABLE;

                idx = endIdx + 1;

            } else {
                throw new InvalidExpressionException("Invalid character: " + chr);
            }

            LexNode lexNode = new LexNode(tokenExpr, tokenType);

            lexNodes.add(lexNode);

            tokenExpr = "";

        }

        return lexNodes.toArray(new LexNode[0]);

    }

    private static int findClosingParen(@NotNull String expr, int startIdx) {
//        This function assumes that the first character at startIdx is an opening parenthesis.
//        It returns the index of the closing parenthesis. -1 if unmatched.

        int parenCount = 0;

        for (int idx = startIdx; idx < expr.length(); idx++) {

            char chr = expr.charAt(idx);

            if (chr == '(') {
                parenCount++;
            } else if (chr == ')') {
                parenCount--;
            }
            if (parenCount == 0) {
                return idx;
            }
        }
        return -1;
    }

    private static int findEndOfNumber(@NotNull String expr, int startIdx) {

        for (int idx = startIdx; idx < expr.length(); idx++) {

            char chr = expr.charAt(idx);

            if (!Character.isDigit(chr)) {
                return idx - 1;
            }
        }

        return expr.length() - 1;
    }

    private static int findEndOfVariable(@NotNull String expr, int startIdx) {

        for (int idx = startIdx; idx < expr.length(); idx++) {

            char chr = expr.charAt(idx);

            if (!Character.isAlphabetic(chr)) {
                return idx - 1;
            }
        }

        return expr.length() - 1;
    }
}
