package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.function.BinaryOperator;

class ExpressionSimplifier {

    public static void main(String... args) throws InvalidExpressionException {
        var expr = "2*x-(-3)*4";

        var simplifiedExpr = simplifyExpr(expr);

        System.out.println(simplifiedExpr);
    }

    static final LinkedHashMap<String, BinaryOperator<Double>> OPERATOR_TO_FUNCTION = createOperationsMap();

    private ExpressionSimplifier() {
        throw new IllegalStateException("Utility class");
    }

    private static @NotNull LinkedHashMap<String, BinaryOperator<Double>> createOperationsMap() {
        LinkedHashMap<String, BinaryOperator<Double>> operations = new LinkedHashMap<>();
        operations.put("^", Math::pow);
        operations.put("*", (a, b) -> a * b);
        operations.put("/", (a, b) -> a / b);
        operations.put("+", Double::sum);
        operations.put("-", (a, b) -> a - b);
        return operations;
    }

    public static String simplifyExpr(String expr) throws InvalidExpressionException {
        var syntaxTree = parse(expr);

        var simplifiedTree = simplify(syntaxTree);

        return simplifiedTree.toString();
    }

    private static SyntaxTree parse(String expr) throws InvalidExpressionException {

        var lexNodes = ExpressionLexer.lexExpression(expr);

        var subTrees = makeSubTrees(lexNodes);

        return buildTree(subTrees);
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

        for (var operator : OPERATOR_TO_FUNCTION.keySet()) {
            // Building the complete tree from subtrees must respect operator precedence.

            subTrees = buildTree(subTrees, operator);

        }

        return subTrees.get(0);

    }

    @Contract("_, _ -> new")
    private static @NotNull ArrayList<SyntaxTree> buildTree(@NotNull ArrayList<SyntaxTree> trees, String operator) {
        /*
          This function is used to applied operator precedence to the trees.
          It will replace the operator and its adjacent operands with a new tree.
         */

        Deque<SyntaxTree> treeStack = new ArrayDeque<>();

        int idx = 0;
        // TODO: change to for loop
        while (idx < trees.size()) {

            SyntaxTree tree = trees.get(idx);

            boolean isOperator = tree.node.type == TokenType.OPERATOR;

            boolean isCorrectOperator = tree.node.token.equals(operator);

            if (isOperator && isCorrectOperator && tree.isLeaf()) {

                SyntaxTree leftTree = trees.get(idx - 1);

                SyntaxTree rightTree = trees.get(idx + 1);

                var newTree = new SyntaxTree(tree.node, leftTree, rightTree);

                treeStack.pop();

                treeStack.push(newTree);

                idx++;
            } else {
                treeStack.push(tree);
            }

            idx++;
        }

        return new ArrayList<>(treeStack);
    }
}
