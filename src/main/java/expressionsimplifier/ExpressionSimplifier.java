package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Moussa
 */
public final class ExpressionSimplifier {
    private ExpressionSimplifier() {
    }

    @Contract(pure = true)
    public static void main(String @NotNull ... args) {

        if (args.length == 0) {
            return;
        }

        final String[] variableValues = new String[args.length - 1];

        if (args.length > 1) {
            System.arraycopy(args, 1, variableValues, 0, args.length - 1);
        }

        final String expr = args[0];

        if (expr.isEmpty()) {
            return;
        }

        final String simplifiedExpr;
        try {
            simplifiedExpr = simplifyExpr(expr, variableValues);
        } catch (InvalidExpressionException e) {
            System.out.println(e.getMessage());
            return;
        }

        //NOPMD - suppressed SystemPrintln
        System.out.println(simplifiedExpr);
    }

    @Contract(pure = true)
    public static @NotNull String simplifyExpr(@NotNull String expr, @NotNull String... variableValues) throws InvalidExpressionException {

        final SyntaxTree syntaxTree = parseExpr(expr);

        final Map<String, String> variableToValue = parseInputVariablesValues(List.of(variableValues));

        final SyntaxTree subbedTree = makeSubstitutions(syntaxTree, variableToValue);

        final SyntaxTree simplifiedTree = simplify(subbedTree);

        return simplifiedTree.toString();
    }

    @Contract(pure = true)
    private static @NotNull SyntaxTree parseExpr(@NotNull String expr) throws InvalidExpressionException {

        final ExpressionLexer lexer = new ExpressionLexer(expr);

        lexer.lexExpression();

        final List<LexNode> lexNodes = lexer.getLexNodes();

        final List<SyntaxTree> subTrees = makeSubTrees(lexNodes);

        return buildTree(subTrees);
    }

    @Contract(pure = true)
    private static @NotNull Map<String, String> parseInputVariablesValues(@NotNull List<String> variableValues) {

        if (variableValues.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, String> variableToValue = new HashMap<>(variableValues.size());

        final String delimiter = "=";

        for (final String input : variableValues) {
            final String[] split = input.split(delimiter);
            variableToValue.put(split[0], split[1]);
        }

        return variableToValue;
    }

    @Contract(pure = true, value = "null, _ -> null; !null, _ -> new")
    private static @Nullable SyntaxTree makeSubstitutions(@Nullable SyntaxTree tree, @NotNull Map<String, String> variableToValue) {

        if (tree == null) {
            return null;
        }

        final SyntaxTree subbedLeft = makeSubstitutions(tree.left, variableToValue);
        final SyntaxTree subbedRight = makeSubstitutions(tree.right, variableToValue);

        final String token = tree.getToken();

        final LexNode node;

        if (variableToValue.containsKey(token)) {

            final String newToken = variableToValue.get(token);

            node = new LexNode(newToken, TokenType.NUMBER);
        } else {

            node = tree.node;
        }

        return new SyntaxTree(node, subbedLeft, subbedRight);
    }

    @Contract(pure = true, value = "null -> null; !null -> new")
    private static @Nullable SyntaxTree simplify(@Nullable SyntaxTree tree) {

        if (tree == null) {
            return null;
        }

        return foldConstants(tree);

        //TODO: implement other simplifications
        //like double negatives
        //and 0*x = 0
        //and 1*x = x
        //and distributive property
    }

    @Contract(pure = true)
    private static @NotNull SyntaxTree foldConstants(@NotNull SyntaxTree tree) {

        if (tree.isLeaf()) {
            return tree;
        }

        assert tree.left != null;
        final SyntaxTree left = simplify(tree.left);

        assert tree.right != null;
        final SyntaxTree right = simplify(tree.right);

        if (left.getType() == TokenType.NUMBER && right.getType() == TokenType.NUMBER) {

            final String newToken = evalTree(tree.getToken(), left.getToken(), right.getToken());

            return new SyntaxTree(new LexNode(newToken, TokenType.NUMBER));
        }

        return new SyntaxTree(tree.node, left, right);
    }

    @Contract(pure = true)
    private static @NotNull String evalTree(@NotNull String operator, @NotNull String leftToken, @NotNull String rightToken) {
        BigDecimal left = new BigDecimal(leftToken);
        BigDecimal right = new BigDecimal(rightToken);

        final BigDecimal res = Operator.getFunction(operator).apply(left, right);

        if (res.scale() <= 0) {
            return String.valueOf(res.longValueExact());
        } else {
            return String.valueOf(res);
        }
    }

    @Contract(pure = true, value = "_ -> new")
    private static @NotNull List<SyntaxTree> makeSubTrees(@NotNull List<LexNode> lexNodes) throws InvalidExpressionException {
        final List<SyntaxTree> subTrees = new ArrayList<>();
        for (final LexNode lexNode : lexNodes) {
            if (lexNode.type == TokenType.SUBEXPR) {
                final String subExpr = lexNode.token;
                // Remove parentheses
                final String subExprNoParens = subExpr.substring(1, subExpr.length() - 1);
                subTrees.add(parseExpr(subExprNoParens));
            } else {
                subTrees.add(new SyntaxTree(lexNode));
            }
        }

        return subTrees;
    }

    @Contract(pure = true)
    private static SyntaxTree buildTree(@NotNull List<SyntaxTree> subTrees) throws InvalidExpressionException {
        if (subTrees.size() == 1) {
            return subTrees.get(0);
        }

        List<SyntaxTree> newSubTrees = new ArrayList<>(subTrees);
        for (final Set<String> operators : Operator.tokensGroupedByPrecedence()) {
            // Building the complete tree from subtrees must respect operator precedence.
            newSubTrees = buildTree(newSubTrees, operators);
        }

        return newSubTrees.get(0);
    }

    @Contract(pure = true, value = "_, _ -> new")
    private static @NotNull List<SyntaxTree> buildTree(@NotNull List<SyntaxTree> trees, @NotNull Set<String> operators) throws InvalidExpressionException {

        final Deque<SyntaxTree> subTreesStack = new ArrayDeque<>();

        SyntaxTree operatorTree = null;

        for (final SyntaxTree tree : trees) {

            final boolean isOperator = tree.getType() == TokenType.OPERATOR;

            final boolean isCorrectOperator = operators.contains(tree.getToken());

            if (isOperator && isCorrectOperator && tree.isLeaf()) {

                operatorTree = tree;
            } else if (operatorTree != null) {

                final SyntaxTree leftTree = subTreesStack.removeLast();

                final SyntaxTree newTree = new SyntaxTree(operatorTree.node, leftTree, tree);

                subTreesStack.addLast(newTree);

                operatorTree = null;
            } else {

                subTreesStack.addLast(tree);
            }
        }

        if (operatorTree != null) {

            throw new InvalidExpressionException("Invalid expression");
        }

        return new ArrayList<>(subTreesStack);
    }
}
