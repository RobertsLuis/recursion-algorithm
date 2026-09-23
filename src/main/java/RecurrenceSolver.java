import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RecurrenceSolver {
    public static final int MAX_EXPRESSION_LENGTH = 4096;
    public static final int MAX_EVALUATED_STATES = 100000;
    public static final int MAX_TRACE_EVENTS = 200;
    private static final int MAX_EXPRESSION_NODES = 512;
    private static final int MAX_EXPRESSION_DEPTH = 48;
    private static final int MAX_RECURSIVE_DEPTH = 16;
    private static final int MAX_NUMBER_BITS = 32768;
    private static final long MAX_CACHED_BITS = 64000000;
    private static final int MAX_EXPONENT = 10000;
    private static final int TRACE_COUNT = 0;
    private static final int STATE_COUNT = 1;
    private static final int CACHED_BITS = 2;
    private static final int REUSE_COUNT = 3;
    private static final int TRACE_LENGTH = 300;

    private final List<String> nodeOperations = new ArrayList<>();
    private final List<int[]> nodeArguments = new ArrayList<>();
    private final List<BigInteger[]> nodeValues = new ArrayList<>();
    private final long baseThreshold;
    private final int recurrenceNode;
    private final int baseNode;
    private String parserText;
    private int parserPosition;

    public RecurrenceSolver(String expression, long baseThreshold, String baseExpression) {
        if (baseThreshold < 0) {
            throw new IllegalArgumentException("The base threshold must be nonnegative.");
        }
        this.baseThreshold = baseThreshold;
        Objects.requireNonNull(expression, "The recurrence expression is required.");
        Objects.requireNonNull(baseExpression, "The base expression is required.");
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException(
                    "An expression may contain at most " + MAX_EXPRESSION_LENGTH + " characters.");
        }
        recurrenceNode =
                parse(expression.replaceFirst("^\\s*T\\s*\\(\\s*n\\s*\\)\\s*=(?!=)\\s*", ""));
        baseNode = parse(baseExpression);
        if (containsRecurrence(baseNode)) {
            throw new IllegalArgumentException("The base expression cannot call T.");
        }
        parserText = null;
    }

    public BigDecimal solve(long inputSize) {
        return solve(inputSize, null);
    }

    public BigDecimal solveWithTrace(long inputSize, PrintStream output) {
        return solve(inputSize, Objects.requireNonNull(output, "Output is required."));
    }

    private BigDecimal solve(long inputSize, PrintStream output) {
        if (inputSize < 0) {
            throw new IllegalArgumentException("The input size must be nonnegative.");
        }
        Map<Long, BigInteger[]> cachedResults = new HashMap<>();
        long[] statistics = new long[4];
        BigInteger[] result = evaluateRecurrence(inputSize, cachedResults, output, statistics, 0);
        BigDecimal decimalResult = toDecimal(result);
        if (output != null) {
            output.printf(
                    "Calculated inputs: %d. Reused results: %d.%n",
                    statistics[STATE_COUNT], statistics[REUSE_COUNT]);
            if (!hasTerminatingDecimal(result)) {
                output.println(
                        "The decimal result is rounded to 34 significant digits; intermediate calculations are exact.");
            }
        }
        return decimalResult;
    }

    private BigInteger[] evaluateRecurrence(
            long inputSize,
            Map<Long, BigInteger[]> cachedResults,
            PrintStream output,
            long[] statistics,
            int depth) {
        BigInteger[] cachedResult = cachedResults.get(inputSize);
        if (cachedResult != null) {
            recordReuse(inputSize, cachedResult, output, statistics);
            return cachedResult;
        }
        if (depth >= MAX_RECURSIVE_DEPTH) {
            return evaluateWithWorkStack(inputSize, cachedResults, output, statistics);
        }
        startInput(inputSize, output, statistics);
        int selectedNode = inputSize <= baseThreshold ? baseNode : recurrenceNode;
        BigInteger[] result =
                evaluateNode(
                        selectedNode, inputSize, cachedResults, output, statistics, depth, null);
        finishInput(inputSize, result, cachedResults, output, statistics);
        return result;
    }

    private BigInteger[] evaluateWithWorkStack(
            long inputSize,
            Map<Long, BigInteger[]> cachedResults,
            PrintStream output,
            long[] statistics) {
        Deque<Long> pendingInputs = new ArrayDeque<>();
        pendingInputs.push(inputSize);
        startInput(inputSize, output, statistics);
        while (!pendingInputs.isEmpty()) {
            long currentInput = pendingInputs.peek();
            int selectedNode = currentInput <= baseThreshold ? baseNode : recurrenceNode;
            Long[] missingInput = new Long[1];
            BigInteger[] result =
                    evaluateNode(
                            selectedNode,
                            currentInput,
                            cachedResults,
                            output,
                            statistics,
                            MAX_RECURSIVE_DEPTH,
                            missingInput);
            if (result == null) {
                long dependency = missingInput[0];
                startInput(dependency, output, statistics);
                pendingInputs.push(dependency);
            } else {
                finishInput(currentInput, result, cachedResults, output, statistics);
                pendingInputs.pop();
            }
        }
        return cachedResults.get(inputSize);
    }

    private void startInput(long inputSize, PrintStream output, long[] statistics) {
        if (++statistics[STATE_COUNT] > MAX_EVALUATED_STATES) {
            throw new IllegalArgumentException(
                    "The recurrence requires more than "
                            + MAX_EVALUATED_STATES
                            + " distinct inputs.");
        }
        if (inputSize > baseThreshold && canTrace(output, statistics)) {
            trace(
                    output,
                    statistics,
                    "Expand T("
                            + inputSize
                            + "): "
                            + describeNode(recurrenceNode, inputSize, null)
                            + ".");
        }
    }

    private void finishInput(
            long inputSize,
            BigInteger[] result,
            Map<Long, BigInteger[]> cachedResults,
            PrintStream output,
            long[] statistics) {
        statistics[CACHED_BITS] += result[0].bitLength() + result[1].bitLength();
        if (statistics[CACHED_BITS] > MAX_CACHED_BITS) {
            throw new IllegalArgumentException(
                    "The stored results exceed the calculation memory budget.");
        }
        cachedResults.put(inputSize, result);
        if (canTrace(output, statistics)) {
            if (inputSize <= baseThreshold) {
                trace(
                        output,
                        statistics,
                        "Base T("
                                + inputSize
                                + "): "
                                + describeNode(baseNode, inputSize, null)
                                + " = "
                                + display(result)
                                + ".");
            } else {
                trace(
                        output,
                        statistics,
                        "Substitute T("
                                + inputSize
                                + "): "
                                + describeNode(recurrenceNode, inputSize, cachedResults)
                                + " = "
                                + display(result)
                                + ".");
            }
        }
    }

    private BigInteger[] evaluateNode(
            int node,
            long inputSize,
            Map<Long, BigInteger[]> cachedResults,
            PrintStream output,
            long[] statistics,
            int depth,
            Long[] missingInput) {
        String operation = nodeOperations.get(node);
        int[] arguments = nodeArguments.get(node);
        if (operation.equals("number")) {
            return nodeValues.get(node);
        }
        if (operation.equals("n")) {
            return integer(BigInteger.valueOf(inputSize));
        }
        if (operation.equals("T")) {
            BigInteger[] argument =
                    evaluateNode(
                            arguments[0],
                            inputSize,
                            cachedResults,
                            output,
                            statistics,
                            depth,
                            missingInput);
            long childInput = recursiveInput(argument, inputSize);
            if (missingInput != null) {
                BigInteger[] cachedResult = cachedResults.get(childInput);
                if (cachedResult == null) {
                    missingInput[0] = childInput;
                } else {
                    recordReuse(childInput, cachedResult, output, statistics);
                }
                return cachedResult;
            }
            return evaluateRecurrence(childInput, cachedResults, output, statistics, depth + 1);
        }
        BigInteger[] left =
                evaluateNode(
                        arguments[0],
                        inputSize,
                        cachedResults,
                        output,
                        statistics,
                        depth,
                        missingInput);
        if (left == null) {
            return null;
        }
        if (operation.equals("if")) {
            int selectedBranch = left[0].signum() == 0 ? arguments[2] : arguments[1];
            return evaluateNode(
                    selectedBranch,
                    inputSize,
                    cachedResults,
                    output,
                    statistics,
                    depth,
                    missingInput);
        }
        if (arguments.length == 1) {
            return applyUnary(operation, left);
        }
        BigInteger[] right =
                evaluateNode(
                        arguments[1],
                        inputSize,
                        cachedResults,
                        output,
                        statistics,
                        depth,
                        missingInput);
        return right == null ? null : applyBinary(operation, left, right);
    }

    private static long recursiveInput(BigInteger[] argument, long parentInput) {
        if (argument[0].signum() < 0) {
            throw new IllegalArgumentException("A recursive input cannot be negative.");
        }
        BigInteger childInput = argument[0].divide(argument[1]);
        if (childInput.compareTo(BigInteger.valueOf(parentInput)) >= 0) {
            throw new IllegalArgumentException(
                    "Each recursive input must be smaller than its parent: T("
                            + parentInput
                            + ") requested T("
                            + childInput
                            + ").");
        }
        return childInput.longValueExact();
    }

    private static BigInteger[] applyUnary(String operation, BigInteger[] value) {
        return switch (operation) {
            case "positive" -> value;
            case "negative" -> rational(value[0].negate(), value[1]);
            case "abs" -> rational(value[0].abs(), value[1]);
            case "floor" -> integer(floor(value));
            case "ceil" -> integer(floor(new BigInteger[] {value[0].negate(), value[1]}).negate());
            default -> throw new IllegalArgumentException("Unknown operation: " + operation + ".");
        };
    }

    private static BigInteger[] applyBinary(
            String operation, BigInteger[] left, BigInteger[] right) {
        return switch (operation) {
            case "+" -> add(left, right, false);
            case "-" -> add(left, right, true);
            case "*" -> multiply(left, right);
            case "/" -> multiply(left, rational(right[1], right[0]));
            case "%" -> remainder(left, right);
            case "^" -> power(left, right);
            case "min" -> compare(left, right) <= 0 ? left : right;
            case "max" -> compare(left, right) >= 0 ? left : right;
            case "==" -> truth(compare(left, right) == 0);
            case "!=" -> truth(compare(left, right) != 0);
            case "<" -> truth(compare(left, right) < 0);
            case "<=" -> truth(compare(left, right) <= 0);
            case ">" -> truth(compare(left, right) > 0);
            case ">=" -> truth(compare(left, right) >= 0);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation + ".");
        };
    }

    private static BigInteger[] add(BigInteger[] left, BigInteger[] right, boolean subtract) {
        BigInteger commonDivisor = left[1].gcd(right[1]);
        BigInteger leftScale = right[1].divide(commonDivisor);
        BigInteger rightScale = left[1].divide(commonDivisor);
        BigInteger leftNumerator = left[0].multiply(leftScale);
        BigInteger rightNumerator = right[0].multiply(rightScale);
        return rational(
                subtract
                        ? leftNumerator.subtract(rightNumerator)
                        : leftNumerator.add(rightNumerator),
                left[1].multiply(leftScale));
    }

    private static BigInteger[] multiply(BigInteger[] left, BigInteger[] right) {
        BigInteger firstDivisor = left[0].gcd(right[1]);
        BigInteger secondDivisor = right[0].gcd(left[1]);
        return rational(
                left[0].divide(firstDivisor).multiply(right[0].divide(secondDivisor)),
                left[1].divide(secondDivisor).multiply(right[1].divide(firstDivisor)));
    }

    private static BigInteger[] remainder(BigInteger[] left, BigInteger[] right) {
        if (!left[1].equals(BigInteger.ONE) || !right[1].equals(BigInteger.ONE)) {
            throw new IllegalArgumentException("The remainder operator requires integer operands.");
        }
        if (right[0].signum() == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed.");
        }
        return integer(left[0].remainder(right[0]));
    }

    private static BigInteger[] power(BigInteger[] value, BigInteger[] exponentValue) {
        if (!exponentValue[1].equals(BigInteger.ONE)
                || exponentValue[0].abs().compareTo(BigInteger.valueOf(MAX_EXPONENT)) > 0) {
            throw new IllegalArgumentException(
                    "The exponent must be an integer between -"
                            + MAX_EXPONENT
                            + " and "
                            + MAX_EXPONENT
                            + ".");
        }
        int exponent = exponentValue[0].intValueExact();
        int magnitude = Math.abs(exponent);
        if ((long) Math.max(0, value[0].abs().bitLength() - 1) * magnitude >= MAX_NUMBER_BITS
                || (long) Math.max(0, value[1].bitLength() - 1) * magnitude >= MAX_NUMBER_BITS) {
            throw new IllegalArgumentException("The power exceeds the supported number size.");
        }
        BigInteger numerator = value[0].pow(magnitude);
        BigInteger denominator = value[1].pow(magnitude);
        return exponent < 0 ? rational(denominator, numerator) : rational(numerator, denominator);
    }

    private static BigInteger floor(BigInteger[] value) {
        BigInteger[] quotientAndRemainder = value[0].divideAndRemainder(value[1]);
        return value[0].signum() < 0 && quotientAndRemainder[1].signum() != 0
                ? quotientAndRemainder[0].subtract(BigInteger.ONE)
                : quotientAndRemainder[0];
    }

    private static int compare(BigInteger[] left, BigInteger[] right) {
        return left[0].multiply(right[1]).compareTo(right[0].multiply(left[1]));
    }

    private static BigInteger[] truth(boolean condition) {
        return integer(condition ? BigInteger.ONE : BigInteger.ZERO);
    }

    private static BigInteger[] integer(BigInteger value) {
        return rational(value, BigInteger.ONE);
    }

    private static BigInteger[] rational(BigInteger numerator, BigInteger denominator) {
        if (denominator.signum() == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed.");
        }
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger divisor = numerator.gcd(denominator);
        numerator = numerator.divide(divisor);
        denominator = denominator.divide(divisor);
        if (numerator.bitLength() > MAX_NUMBER_BITS || denominator.bitLength() > MAX_NUMBER_BITS) {
            throw new IllegalArgumentException(
                    "A number exceeds the supported size of " + MAX_NUMBER_BITS + " bits.");
        }
        return new BigInteger[] {numerator, denominator};
    }

    private static BigDecimal toDecimal(BigInteger[] value) {
        BigDecimal numerator = new BigDecimal(value[0]);
        BigDecimal denominator = new BigDecimal(value[1]);
        try {
            return numerator.divide(denominator).stripTrailingZeros();
        } catch (ArithmeticException exception) {
            return numerator.divide(denominator, MathContext.DECIMAL128).stripTrailingZeros();
        }
    }

    private static boolean hasTerminatingDecimal(BigInteger[] value) {
        BigInteger remaining = value[1].shiftRight(value[1].getLowestSetBit());
        BigInteger five = BigInteger.valueOf(5);
        while (remaining.mod(five).signum() == 0) {
            remaining = remaining.divide(five);
        }
        return remaining.equals(BigInteger.ONE);
    }

    private int parse(String expression) {
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException(
                    "An expression may contain at most " + MAX_EXPRESSION_LENGTH + " characters.");
        }
        parserText = expression;
        parserPosition = 0;
        int root = parseExpression(0, 0);
        skipWhitespace();
        if (parserPosition != parserText.length()) {
            throw parserError("Unexpected input");
        }
        validateExpressionDepth(root, 0);
        return root;
    }

    private int parseExpression(int minimumPrecedence, int depth) {
        if (depth > MAX_EXPRESSION_DEPTH) {
            throw parserError("The expression is nested too deeply");
        }
        skipWhitespace();
        int left;
        if (consume("+")) {
            left = addNode("positive", new int[] {parseExpression(30, depth + 1)}, null);
        } else if (consume("-")) {
            left = addNode("negative", new int[] {parseExpression(30, depth + 1)}, null);
        } else {
            left = parsePrimary(depth + 1);
        }
        while (true) {
            skipWhitespace();
            String operation = nextOperator();
            int precedence = precedence(operation);
            if (precedence < minimumPrecedence) {
                return left;
            }
            parserPosition += operation.length();
            int right =
                    parseExpression(operation.equals("^") ? precedence : precedence + 1, depth + 1);
            left = addNode(operation, new int[] {left, right}, null);
        }
    }

    private int parsePrimary(int depth) {
        skipWhitespace();
        if (consume("(")) {
            int inner = parseExpression(0, depth);
            require(")");
            return inner;
        }
        if (parserPosition >= parserText.length()) {
            throw parserError("Expected a number, n, or a function");
        }
        char firstCharacter = parserText.charAt(parserPosition);
        if (firstCharacter >= '0' && firstCharacter <= '9' || firstCharacter == '.') {
            return parseNumber();
        }
        int nameStart = parserPosition;
        while (parserPosition < parserText.length()
                && isAsciiLetter(parserText.charAt(parserPosition))) {
            parserPosition++;
        }
        String name = parserText.substring(nameStart, parserPosition);
        if (name.equals("n")) {
            return addNode("n", new int[0], null);
        }
        int argumentCount =
                switch (name) {
                    case "T", "floor", "ceil", "abs" -> 1;
                    case "min", "max" -> 2;
                    case "if" -> 3;
                    default ->
                            throw parserError(
                                    "Unknown name '" + name + "'; use n or a supported function");
                };
        require("(");
        int[] arguments = new int[argumentCount];
        for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
            if (argumentIndex > 0) {
                require(",");
            }
            arguments[argumentIndex] = parseExpression(0, depth);
        }
        require(")");
        if ((name.equals("T") || name.equals("if")) && containsRecurrence(arguments[0])) {
            throw parserError(
                    name.equals("T")
                            ? "The argument of T cannot contain another T call"
                            : "An if condition cannot contain T calls");
        }
        return addNode(name, arguments, null);
    }

    private int parseNumber() {
        int numberStart = parserPosition;
        boolean decimalPoint = false;
        while (parserPosition < parserText.length()) {
            char character = parserText.charAt(parserPosition);
            if (character >= '0' && character <= '9') {
                parserPosition++;
            } else if (character == '.' && !decimalPoint) {
                decimalPoint = true;
                parserPosition++;
            } else {
                break;
            }
        }
        try {
            BigDecimal value = new BigDecimal(parserText.substring(numberStart, parserPosition));
            return addNode(
                    "number",
                    new int[0],
                    rational(value.unscaledValue(), BigInteger.TEN.pow(value.scale())));
        } catch (NumberFormatException exception) {
            throw parserError("Invalid number");
        }
    }

    private int addNode(String operation, int[] arguments, BigInteger[] value) {
        if (nodeOperations.size() >= MAX_EXPRESSION_NODES) {
            throw parserError("The expressions contain too many operations");
        }
        int node = nodeOperations.size();
        nodeOperations.add(operation);
        nodeArguments.add(arguments);
        nodeValues.add(value);
        return node;
    }

    private boolean containsRecurrence(int node) {
        if (nodeOperations.get(node).equals("T")) {
            return true;
        }
        for (int argument : nodeArguments.get(node)) {
            if (containsRecurrence(argument)) {
                return true;
            }
        }
        return false;
    }

    private void validateExpressionDepth(int node, int depth) {
        if (depth > MAX_EXPRESSION_DEPTH) {
            throw parserError("The expression is nested too deeply");
        }
        for (int argument : nodeArguments.get(node)) {
            validateExpressionDepth(argument, depth + 1);
        }
    }

    private String nextOperator() {
        for (String operation :
                new String[] {"==", "!=", "<=", ">=", "+", "-", "*", "/", "%", "^", "<", ">"}) {
            if (parserText.startsWith(operation, parserPosition)) {
                return operation;
            }
        }
        return "";
    }

    private static int precedence(String operation) {
        return switch (operation) {
            case "==", "!=", "<", "<=", ">", ">=" -> 5;
            case "+", "-" -> 10;
            case "*", "/", "%" -> 20;
            case "^" -> 40;
            default -> -1;
        };
    }

    private void require(String token) {
        skipWhitespace();
        if (!consume(token)) {
            throw parserError("Expected '" + token + "'");
        }
    }

    private boolean consume(String token) {
        if (parserText.startsWith(token, parserPosition)) {
            parserPosition += token.length();
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (parserPosition < parserText.length()
                && Character.isWhitespace(parserText.charAt(parserPosition))) {
            parserPosition++;
        }
    }

    private IllegalArgumentException parserError(String message) {
        return new IllegalArgumentException(message + " at position " + (parserPosition + 1) + ".");
    }

    private static boolean isAsciiLetter(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z';
    }

    private String describeNode(int node, long inputSize, Map<Long, BigInteger[]> cachedResults) {
        String operation = nodeOperations.get(node);
        int[] arguments = nodeArguments.get(node);
        if (operation.equals("number")) {
            return display(nodeValues.get(node));
        }
        if (operation.equals("n")) {
            return Long.toString(inputSize);
        }
        if (operation.equals("if")) {
            BigInteger[] condition =
                    evaluateNode(arguments[0], inputSize, null, null, null, 0, null);
            return describeNode(
                    arguments[condition[0].signum() == 0 ? 2 : 1], inputSize, cachedResults);
        }
        if (operation.equals("T")) {
            BigInteger[] argument =
                    evaluateNode(arguments[0], inputSize, null, null, null, 0, null);
            long childInput = recursiveInput(argument, inputSize);
            if (cachedResults != null && cachedResults.containsKey(childInput)) {
                return display(cachedResults.get(childInput));
            }
            return "T(" + childInput + ")";
        }
        String left = describeNode(arguments[0], inputSize, cachedResults);
        if (operation.equals("negative") || operation.equals("positive")) {
            return shorten("(" + (operation.equals("negative") ? "-" : "+") + left + ")");
        }
        if (arguments.length == 1) {
            return shorten(operation + "(" + left + ")");
        }
        String right = describeNode(arguments[1], inputSize, cachedResults);
        return shorten(
                operation.equals("min") || operation.equals("max")
                        ? operation + "(" + left + ", " + right + ")"
                        : "(" + left + " " + operation + " " + right + ")");
    }

    private static String display(BigInteger[] value) {
        if (value[1].equals(BigInteger.ONE)) {
            return shorten(value[0].toString());
        }
        return shorten(value[0] + "/" + value[1]);
    }

    private static String shorten(String text) {
        return text.length() <= TRACE_LENGTH ? text : text.substring(0, TRACE_LENGTH) + "...";
    }

    private static void recordReuse(
            long inputSize, BigInteger[] result, PrintStream output, long[] statistics) {
        statistics[REUSE_COUNT]++;
        if (canTrace(output, statistics)) {
            trace(output, statistics, "Reuse T(" + inputSize + ") = " + display(result) + ".");
        }
    }

    private static boolean canTrace(PrintStream output, long[] statistics) {
        return output != null && statistics[TRACE_COUNT] <= MAX_TRACE_EVENTS;
    }

    private static void trace(PrintStream output, long[] statistics, String message) {
        if (statistics[TRACE_COUNT] < MAX_TRACE_EVENTS) {
            output.println(message);
        } else if (statistics[TRACE_COUNT] == MAX_TRACE_EVENTS) {
            output.println("Further steps are hidden; the calculation continues.");
        }
        statistics[TRACE_COUNT]++;
    }

    public static void runConsole(Reader input, PrintStream output) {
        Objects.requireNonNull(input, "Input is required.");
        Objects.requireNonNull(output, "Output is required.");
        BufferedReader reader = new BufferedReader(input);
        output.println("Recurrence solver");
        output.println("Enter an expression such as 2*T(n/2)+n. Use * for multiplication.");
        output.println("Operators: + - * / % ^. Functions: T, floor, ceil, abs, min, max, if.");
        output.println("Use n and numbers; replace constants such as c with a number.");
        output.println("Recursive inputs are rounded down. Enter q at any prompt to exit.");
        try {
            while (true) {
                output.println();
                String expression = readLine(reader, output, "Expression: ");
                long inputSize = readNonnegativeLong(reader, output, "Input size n: ");
                long baseThreshold =
                        readNonnegativeLong(reader, output, "Base threshold (n <= threshold): ");
                String baseExpression = readLine(reader, output, "Base expression: ");
                boolean showSteps = readTraceChoice(reader, output);
                try {
                    RecurrenceSolver solver =
                            new RecurrenceSolver(expression, baseThreshold, baseExpression);
                    BigDecimal result = solver.solve(inputSize, showSteps ? output : null);
                    if (!showSteps) {
                        output.println(
                                "Intermediate calculations are exact; nonterminating decimal results are rounded to 34 significant digits.");
                    }
                    output.printf("Result: T(%d) = %s%n", inputSize, result.toPlainString());
                } catch (IllegalArgumentException exception) {
                    output.println("Cannot calculate: " + exception.getMessage());
                    output.println("Enter a new expression to try again.");
                }
            }
        } catch (EOFException exception) {
            output.println("Goodbye.");
        } catch (IOException exception) {
            output.println("Unable to read the input.");
        }
    }

    private static long readNonnegativeLong(
            BufferedReader reader, PrintStream output, String prompt) throws IOException {
        while (true) {
            String input = readLine(reader, output, prompt);
            try {
                long value = Long.parseLong(input);
                if (value >= 0) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                output.println("Enter a nonnegative integer up to " + Long.MAX_VALUE + ".");
                continue;
            }
            output.println("Enter a nonnegative integer up to " + Long.MAX_VALUE + ".");
        }
    }

    private static boolean readTraceChoice(BufferedReader reader, PrintStream output)
            throws IOException {
        while (true) {
            String input = readLine(reader, output, "Show steps? [1=yes, 0=no]: ");
            if (input.equals("1") || input.equals("0")) {
                return input.equals("1");
            }
            output.println("Enter 1 to show steps or 0 to show only the result.");
        }
    }

    private static String readLine(BufferedReader reader, PrintStream output, String prompt)
            throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            StringBuilder line = new StringBuilder();
            boolean tooLong = false;
            int character;
            while ((character = reader.read()) != -1 && character != '\n' && character != '\r') {
                if (line.length() < MAX_EXPRESSION_LENGTH) {
                    line.append((char) character);
                } else {
                    tooLong = true;
                }
            }
            if (character == '\r') {
                reader.mark(1);
                if (reader.read() != '\n') {
                    reader.reset();
                }
            }
            if (character == -1 && line.isEmpty()) {
                throw new EOFException();
            }
            if (tooLong) {
                output.println(
                        "The input is too long. Enter at most "
                                + MAX_EXPRESSION_LENGTH
                                + " characters.");
                continue;
            }
            String value = line.toString().trim();
            if (value.equalsIgnoreCase("q")) {
                throw new EOFException();
            }
            return value;
        }
    }

    public static void main(String[] arguments) {
        runConsole(new InputStreamReader(System.in, StandardCharsets.UTF_8), System.out);
    }
}
