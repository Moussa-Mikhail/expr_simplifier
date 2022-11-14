package exprsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.function.BinaryOperator;

enum TokenType {
    OPERATOR, NUMBER, VARIABLE, SUBEXPR
}

class LexNode {
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

    public String getToken() {
        return token;
    }

    public TokenType getType() {
        return type;
    }
}

class SyntaxTree {
    public final LexNode node;
    public final SyntaxTree left;
    public final SyntaxTree right;

    public SyntaxTree(LexNode node) {
        this.node = node;
        this.left = null;
        this.right = null;
    }

    public SyntaxTree(LexNode node, SyntaxTree left, SyntaxTree right) {
        this.node = node;
        this.left = left;
        this.right = right;
    }

    @Contract(pure = true)
    public SyntaxTree(@NotNull SyntaxTree tree) {
        this.node = tree.node;
        this.left = tree.left;
        this.right = tree.right;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return node.toString();
        } else {
            return String.format("(%s %s %s)", left, node, right);
        }
    }
}

class InvalidExpressionException extends Exception {
    public InvalidExpressionException(String message) {
        super(message);
    }
}

public class ExprSimplifier {

    private static LinkedHashMap<String, BinaryOperator<Double>> createOperationsMap() {
        LinkedHashMap<String, BinaryOperator<Double>> operations = new LinkedHashMap<>();
        operations.put("^", Math::pow);
        operations.put("*", (a, b) -> a * b);
        operations.put("/", (a, b) -> a / b);
        operations.put("+", Double::sum);
        operations.put("-", (a, b) -> a - b);
        return operations;
    }

    static final LinkedHashMap<String, BinaryOperator<Double>> OPERATOR_TO_FUNCTION = createOperationsMap();

    public static void main(String[] args) throws InvalidExpressionException {
        var expr = "2*x-(-3)*4";

        var exprTree = parse(expr);

        System.out.printf("Expression: %s%n", exprTree);

//        var subbedTree = substitute(exprTree, Map.of("x", 2.0));

//        System.out.println(subbedTree);

        var simplifiedTree = simplify(exprTree);

        System.out.println(simplifiedTree);
    }

    private static SyntaxTree parse(String expr) throws InvalidExpressionException {

        var lexNodes = lexExpression(expr);

        return parse(lexNodes);
    }

    private static SyntaxTree parse(LexNode[] lexNodes) throws InvalidExpressionException {
        var subTrees = makeSubTrees(lexNodes);

        buildTree(subTrees);

        return subTrees.get(0);
    }

    private static LexNode @NotNull [] lexExpression(String expr) throws InvalidExpressionException {
        expr = expr.replaceAll("\\s+", "");

        ArrayList<LexNode> lexNodes = new ArrayList<>();

        String tokenExpr = "";

        TokenType tokenType = null;

        int idx = 0;

        while (idx < expr.length()) {

            char chr = expr.charAt(idx);

            if (chr == '(') {

                int endIdx = findClosingParen(expr, idx);

                if (endIdx == -1) {
                    throw new InvalidExpressionException("Unmatched opening parenthesis");
                }

                tokenExpr = expr.substring(idx, endIdx + 1);

                tokenType = TokenType.SUBEXPR;

                idx = endIdx + 1;

            } else if (chr == ')') {

                throw new InvalidExpressionException("Unmatched closing parenthesis");

            } else if (chr == '-' && (tokenType == TokenType.OPERATOR || tokenType == null)) {

                tokenExpr = String.valueOf(chr);

                tokenType = TokenType.NUMBER;

                idx++;

                continue;

            } else if (chr == '+' || chr == '*' || chr == '/' || chr == '-') {

                tokenExpr = String.valueOf(chr);

                tokenType = TokenType.OPERATOR;

                idx++;

            } else if (Character.isDigit(chr)) {

                int endIdx = findEndOfNumber(expr, idx);

                String numberStr = expr.substring(idx, endIdx + 1);

                tokenExpr = tokenExpr.concat(numberStr);

                tokenType = TokenType.NUMBER;

                idx = endIdx + 1;

            } else if (Character.isAlphabetic(chr)) {

                int endIdx = findEndOfVariable(expr, idx);

                String variableStr = expr.substring(idx, endIdx + 1);

                tokenExpr = tokenExpr.concat(variableStr);

                tokenType = TokenType.VARIABLE;

                idx = endIdx + 1;

            } else {
                throw new InvalidExpressionException("Invalid character: " + chr);
            }

            LexNode lexNode = new LexNode(tokenExpr, tokenType);

            lexNodes.add(lexNode);

            tokenExpr = "";

        }

        return lexNodes.toArray(new LexNode[0]);

    }

    private static int findClosingParen(@NotNull String expr, int startIdx) {
//        This function assumes that the first character at startIdx is an opening parenthesis.
//        It returns the index of the closing parenthesis. -1 if unmatched.

        int parenCount = 0;

        for (int idx = startIdx; idx < expr.length(); idx++) {

            char chr = expr.charAt(idx);

            if (chr == '(') {
                parenCount++;
            } else if (chr == ')') {
                parenCount--;
            }
            if (parenCount == 0) {
                return idx;
            }
        }
        return -1;
    }

    private static int findEndOfNumber(@NotNull String expr, int startIdx) {

        for (int idx = startIdx; idx < expr.length(); idx++) {

            char chr = expr.charAt(idx);

            if (!Character.isDigit(chr)) {
                return idx - 1;
            }
        }

        return expr.length() - 1;
    }

    private static int findEndOfVariable(@NotNull String expr, int startIdx) {

        for (int idx = startIdx; idx < expr.length(); idx++) {

            char chr = expr.charAt(idx);

            if (!Character.isAlphabetic(chr)) {
                return idx - 1;
            }
        }

        return expr.length() - 1;
    }

    private static @NotNull SyntaxTree simplify(@NotNull SyntaxTree tree) {
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

    private static String evalTree(String operator, String leftToken, String rightToken) {

        var left = Double.parseDouble(leftToken);

        var right = Double.parseDouble(rightToken);

        double res = OPERATOR_TO_FUNCTION.get(operator).apply(left, right);

        if (res == Math.floor(res)) {

            return String.valueOf((int) res);

        } else {

            return String.valueOf(res);

        }
    }

    private static ArrayList<SyntaxTree> makeSubTrees(LexNode[] lexNodes) throws InvalidExpressionException {
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

    private static ArrayList<SyntaxTree> buildTree(ArrayList<SyntaxTree> subTrees) {

        for (var operator : OPERATOR_TO_FUNCTION.keySet()) {
        // Building the complete tree from subtrees must respect operator precedence.

            subTrees = evalTree(subTrees, operator);

        }

        return subTrees;

    }

    private static ArrayList<SyntaxTree> evalTree(ArrayList<SyntaxTree> trees, String operator) {
        /*
          This function is used to applied operator precedence to the trees.
          It will replace the operator and its adjacent operands with a new tree.
         */

        Stack<SyntaxTree> treeStack = new Stack<>();

        int idx = 0;

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
            }

            else {
                treeStack.push(tree);
            }

            idx++;
        }

        var newTrees = new ArrayList<SyntaxTree>(treeStack);

        return newTrees;
    }

}
