package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class ExpressionSimplifier {

    public static void main(String @NotNull ... args) throws InvalidExpressionException {

        if (args.length == 0) {
            return;
        }

        System.out.println(Arrays.toString(args));

        String[] variables = new String[args.length - 1];

        if (args.length > 1) System.arraycopy(args, 1, variables, 0, args.length - 1);

        final Map<String, String> variableToValue = parseInputVariables(variables);

        var expr = args[0];

        if (expr.isEmpty()) {
            return;
        }

        var simplifiedExpr = simplifyExpr(expr, variableToValue);

        System.out.println(simplifiedExpr);
    }

    private static @NotNull Map<String, String> parseInputVariables(String @NotNull ... variables) {

        Map<String, String> variableToValue = new HashMap<>();

        for (String input : variables) {

            String[] split = input.split("=");

            variableToValue.put(split[0], split[1]);
        }

        return variableToValue;
    }

    public static String simplifyExpr(@NotNull String expr, @NotNull Map<String, String> variableToValue) throws InvalidExpressionException {
        var syntaxTree = parse(expr);

        var simplifiedTree = simplify(syntaxTree, variableToValue);

        return simplifiedTree.toString();
    }

    private static SyntaxTree parse(@NotNull String expr) throws InvalidExpressionException {

        var lexNodes = ExpressionLexer.lexExpression(expr);

        var subTrees = makeSubTrees(lexNodes);

        return buildTree(subTrees);
    }

    private static @NotNull SyntaxTree simplify(@NotNull SyntaxTree tree, @NotNull Map<String, String> variableToValue) {

        var subbedTree = makeSubstitutions(tree, variableToValue);

        return simplify(subbedTree);
    }

    private static SyntaxTree makeSubstitutions(@Nullable SyntaxTree tree, @NotNull Map<String, String> variableToValue) {

        if (tree == null) {
            return null;
        }

        assert tree.left != null;
        var subbedLeft = makeSubstitutions(tree.left, variableToValue);

        assert tree.right != null;
        var subbedRight = makeSubstitutions(tree.right, variableToValue);

        String token = tree.getToken();

        LexNode node;

        if (variableToValue.containsKey(token)) {

            var newToken = variableToValue.get(token);

            node = new LexNode(newToken, TokenType.NUMBER);
        } else {
            node = tree.node;
        }

        return new SyntaxTree(node, subbedLeft, subbedRight);
    }

    private static @NotNull SyntaxTree simplify(@NotNull SyntaxTree tree) {

        return foldConstants(tree);

        //TODO: implement other simplifications
        //like double negatives
        //and 0*x = 0
        //and 1*x = x
        //and distributive property
    }


    @NotNull
    private static SyntaxTree foldConstants(@NotNull SyntaxTree tree) {

        if (tree.isLeaf()) {
            return tree;
        }

        assert tree.left != null;
        SyntaxTree left = simplify(tree.left);

        assert tree.right != null;
        SyntaxTree right = simplify(tree.right);

        if (left.getType() == TokenType.NUMBER && right.getType() == TokenType.NUMBER) {

            var newToken = evalTree(tree.getToken(), left.getToken(), right.getToken());

            return new SyntaxTree(new LexNode(newToken, TokenType.NUMBER));
        }

        return new SyntaxTree(tree.node, left, right);
    }

    private static @NotNull String evalTree(@NotNull String operator, @NotNull String leftToken, @NotNull String rightToken) {

        var left = Double.parseDouble(leftToken);

        var right = Double.parseDouble(rightToken);

        double res = Operator.getFunction(operator).applyAsDouble(left, right);

        if (res == Math.floor(res)) {

            return String.valueOf((int) res);

        } else {

            return String.valueOf(res);

        }
    }

    private static @NotNull ArrayList<SyntaxTree> makeSubTrees(@NotNull List<LexNode> lexNodes) throws InvalidExpressionException {

        ArrayList<SyntaxTree> subTrees = new ArrayList<>();

        for (var lexNode : lexNodes) {

            if (lexNode.type == TokenType.SUBEXPR) {

                var subExpr = lexNode.token;

                var subExprNoParens = subExpr.substring(1, subExpr.length() - 1); // Remove parentheses

                subTrees.add(parse(subExprNoParens));

            } else {

                subTrees.add(new SyntaxTree(lexNode));

            }
        }

        return subTrees;
    }

    private static SyntaxTree buildTree(@NotNull ArrayList<SyntaxTree> subTrees) throws InvalidExpressionException {

        if (subTrees.size() == 1) {
            return subTrees.get(0);
        }

        var newSubTrees = new ArrayList<>(subTrees);


        for (Set<String> operators : Operator.tokensGroupedByPrecedenceDecreasingOrder()) {
            // Building the complete tree from subtrees must respect operator precedence.

            newSubTrees = buildTree(newSubTrees, operators);

        }

        return newSubTrees.get(0);

    }

    @Contract("_, _ -> new")
    private static @NotNull ArrayList<SyntaxTree> buildTree(@NotNull ArrayList<SyntaxTree> trees, @NotNull Set<String> operators) throws InvalidExpressionException {
        /*
          This function is used to apply operator precedence to the trees.
          It will replace the operator and its adjacent operands with a new tree.
         */

        Deque<SyntaxTree> subTreesStack = new ArrayDeque<>();

        SyntaxTree operatorTree = null;

        for (SyntaxTree tree : trees) {

            boolean isOperator = tree.getType() == TokenType.OPERATOR;

            boolean isCorrectOperator = operators.contains(tree.getToken());

            if (isOperator && isCorrectOperator && tree.isLeaf()) {

                operatorTree = tree;

            } else if (operatorTree != null) {

                var leftTree = subTreesStack.removeLast();

                SyntaxTree newTree = new SyntaxTree(operatorTree.node, leftTree, tree);

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
