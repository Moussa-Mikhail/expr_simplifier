package expressionsimplifier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

import static expressionsimplifier.Constants.*;

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

        String[] variableValues = new String[args.length - 1];
        if (args.length > 1) {
            System.arraycopy(args, 1, variableValues, 0, args.length - 1);
        }

        String expr = args[0];
        if (expr.isEmpty()) {
            return;
        }

        String simplifiedExpr;
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
    public static @NotNull String simplifyExpr(@NotNull String expr, @NotNull List<String> variableValues) throws InvalidExpressionException {
        SyntaxTree syntaxTree = parseExpr(expr);
        Map<String, String> variableToValue = parseInputVariablesValues(variableValues);

        SyntaxTree subbedTree = makeSubstitutions(syntaxTree, variableToValue);
        SyntaxTree simplifiedTree = simplify(subbedTree);

        return simplifiedTree.toString();
    }

    @Contract(pure = true)
    public static @NotNull String simplifyExpr(@NotNull String expr, String @NotNull ... variableValues) throws InvalidExpressionException {
        return simplifyExpr(expr, Arrays.asList(variableValues));
    }

    @Contract(pure = true)
    private static @NotNull SyntaxTree parseExpr(@NotNull String expr) throws InvalidExpressionException {
        ExpressionLexer lexer = new ExpressionLexer(expr);
        lexer.lexExpression();
        List<LexNode> lexNodes = lexer.getLexNodes();

        List<SyntaxTree> subTrees = makeSubTrees(lexNodes);
        return buildTree(subTrees);
    }

    @Contract(pure = true)
    private static @NotNull Map<String, String> parseInputVariablesValues(@NotNull List<String> variableValues) {
        if (variableValues.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> variableToValue = new HashMap<>(variableValues.size());
        String delimiter = "=";
        for (String input : variableValues) {
            String[] split = input.split(delimiter);
            variableToValue.put(split[0], split[1]);
        }

        return variableToValue;
    }

    @Contract(pure = true, value = "null, _ -> null; !null, _ -> new")
    private static @Nullable SyntaxTree makeSubstitutions(@Nullable SyntaxTree tree, @NotNull Map<String, String> variableToValue) {
        if (tree == null) {
            return null;
        }

        SyntaxTree subbedLeft = makeSubstitutions(tree.left, variableToValue);
        SyntaxTree subbedRight = makeSubstitutions(tree.right, variableToValue);
        String token = tree.getToken();
        LexNode node;
        if (variableToValue.containsKey(token)) {
            String newToken = variableToValue.get(token);
            node = new LexNode(newToken, TokenType.NUMBER);
        } else {
            node = tree.node;
        }

        return new SyntaxTree(node, subbedLeft, subbedRight);
    }

    @Contract(pure = true, value = "_ -> new")
    private static @NotNull SyntaxTree simplify(@NotNull SyntaxTree tree) {
        SyntaxTree simplifiedTree = tree;
        List<Simplifier> simplifiers = List.of(ExpressionSimplifier::foldConstants, ExpressionSimplifier::standardize);
        for (var simplifier : simplifiers) {
            if (simplifiedTree.isLeaf()) {
                return simplifiedTree;
            }
            assert simplifiedTree.left != null;
            assert simplifiedTree.right != null;
            LexNode node = simplifiedTree.node;
            SyntaxTree left = simplify(simplifiedTree.left);
            SyntaxTree right = simplify(simplifiedTree.right);
            simplifiedTree = simplifier.simplify(node.token, left, right);
        }

        return simplifiedTree;

        //TODO: implement other simplifications
        //double negatives
        //and 0*x = 0
        //and 1*x = x
        //and distributive property
    }

    @Contract(pure = true)
    private static @NotNull SyntaxTree foldConstants(@NotNull String operator, @NotNull SyntaxTree left, @NotNull SyntaxTree right) {
        if (left.getTokenType() == TokenType.NUMBER && right.getTokenType() == TokenType.NUMBER) {
            BigDecimal leftNum = new BigDecimal(left.getToken());
            BigDecimal rightNum = new BigDecimal(right.getToken());

            BigDecimal result = Operator.getFunction(operator).apply(leftNum, rightNum);
            String newToken = result.toPlainString();
            return new SyntaxTree(new LexNode(newToken, TokenType.NUMBER));
        }

        LexNode node = new LexNode(operator, TokenType.OPERATOR);
        return new SyntaxTree(node, left, right);
    }

    @Contract(pure = true)
    private static @NotNull SyntaxTree standardize(@NotNull String operator, @NotNull SyntaxTree left, @NotNull SyntaxTree right) {
        boolean isLeftNumber = left.getTokenType() == TokenType.NUMBER;
        boolean isRightNumber = right.getTokenType() == TokenType.NUMBER;
        LexNode node = new LexNode(operator, TokenType.OPERATOR);
        if (operator.equals(ADD) && isLeftNumber && !isRightNumber) {
            return new SyntaxTree(node, right, left);
        }

        if (operator.equals(MUL) && !isLeftNumber && isRightNumber) {
            return new SyntaxTree(node, right, left);
        }

        boolean isLeftVariable = left.expressionTypeEquals(ExpressionType.VARIABLE);
        boolean isLeftPow = left.expressionTypeEquals(ExpressionType.POW) || isLeftVariable;

        boolean isRightVariable = right.expressionTypeEquals(ExpressionType.VARIABLE);
        boolean isRightPow = right.expressionTypeEquals(ExpressionType.POW) || isRightVariable;
        if (operator.equals(ADD) && isLeftPow && isRightPow) {
            return standardizePowers(node, left, right);
        }

        return new SyntaxTree(node, left, right);
    }

    private static @NotNull SyntaxTree standardizePowers(LexNode node, @NotNull SyntaxTree left, @NotNull SyntaxTree right) {
        SyntaxTree leftBase = left.expressionTypeEquals(ExpressionType.POW) ? left.left : left;
        SyntaxTree rightBase = right.expressionTypeEquals(ExpressionType.POW) ? right.left : right;

        assert leftBase != null;
        if (!leftBase.equals(rightBase)) {
            return new SyntaxTree(node, left, right);
        }

        BigDecimal leftNum = left.right != null ? new BigDecimal(left.right.getToken()) : BigDecimal.ONE;
        BigDecimal rightNum = right.right != null ? new BigDecimal(right.right.getToken()) : BigDecimal.ONE;

        if (leftNum.compareTo(rightNum) < 0) {
            return new SyntaxTree(node, right, left);
        }

        return new SyntaxTree(node, left, right);
    }

    @Contract(pure = true, value = "_ -> new")
    private static @NotNull List<SyntaxTree> makeSubTrees(@NotNull List<LexNode> lexNodes) throws InvalidExpressionException {
        List<SyntaxTree> subTrees = new ArrayList<>();
        for (var lexNode : lexNodes) {
            if (lexNode.type == TokenType.SUBEXPR) {
                String subExpr = lexNode.token;
                // Remove parentheses
                subExpr = subExpr.substring(1, subExpr.length() - 1);
                subTrees.add(parseExpr(subExpr));
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
        for (Set<String> operators : Operator.tokensGroupedByPrecedence()) {
            // Building the complete tree from subtrees must respect operator precedence.
            newSubTrees = buildTree(newSubTrees, operators);
        }

        return newSubTrees.get(0);
    }

    @Contract(pure = true, value = "_, _ -> new")
    private static @NotNull List<SyntaxTree> buildTree(@NotNull List<SyntaxTree> trees, @NotNull Set<String> operators) throws InvalidExpressionException {
        Deque<SyntaxTree> subTreesStack = new ArrayDeque<>();
        SyntaxTree operatorTree = null;
        for (SyntaxTree tree : trees) {
            boolean isOperator = tree.getTokenType() == TokenType.OPERATOR;
            boolean isCorrectOperator = operators.contains(tree.getToken());

            if (isOperator && isCorrectOperator && tree.isLeaf()) {
                operatorTree = tree;
            } else if (operatorTree != null) {
                SyntaxTree leftTree = subTreesStack.removeLast();
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
