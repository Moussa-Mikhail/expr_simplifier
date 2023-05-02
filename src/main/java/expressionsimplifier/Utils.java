package expressionsimplifier;

import org.jetbrains.annotations.NotNull;

import static expressionsimplifier.Constants.LEFT_PAREN;
import static expressionsimplifier.Constants.RIGHT_PAREN;

public final class Utils {
    private Utils() {
    }

    public static @NotNull String removeParens(String expr) {
        String firstChar = String.valueOf(expr.charAt(0));
        String lastChar = String.valueOf(expr.charAt(expr.length() - 1));
        if (firstChar.equals(LEFT_PAREN) && lastChar.equals(RIGHT_PAREN)) {
            return expr.substring(1, expr.length() - 1);
        }

        return expr;
    }
}
