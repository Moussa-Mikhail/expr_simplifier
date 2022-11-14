package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

class SyntaxTree {
    public final LexNode node;
    public final SyntaxTree left;
    public final SyntaxTree right;

    public SyntaxTree(LexNode node) {
        this.node = node;
        this.left = null;
        this.right = null;
    }

    public SyntaxTree(LexNode node, SyntaxTree left, SyntaxTree right) {
        this.node = node;
        this.left = left;
        this.right = right;
    }

    @Contract(pure = true)
    public SyntaxTree(@NotNull SyntaxTree tree) {
        this.node = tree.node;
        this.left = tree.left;
        this.right = tree.right;
    }

    public boolean isLeaf() {
        return left == null && right == null;
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
