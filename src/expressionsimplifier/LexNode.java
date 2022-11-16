package expressionsimplifier;

final class LexNode {
    public final String token;
    public final TokenType type;

    public LexNode(String token, TokenType type) {
        this.token = token;
        this.type = type;
    }

    @Override
    public String toString() {
        return token;
    }

}
