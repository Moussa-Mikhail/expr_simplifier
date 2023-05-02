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
    private ExpressionSimplifier() {}

    @Contract(pure = true)
    public static void main(@NotNull String... args) {
        if (args.length == 0) {
            return;
        }

        @NotNull String[] variableValues = Arrays.copyOfRange(args, 1, args.length);

        String expr = args[0];
        if (expr.isEmpty()) {
            return;
        }

        String simplifiedExpr;
        try {
            simplifiedExpr = simplifyExpr(expr, variableValues);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        //NOPMD - suppressed SystemPrintln
        System.out.println(simplifiedExpr);
    }

    @Contract(pure = true, value = "_, _ -> new")
    public static @NotNull String simplifyExpr(String expr, List<@NotNull String> variableValues) throws InvalidExpressionException {
        SyntaxTree syntaxTree = parseExpr(expr);
        Map<@NotNull String, @NotNull String> variableToValue = parseInputVariablesValues(variableValues);

        SyntaxTree subbedTree = makeSubstitutions(syntaxTree, variableToValue);
        SyntaxTree simplifiedTree = simplify(subbedTree);

        return simplifiedTree.toString();
    }

    @Contract(pure = true, value = "_, _ -> new")
    public static @NotNull String simplifyExpr(String expr, @NotNull String... variableValues) throws InvalidExpressionException {
        return simplifyExpr(expr, Arrays.asList(variableValues));
    }

    @Contract(pure = true, value = "_ -> new")
    private static @NotNull SyntaxTree parseExpr(String expr) throws InvalidExpressionException {
        var lexer = new ExpressionLexer(expr);
        lexer.lexExpression();
        List<@NotNull LexNode> lexNodes = lexer.getLexNodes();
        return buildTree(lexNodes);
    }

    @Contract(pure = true)
    private static @NotNull Map<@NotNull String, @NotNull String> parseInputVariablesValues(List<@NotNull String> variableValues) {
        if (variableValues.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> variableToValue = new HashMap<>(variableValues.size());
        String delimiter = "=";
        for (var input : variableValues) {
            String[] split = input.split(delimiter);
            variableToValue.put(split[0], split[1]);
        }

        return variableToValue;
    }

    @Contract(pure = true, value = "null, _ -> null; !null, _ -> new")
    private static @Nullable SyntaxTree makeSubstitutions(@Nullable SyntaxTree tree, Map<@NotNull String, @NotNull String> variableToValue) {
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

    private static boolean equalsZero(String token) {
        return BigDecimal.ZERO.compareTo(new BigDecimal(token)) == 0;
    }

    private static boolean isNegative(String token) {
        return (new BigDecimal(token)).compareTo(BigDecimal.ZERO) < 0;
    }

    @Contract(pure = true)
    private static void checkInvalidExpr(String operator, SyntaxTree left, SyntaxTree right) throws InvalidExpressionException {
        String rightToken = right.getToken();
        if (operator.equals(DIV) && right.isNumber() && equalsZero(rightToken)) {
            throw new InvalidExpressionException("Division by zero");
        }

        String leftToken = left.getToken();
        if (operator.equals(POW) && left.isNumber() && equalsZero(leftToken) && (isNegative(rightToken))) {
            throw new InvalidExpressionException("Division by zero");
        }

        if (operator.equals(POW) && left.isNumber() && isNegative(leftToken)) {
            var power = new BigDecimal(rightToken);
            if (power.stripTrailingZeros().scale() > 0) {
                throw new InvalidExpressionException("Negative number raised to non-integer exponent.");
            }
        }
    }

    @Contract(pure = true)
    private static @NotNull SyntaxTree simplify(SyntaxTree tree) throws InvalidExpressionException {
        SyntaxTree simplifiedTree = tree;
        List<@NotNull Simplifier> simplifiers = List.of(
                ExpressionSimplifier::foldConstants,
                ExpressionSimplifier::standardize,
                ExpressionSimplifier::applyAlgebraicIdentities
        );
        for (var simplifier : simplifiers) {
            if (simplifiedTree.isLeaf()) {
                return simplifiedTree;
            }

            assert simplifiedTree.left != null;
            assert simplifiedTree.right != null;
            LexNode node = simplifiedTree.node;
            SyntaxTree left = simplify(simplifiedTree.left);
            SyntaxTree right = simplify(simplifiedTree.right);
            checkInvalidExpr(node.token, left, right);
            simplifiedTree = simplifier.simplify(node.token, left, right);
        }

        return simplifiedTree;

        // TODO: implement other simplifications
        // distributive property
        // associativity
    }

    @Contract(pure = true, value = "_, _, _ -> new")
    private static @NotNull SyntaxTree foldConstants(String operator, SyntaxTree left, SyntaxTree right) {
        if (left.isNumber() && right.isNumber()) {
            var leftNum = new BigDecimal(left.getToken());
            var rightNum = new BigDecimal(right.getToken());

            BigDecimal result = Operator.getFunction(operator).apply(leftNum, rightNum);
            String newToken = result.toPlainString();
            var resultNode = new LexNode(newToken, TokenType.NUMBER);
            return new SyntaxTree(resultNode);
        }

        var node = new LexNode(operator, TokenType.OPERATOR);
        return new SyntaxTree(node, left, right);
    }

    @Contract(pure = true, value = "_, _, _ -> new")
    private static @NotNull SyntaxTree standardize(String operator, SyntaxTree left, SyntaxTree right) {
        boolean isLeftNumber = left.isNumber();
        boolean isRightNumber = right.isNumber();
        var node = new LexNode(operator, TokenType.OPERATOR);
        if (operator.equals(ADD) && isLeftNumber && !isRightNumber) {
            return new SyntaxTree(node, right, left);
        }

        if (operator.equals(MUL) && !isLeftNumber && isRightNumber) {
            return new SyntaxTree(node, right, left);
        }

        boolean isLeftVariable = left.expressionTypeEquals(ExpressionType.VARIABLE);
        boolean isLeftPow = left.expressionTypeEquals(ExpressionType.POLY) || isLeftVariable;

        boolean isRightVariable = right.expressionTypeEquals(ExpressionType.VARIABLE);
        boolean isRightPow = right.expressionTypeEquals(ExpressionType.POLY) || isRightVariable;
        if (operator.equals(ADD) && isLeftPow && isRightPow) {
            return standardizePowers(node, left, right);
        }

        return new SyntaxTree(node, left, right);
    }

    @Contract(pure = true, value = "_, _, _ -> new")
    private static @NotNull SyntaxTree standardizePowers(LexNode node, SyntaxTree left, SyntaxTree right) {
        SyntaxTree leftBase = left.expressionTypeEquals(ExpressionType.POLY) ? left.left : left;
        SyntaxTree rightBase = right.expressionTypeEquals(ExpressionType.POLY) ? right.left : right;

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

    @SuppressWarnings("java:S3776")
    @Contract(pure = true)
    private static @NotNull SyntaxTree applyAlgebraicIdentities(String operator, SyntaxTree left, SyntaxTree right) {
        String rightToken = right.getToken();
        if (operator.equals(ADD) && "0".equals(rightToken)) {
            return left;
        }

        if (operator.equals(SUB) && "0".equals(rightToken)) {
            return left;
        }

        if (operator.equals(SUB) && left.equals(right)) {
            return new SyntaxTree(new LexNode("0", TokenType.NUMBER));
        }

        String leftToken = left.getToken();
        if (operator.equals(MUL) && "0".equals(leftToken)) {
            return new SyntaxTree(new LexNode("0", TokenType.NUMBER));
        }

        if (operator.equals(MUL) && "1".equals(leftToken)) {
            return right;
        }

        if (operator.equals(DIV) && "1".equals(rightToken)) {
            return left;
        }

        if (operator.equals(DIV) && left.equals(right)) {
            return new SyntaxTree(new LexNode("1", TokenType.NUMBER));
        }

        if (operator.equals(POW) && "1".equals(rightToken)) {
            return left;
        }

        if (operator.equals(POW) && "0".equals(leftToken)) {
            var power = new BigDecimal(rightToken);
            if (power.compareTo(BigDecimal.ZERO) == 0) {
                return new SyntaxTree(new LexNode("1", TokenType.NUMBER));
            }

            return new SyntaxTree(new LexNode("0", TokenType.NUMBER));
        }

        if (operator.equals(POW) && "0".equals(rightToken)) {
            return new SyntaxTree(new LexNode("1", TokenType.NUMBER));
        }

        return new SyntaxTree(new LexNode(operator, TokenType.OPERATOR), left, right);
    }

    @Contract(pure = true, value = "_ -> new")
    private static @NotNull List<@NotNull SyntaxTree> makeSubTrees(List<@NotNull LexNode> lexNodes) throws InvalidExpressionException {
        List<SyntaxTree> subTrees = new ArrayList<>();
        for (var lexNode : lexNodes) {
            if (lexNode.type == TokenType.SUBEXPR) {
                String subExpr = lexNode.token;
                subExpr = Utils.removeParens(subExpr);
                subTrees.add(parseExpr(subExpr));
            } else {
                subTrees.add(new SyntaxTree(lexNode));
            }
        }

        return subTrees;
    }

    @Contract(pure = true)
    private static @NotNull SyntaxTree buildTree(List<@NotNull LexNode> lexNodes) throws InvalidExpressionException {
        List<@NotNull SyntaxTree> subTrees = makeSubTrees(lexNodes);
        if (subTrees.size() == 1) {
            return subTrees.get(0);
        }

        List<@NotNull SyntaxTree> newSubTrees = new ArrayList<>(subTrees);
        for (Set<@NotNull String> operators : OPERATOR_TOKENS_GROUPED_BY_PRECEDENCE) {
            // Building the complete tree from subtrees must respect operator precedence.
            newSubTrees = buildTree(newSubTrees, operators);
        }

        if (newSubTrees.size() != 1) {
            throw new InvalidExpressionException("Invalid expression");
        }

        return newSubTrees.get(0);
    }

    @Contract(pure = true, value = "_, _ -> new")
    private static @NotNull List<@NotNull SyntaxTree> buildTree(List<@NotNull SyntaxTree> trees, Set<@NotNull String> operators) throws InvalidExpressionException {
        Deque<SyntaxTree> subTreesStack = new ArrayDeque<>();
        SyntaxTree operatorTree = null;
        for (var tree : trees) {
            boolean isOperator = tree.tokenTypeEquals(TokenType.OPERATOR);
            boolean isCorrectOperator = operators.contains(tree.getToken());

            if (isOperator && isCorrectOperator && tree.isLeaf()) {
                operatorTree = tree;
            } else if (operatorTree != null) {
                SyntaxTree leftTree = subTreesStack.removeLast();
                var newTree = new SyntaxTree(operatorTree.node, leftTree, tree);
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
