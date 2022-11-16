package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

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

}
