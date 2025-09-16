package expressionsimplifier;

@SuppressWarnings("unused")
@FunctionalInterface
interface Simplifier {
    SyntaxTree simplify(String operator, SyntaxTree left, SyntaxTree right);
}
