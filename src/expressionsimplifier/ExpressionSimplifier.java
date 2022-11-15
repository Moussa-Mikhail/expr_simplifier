package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BinaryOperator;

class ExpressionSimplifier {

    // TODO: move this to own class
    static final Map<String, BinaryOperator<Double>> OPERATOR_TO_FUNCTION = new HashMap<>();

    static {
        OPERATOR_TO_FUNCTION.put("^", Math::pow);

        OPERATOR_TO_FUNCTION.put("*", (a, b) -> a * b);

        OPERATOR_TO_FUNCTION.put("/", (a, b) -> a / b);

        OPERATOR_TO_FUNCTION.put("+", Double::sum);

        OPERATOR_TO_FUNCTION.put("-", (a, b) -> a - b);
    }

    public static void main(String @NotNull ... args) throws InvalidExpressionException {

        String[] variables = new String[args.length - 1];

        if (args.length > 1) System.arraycopy(args, 1, variables, 0, args.length - 1);

        final Map<String, String> variableToValue = parseInputVariables(variables);

        var expr = args[0];

        var simplifiedExpr = simplifyExpr(expr, variableToValue);

        System.out.println(simplifiedExpr);
    }

    private static @NotNull Map<String, String> parseInputVariables(String @NotNull [] variables) {

        Map<String, String> variableToValue = new HashMap<>();

        for (String input : variables) {

            String[] split = input.split("=");

            variableToValue.put(split[0], split[1]);
        }

        return variableToValue;
    }

    public static String simplifyExpr(String expr, Map<String, String> variableToValue) throws InvalidExpressionException {
        var syntaxTree = parse(expr);

        var simplifiedTree = simplify(syntaxTree, variableToValue);

        return simplifiedTree.toString();
    }

    private static SyntaxTree parse(String expr) throws InvalidExpressionException {

        var lexNodes = ExpressionLexer.lexExpression(expr);

        var subTrees = makeSubTrees(lexNodes);

        return buildTree(subTrees);
    }

    private static @NotNull SyntaxTree simplify(@NotNull SyntaxTree tree, Map<String, String> variableToValue) {
        var subbedTree = makeSubstitutions(tree, variableToValue);

        return simplify(subbedTree);
    }

    private static SyntaxTree makeSubstitutions(SyntaxTree tree, Map<String, String> variableToValue) {
        if (tree == null) {
            return null;
        }

        var subbedLeft = makeSubstitutions(tree.left, variableToValue);

        var subbedRight = makeSubstitutions(tree.right, variableToValue);

        String token = tree.node.token;

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

        var treeCopy = new SyntaxTree(tree);

        SyntaxTree left = simplify(treeCopy.left);

        SyntaxTree right = simplify(treeCopy.right);

        if (left.node.type == TokenType.NUMBER && right.node.type == TokenType.NUMBER) {

            var newToken = evalTree(treeCopy.node.token, left.node.token, right.node.token);

            return new SyntaxTree(new LexNode(newToken, TokenType.NUMBER));
        }

        return new SyntaxTree(treeCopy.node, left, right);
    }

    private static @NotNull String evalTree(String operator, String leftToken, String rightToken) {

        var left = Double.parseDouble(leftToken);

        var right = Double.parseDouble(rightToken);

        double res = OPERATOR_TO_FUNCTION.get(operator).apply(left, right);

        if (res == Math.floor(res)) {

            return String.valueOf((int) res);

        } else {

            return String.valueOf(res);

        }
    }

    private static @NotNull ArrayList<SyntaxTree> makeSubTrees(LexNode @NotNull ... lexNodes) throws InvalidExpressionException {

        ArrayList<SyntaxTree> subTrees = new ArrayList<>();

        for (LexNode lexNode : lexNodes) {
            if (lexNode.getType() == TokenType.SUBEXPR) {

                String subExpr = lexNode.getToken();

                subExpr = subExpr.substring(1, subExpr.length() - 1); // Remove parentheses

                subTrees.add(parse(subExpr));

            } else {

                subTrees.add(new SyntaxTree(lexNode));

            }
        }

        return subTrees;
    }

    private static SyntaxTree buildTree(ArrayList<SyntaxTree> subTrees) {

        final List<Set<String>> operatorPrecedence = new ArrayList<>();


        operatorPrecedence.add(Set.of("^"));
        operatorPrecedence.add(Set.of("*", "/"));
        operatorPrecedence.add(Set.of("+", "-"));


        for (var operators : operatorPrecedence) {
            // Building the complete tree from subtrees must respect operator precedence.

            subTrees = buildTree(subTrees, operators);

        }

        return subTrees.get(0);

    }

    @Contract("_, _ -> new")
    private static @NotNull ArrayList<SyntaxTree> buildTree(@NotNull ArrayList<SyntaxTree> trees, Set<String> operators) {
        /*
          This function is used to apply operator precedence to the trees.
          It will replace the operator and its adjacent operands with a new tree.
         */

        Deque<SyntaxTree> operandsStack = new ArrayDeque<>();

        SyntaxTree operatorTree = null;

        for (SyntaxTree tree : trees) {

            boolean isCorrectOperator = operators.contains(tree.node.token);

            if (isCorrectOperator && tree.isLeaf()) {

                operatorTree = tree;

            } else if (operatorTree != null) {

                var leftTree = operandsStack.removeLast();

                SyntaxTree newTree = new SyntaxTree(operatorTree.node, leftTree, tree);

                operandsStack.addLast(newTree);

                operatorTree = null;

            } else {

                operandsStack.addLast(tree);

            }
        }

        return new ArrayList<>(operandsStack);
    }
}
