package expressionsimplifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class LexNode {
    public final @NotNull String token;
    public final @NotNull TokenType type;
    public static final LexNode MUL = new LexNode(Constants.MUL, TokenType.OPERATOR);
    public static final LexNode POW = new LexNode(Constants.POW, TokenType.OPERATOR);
    public static final LexNode DIV = new LexNode(Constants.DIV, TokenType.OPERATOR);
    public static final LexNode ZERO = new LexNode("0", TokenType.NUMBER);
    public static final LexNode ONE = new LexNode("1", TokenType.NUMBER);

    public LexNode(String token, TokenType type) {
        this.token = token;
        this.type = type;
    }

    @Override
    public @NotNull String toString() {
        return token;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LexNode lexNode = (LexNode) o;

        if (!token.equals(lexNode.token)) {
            return false;
        }
        return type == lexNode.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, type);
    }
}
