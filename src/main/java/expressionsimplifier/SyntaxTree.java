package expressionsimplifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static expressionsimplifier.Constants.*;

final class SyntaxTree {
    public final @NotNull LexNode node;
    public final @Nullable SyntaxTree left;
    public final @Nullable SyntaxTree right;

    public SyntaxTree(@NotNull LexNode node) {
        this.node = node;
        this.left = null;
        this.right = null;
    }

    public SyntaxTree(@NotNull LexNode node, @Nullable SyntaxTree left, @Nullable SyntaxTree right) {
        boolean bothNonNull = left != null && right != null;
        if (node.type == TokenType.OPERATOR && !bothNonNull) {
            throw new IllegalArgumentException("Operator nodes must have non-null children");
        }

        boolean bothNull = left == null && right == null;
        if (node.type != TokenType.OPERATOR && !bothNull) {
            throw new IllegalArgumentException("Non-operator nodes must have null children");
        }

        this.node = node;
        this.left = left;
        this.right = right;
    }

    private static @NotNull String removeParens(@NotNull String expr) {
        String firstChar = String.valueOf(expr.charAt(0));
        String lastChar = String.valueOf(expr.charAt(expr.length() - 1));
        if (firstChar.equals(LEFT_PAREN) && lastChar.equals(RIGHT_PAREN)) {
            return expr.substring(1, expr.length() - 1);
        }

        return expr;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public @NotNull TokenType getType() {
        return node.type;
    }

    public @NotNull String getToken() {
        return node.token;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SyntaxTree that = (SyntaxTree) o;

        if (!node.equals(that.node)) {
            return false;
        }

        if (!Objects.equals(left, that.left)) {
            return false;
        }

        return Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, left, right);
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return node.toString();
        }

        assert left != null;
        assert right != null;

        // Implicit multiplication
        if (node.token.equals(MUL)) {
            String expr = handleImplicitMultiplication();
            if (!expr.isEmpty()) {
                return expr;
            }
        }

        String leftString = formatParens(left);
        String rightString = formatParens(right);

        if (node.token.equals(ADD) || node.token.equals(SUB)) {
            return String.format("%s %s %s", leftString, node, rightString);
        }

        return String.format("%s%s%s", leftString, node, rightString);
    }

    private @NotNull String handleImplicitMultiplication() {
        assert left != null;
        assert right != null;

        boolean isRightVariable = right.getType() == TokenType.VARIABLE;
        if (left.getToken().equals(NEGATIVE_ONE) && isRightVariable) {
            return String.format("-%s", right);
        }

        boolean isLeftNumber = left.getType() == TokenType.NUMBER;
        if (isLeftNumber && isRightVariable) {
            return String.format("%s%s", left, right);
        }

        boolean isLeftLeaf = left.isLeaf();
        boolean isRightLeaf = right.isLeaf();
        if (isLeftNumber && !isRightLeaf) {
            return String.format("%s(%s)", left, right);
        }

        if (!(isLeftLeaf || isRightLeaf)) {
            return String.format("(%s)(%s)", left, right);
        }

        return "";
    }

    private @NotNull String formatParens(@NotNull SyntaxTree child) {
        // Expressions are parenthesized by default
        String childString = child.toString();
        childString = String.format("(%s)", childString);

        if (child.getPrecedence() >= this.getPrecedence()) {
            childString = removeParens(childString);
        }

        if (child.isLeaf()) {
            childString = removeParens(childString);

            if (childString.startsWith(SUB)) {
                childString = String.format("(%s)", childString);
            }
        }

        return childString;
    }

    private int getPrecedence() {
        if (isLeaf()) {
            return -1;
        }

        return Operator.getPrecedence(getToken());
    }
}
