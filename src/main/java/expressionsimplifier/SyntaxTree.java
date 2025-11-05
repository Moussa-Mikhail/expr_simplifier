package expressionsimplifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static expressionsimplifier.Constants.*;

final class SyntaxTree {
    public final @NotNull LexNode node;
    public final @Nullable SyntaxTree left;
    public final @Nullable SyntaxTree right;
    public static final SyntaxTree ZERO = new SyntaxTree(LexNode.ZERO);
    public static final SyntaxTree ONE = new SyntaxTree(LexNode.ONE);

    public SyntaxTree(LexNode node) {
        this.node = node;
        this.left = null;
        this.right = null;
    }

    public SyntaxTree(LexNode node, @Nullable SyntaxTree left, @Nullable SyntaxTree right) {
        boolean hasNullChild = left == null || right == null;
        if (node.type == TokenType.OPERATOR && hasNullChild) {
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

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public boolean tokenTypeEquals(TokenType type) {
        return node.type == type;
    }

    boolean isNumber() {
        return tokenTypeEquals(TokenType.NUMBER);
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
            if (expr != null) {
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

    private @Nullable String handleImplicitMultiplication() {
        assert left != null;
        assert right != null;

        boolean isRightVariableOrPower = right.tokenTypeEquals(TokenType.VARIABLE) || right.getToken().equals(POW);
        if (left.getToken().equals(NEGATIVE_ONE) && isRightVariableOrPower) {
            return String.format("-%s", right);
        }

        boolean isLeftNumber = left.tokenTypeEquals(TokenType.NUMBER);
        if (isLeftNumber && isRightVariableOrPower ) {
            return String.format("%s%s", left, right);
        }

        if (isLeftNumber && !right.isLeaf()) {
            return String.format("%s(%s)", left, right);
        }

        if (!(left.isLeaf() || right.isLeaf())) {
            return String.format("(%s)(%s)", left, right);
        }

        return null;
    }

    private @NotNull String formatParens(SyntaxTree child) {
        // Expressions are parenthesized by default
        String childString = child.toString();
        childString = String.format("(%s)", childString);

        if (child.getPrecedence() >= this.getPrecedence()) {
            childString = Utils.removeParens(childString);
        }

        if (child.isLeaf()) {
            childString = Utils.removeParens(childString);

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

    @SuppressWarnings("java:S3776")
    public ExpressionType getExpressionType() {
        if (isLeaf()) {
            if (node.type == TokenType.NUMBER) {
                return ExpressionType.NUMBER;
            }

            if (node.type == TokenType.VARIABLE) {
                return ExpressionType.VARIABLE;
            }
        }

        assert left != null;
        assert right != null;
        if (left.isLeaf() && right.isLeaf()) {
            if (node.token.equals(POW)) {
                if (left.expressionTypeEquals(ExpressionType.VARIABLE) && right.expressionTypeEquals(ExpressionType.NUMBER)) {
                    return ExpressionType.UNIPOLY;
                }

                if (left.expressionTypeEquals(ExpressionType.NUMBER) && !right.expressionTypeEquals(ExpressionType.NUMBER)) {
                    return ExpressionType.EXP;
                }

                return ExpressionType.COMPLEX;
            }
            return ExpressionType.OPERATOR_TO_EXPRESSION_TYPE.get(node.token);
        }

        return ExpressionType.COMPLEX;
    }

    public boolean expressionTypeEquals(ExpressionType type) {
        return getExpressionType() == type;
    }
}
