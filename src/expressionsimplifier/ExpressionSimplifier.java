package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Moussa
 */
public final class ExpressionSimplifier {

    private ExpressionSimplifier() {
    }

    public static void main(String @NotNull ... args) throws InvalidExpressionException {

        if (args.length == 0) {
            return;
        }

        final String[] variables = new String[args.length - 1];

        if (args.length > 1) {

            System.arraycopy(args, 1, variables, 0, args.length - 1);
        }

        final var expr = args[0];

        final var cleanExpr = expr.replaceAll("\\s+", "");

        if (cleanExpr.isEmpty()) {
            return;
        }

        final Map<String, String> variableToValue = parseInputVariables(variables);

        final var simplifiedExpr = simplifyExpr(cleanExpr, variableToValue);

        //NOPMD - suppressed SystemPrintln
        System.out.println(simplifiedExpr);
    }

    private static @NotNull Map<String, String> parseInputVariables(String @NotNull ... variables) {

        final Map<String, String> variableToValue = new HashMap<>(variables.length);

        for (final var input : variables) {

            final var split = input.split("=");

            variableToValue.put(split[0], split[1]);
        }

        return variableToValue;
    }

    public static String simplifyExpr(@NotNull String expr, @NotNull Map<String, String> variableToValue) throws InvalidExpressionException {

        final var syntaxTree = parse(expr);

        final var subbedTree = makeSubstitutions(syntaxTree, variableToValue);

        final var simplifiedTree = simplify(subbedTree);

        return simplifiedTree.toString();
    }

    private static @NotNull SyntaxTree parse(@NotNull String expr) throws InvalidExpressionException {

        final var lexer = new ExpressionLexer(expr);

        lexer.lexExpression();

        final var lexNodes = lexer.getLexNodes();

        final var subTrees = makeSubTrees(lexNodes);

        return buildTree(subTrees);
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    private static @Nullable SyntaxTree makeSubstitutions(@Nullable SyntaxTree tree, @NotNull Map<String, String> variableToValue) {

        if (tree == null) {

            return null;
        }

        assert tree.left != null;
        final var subbedLeft = makeSubstitutions(tree.left, variableToValue);

        assert tree.right != null;
        final var subbedRight = makeSubstitutions(tree.right, variableToValue);

        final String token = tree.getToken();

        final LexNode node;

        if (variableToValue.containsKey(token)) {

            final var newToken = variableToValue.get(token);

            node = new LexNode(newToken, TokenType.NUMBER);

        } else {

            node = tree.node;
        }

        return new SyntaxTree(node, subbedLeft, subbedRight);
    }

    @Contract("null -> null; !null -> !null")
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


    private static @NotNull SyntaxTree foldConstants(@NotNull SyntaxTree tree) {

        if (tree.isLeaf()) {

            return tree;
        }


        final SyntaxTree left = simplify(tree.left);
        assert left != null;

        final SyntaxTree right = simplify(tree.right);
        assert right != null;

        if (left.getType() == TokenType.NUMBER && right.getType() == TokenType.NUMBER) {

            final var newToken = evalTree(tree.getToken(), left.getToken(), right.getToken());

            return new SyntaxTree(new LexNode(newToken, TokenType.NUMBER));
        }

        return new SyntaxTree(tree.node, left, right);
    }

    private static @NotNull String evalTree(@NotNull String operator, @NotNull String leftToken, @NotNull String rightToken) {

        final var left = Double.parseDouble(leftToken);

        final var right = Double.parseDouble(rightToken);

        final double res = Operator.getFunction(operator).applyAsDouble(left, right);

        if (res == Math.floor(res)) {

            return String.valueOf((int) res);

        } else {

            return String.valueOf(res);

        }
    }

    private static @NotNull List<SyntaxTree> makeSubTrees(@NotNull List<LexNode> lexNodes) throws InvalidExpressionException {

        final List<SyntaxTree> subTrees = new ArrayList<>();

        for (final var lexNode : lexNodes) {

            if (lexNode.type == TokenType.SUBEXPR) {

                final var subExpr = lexNode.token;

                // Remove parentheses
                final var subExprNoParens = subExpr.substring(1, subExpr.length() - 1);

                subTrees.add(parse(subExprNoParens));

            } else {

                subTrees.add(new SyntaxTree(lexNode));

            }
        }

        return subTrees;
    }

    private static SyntaxTree buildTree(@NotNull List<SyntaxTree> subTrees) throws InvalidExpressionException {

        if (subTrees.size() == 1) {

            return subTrees.get(0);
        }

        List<SyntaxTree> newSubTrees = new ArrayList<>(subTrees);


        for (final var operators : Operator.tokensGroupedByPrecedence()) {
            // Building the complete tree from subtrees must respect operator precedence.

            newSubTrees = buildTree(newSubTrees, operators);

        }

        return newSubTrees.get(0);

    }

    @Contract("_, _ -> new")
    private static @NotNull List<SyntaxTree> buildTree(@NotNull List<SyntaxTree> trees, @NotNull Set<String> operators) throws InvalidExpressionException {

        final Deque<SyntaxTree> subTreesStack = new ArrayDeque<>();

        SyntaxTree operatorTree = null;

        for (final SyntaxTree tree : trees) {

            final boolean isOperator = tree.getType() == TokenType.OPERATOR;

            final boolean isCorrectOperator = operators.contains(tree.getToken());

            if (isOperator && isCorrectOperator && tree.isLeaf()) {

                operatorTree = tree;

            } else if (operatorTree != null) {

                final var leftTree = subTreesStack.removeLast();

                final var newTree = new SyntaxTree(operatorTree.node, leftTree, tree);

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
