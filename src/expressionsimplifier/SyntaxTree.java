package expressionsimplifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class SyntaxTree {
    public final LexNode node;
    public final @Nullable SyntaxTree left;
    public final @Nullable SyntaxTree right;

    public SyntaxTree(LexNode node) {
        this.node = node;
        this.left = null;
        this.right = null;
    }

    public SyntaxTree(@NotNull LexNode node, @Nullable SyntaxTree left, @Nullable SyntaxTree right) {
        this.node = node;
        this.left = left;
        this.right = right;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public TokenType getType() {
        return node.type;
    }

    public String getToken() {
        return node.token;
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return node.toString();
        } else {
            return String.format("(%s %s %s)", left, node, right);
        }
    }
}
