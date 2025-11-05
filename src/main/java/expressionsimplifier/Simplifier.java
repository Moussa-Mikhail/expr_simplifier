package expressionsimplifier;

@SuppressWarnings("unused")
@FunctionalInterface
interface Simplifier {
    SyntaxTree simplify(SyntaxTree tree);
}
