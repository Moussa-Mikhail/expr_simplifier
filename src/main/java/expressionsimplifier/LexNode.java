package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class LexNode {
    public final @NotNull String token;
    public final @NotNull TokenType type;

    public LexNode(@NotNull String token, @NotNull TokenType type) {
        this.token = token;
        this.type = type;
    }

    @Override
    public @NotNull String toString() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
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
