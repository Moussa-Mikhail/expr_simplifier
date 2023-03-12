package expressionsimplifier;

@FunctionalInterface
public interface Simplifier {
    SyntaxTree simplify(String operator, SyntaxTree left, SyntaxTree right);
}
