import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class RecurrenceSolver {
    public static final int MAX_TRACE_DEPTH = 128;
    private static final int MAX_INPUT_LENGTH = 80;

    private final boolean divideInput;
    private final long reduction;
    private final long baseThreshold;
    private final BigInteger baseValue;
    private final BigInteger linearCoefficient;
    private final BigInteger constantTerm;

    public RecurrenceSolver(
            boolean divideInput,
            long reduction,
            long baseThreshold,
            long baseValue,
            long linearCoefficient,
            long constantTerm) {
        if (reduction < (divideInput ? 2 : 1)) {
            throw new IllegalArgumentException(
                    divideInput ? "The divisor must be at least 2." : "The step must be positive.");
        }
        if (baseThreshold < 0) {
            throw new IllegalArgumentException("The base threshold must be nonnegative.");
        }
        this.divideInput = divideInput;
        this.reduction = reduction;
        this.baseThreshold = baseThreshold;
        this.baseValue = BigInteger.valueOf(baseValue);
        this.linearCoefficient = BigInteger.valueOf(linearCoefficient);
        this.constantTerm = BigInteger.valueOf(constantTerm);
    }

    public BigInteger solve(long inputSize) {
        validateInputSize(inputSize);
        if (inputSize <= baseThreshold) {
            return baseValue;
        }
        if (divideInput) {
            return evaluateRecursively(inputSize, 0, null);
        }
        return solveSubtraction(inputSize);
    }

    public BigInteger solveWithTrace(long inputSize, PrintStream output) {
        Objects.requireNonNull(output, "Output is required.");
        long expansionCount = countExpansions(inputSize);
        if (expansionCount > MAX_TRACE_DEPTH) {
            output.printf(
                    "Full trace requires %d expansions; the limit is %d.%n",
                    expansionCount, MAX_TRACE_DEPTH);
            output.println(
                    "Using the equivalent arithmetic sum; individual calls are not executed.");
            printSubtractionFormula(inputSize, output);
            return solveSubtraction(inputSize);
        }
        return evaluateRecursively(inputSize, 0, output);
    }

    public long countExpansions(long inputSize) {
        validateInputSize(inputSize);
        if (inputSize <= baseThreshold) {
            return 0;
        }
        if (!divideInput) {
            return (inputSize - baseThreshold - 1) / reduction + 1;
        }
        long expansionCount = 0;
        while (inputSize > baseThreshold) {
            inputSize /= reduction;
            expansionCount++;
        }
        return expansionCount;
    }

    private BigInteger evaluateRecursively(long inputSize, int depth, PrintStream output) {
        if (inputSize <= baseThreshold) {
            if (output != null) {
                output.printf("[%03d] BASE   T(%d) = %s%n", depth, inputSize, baseValue);
            }
            return baseValue;
        }
        long nextInputSize = divideInput ? inputSize / reduction : inputSize - reduction;
        BigInteger localCost =
                linearCoefficient.multiply(BigInteger.valueOf(inputSize)).add(constantTerm);
        if (output != null) {
            output.printf(
                    "[%03d] EXPAND T(%d) = T(%d) + (%s * %d + %s) = T(%d) + (%s)%n",
                    depth,
                    inputSize,
                    nextInputSize,
                    linearCoefficient,
                    inputSize,
                    constantTerm,
                    nextInputSize,
                    localCost);
        }
        BigInteger smallerResult = evaluateRecursively(nextInputSize, depth + 1, output);
        BigInteger result = smallerResult.add(localCost);
        if (output != null) {
            output.printf(
                    "[%03d] RETURN T(%d) = %s + (%s) = %s%n",
                    depth, inputSize, smallerResult, localCost, result);
        }
        return result;
    }

    private BigInteger solveSubtraction(long inputSize) {
        BigInteger expansionCount = BigInteger.valueOf(countExpansions(inputSize));
        BigInteger inputSum = sumExpandedInputs(inputSize, expansionCount);
        return baseValue
                .add(linearCoefficient.multiply(inputSum))
                .add(constantTerm.multiply(expansionCount));
    }

    private BigInteger sumExpandedInputs(long inputSize, BigInteger expansionCount) {
        BigInteger firstInput = BigInteger.valueOf(inputSize);
        BigInteger twiceAverage =
                firstInput
                        .multiply(BigInteger.TWO)
                        .subtract(
                                expansionCount
                                        .subtract(BigInteger.ONE)
                                        .multiply(BigInteger.valueOf(reduction)));
        return expansionCount.multiply(twiceAverage).divide(BigInteger.TWO);
    }

    private void printSubtractionFormula(long inputSize, PrintStream output) {
        BigInteger expansionCount = BigInteger.valueOf(countExpansions(inputSize));
        BigInteger terminalInput =
                BigInteger.valueOf(inputSize)
                        .subtract(expansionCount.multiply(BigInteger.valueOf(reduction)));
        BigInteger inputSum = sumExpandedInputs(inputSize, expansionCount);
        output.println("k = ceil((n - baseThreshold) / step)");
        output.println("S = k * (2*n - (k - 1)*step) / 2");
        output.println("T(n) = baseValue + linearCoefficient*S + constantTerm*k");
        output.printf(
                "k = %s; terminal input = %s; S = %s%n", expansionCount, terminalInput, inputSum);
        output.printf(
                "T(%d) = %s + (%s * %s) + (%s * %s)%n",
                inputSize, baseValue, linearCoefficient, inputSum, constantTerm, expansionCount);
    }

    private static void validateInputSize(long inputSize) {
        if (inputSize < 0) {
            throw new IllegalArgumentException("The input size must be nonnegative.");
        }
    }

    private void printProblem(long inputSize, PrintStream output) {
        output.println();
        output.println("=== 1. PROBLEM ===");
        String smallerInput = divideInput ? "floor(n / " + reduction + ")" : "n - " + reduction;
        output.printf(
                "T(n) = T(%s) + (%s)*n + (%s), for n > %d%n",
                smallerInput, linearCoefficient, constantTerm, baseThreshold);
        output.printf("T(n) = %s, for every integer n <= %d%n", baseValue, baseThreshold);
        output.printf("Requested input: %d%n", inputSize);
        output.printf("Expansions in the original recurrence: %d%n", countExpansions(inputSize));
    }

    private void printAnalysis(long inputSize, boolean detailed, PrintStream output) {
        output.println();
        output.println("=== 4. ANALYSIS ===");
        if (inputSize <= baseThreshold) {
            output.println("Evaluation: base case, no expansion.");
        } else if (divideInput) {
            output.println("Evaluation: O(log n) arithmetic operations and O(log n) stack frames.");
        } else if (detailed && countExpansions(inputSize) <= MAX_TRACE_DEPTH) {
            output.println(
                    "Evaluation: O(k) arithmetic operations and O(k) stack frames; k expansions.");
        } else {
            output.println("Evaluation: O(1) arithmetic operations and O(1) stack frames.");
        }
        output.println(
                "BigInteger operation costs depend on the number of bits; output also has a cost.");
        output.println(
                "The growth of T(n) is different from the work used to calculate its value.");
        if (linearCoefficient.signum() < 0 || constantTerm.signum() < 0 || baseValue.signum() < 0) {
            output.println("Signed values define a sequence; no running-time growth is inferred.");
            return;
        }
        String growth;
        if (linearCoefficient.signum() > 0) {
            growth = divideInput ? "Theta(n)" : "Theta(n^2)";
        } else if (constantTerm.signum() > 0) {
            growth = divideInput ? "Theta(log n)" : "Theta(n)";
        } else {
            growth = baseValue.signum() == 0 ? "identically zero" : "Theta(1)";
        }
        output.printf("Model growth for fixed parameters as n increases: %s.%n", growth);
    }

    public static void runConsole(Reader input, PrintStream output) {
        Objects.requireNonNull(input, "Input is required.");
        Objects.requireNonNull(output, "Output is required.");
        BufferedReader reader = new BufferedReader(input);
        output.println("RECURRENCE SOLVER");
        output.println("One recursive term and an affine local cost: a*n + b.");
        output.println(
                "All numbers are signed 64-bit integers; results use exact BigInteger arithmetic.");
        output.println("Enter q at any prompt to exit.");
        try {
            while (true) {
                output.println();
                output.println("1. Subtraction: T(n) = T(n - step) + a*n + b");
                output.println("2. Division:    T(n) = T(floor(n / divisor)) + a*n + b");
                output.println("0. Exit");
                long choice = readLong(reader, output, "Choose a recurrence [0..2]: ", 0, 2);
                if (choice == 0) {
                    output.println("Goodbye.");
                    return;
                }
                boolean divideInput = choice == 2;
                long inputSize =
                        readLong(
                                reader,
                                output,
                                "Input size n [0..Long.MAX_VALUE]: ",
                                0,
                                Long.MAX_VALUE);
                long baseThreshold =
                        readLong(reader, output, "Base threshold: ", 0, Long.MAX_VALUE);
                long baseValue =
                        readLong(
                                reader,
                                output,
                                "Base value T(n) for all n <= threshold: ",
                                Long.MIN_VALUE,
                                Long.MAX_VALUE);
                long reduction =
                        readLong(
                                reader,
                                output,
                                divideInput ? "Divisor [at least 2]: " : "Step [at least 1]: ",
                                divideInput ? 2 : 1,
                                Long.MAX_VALUE);
                long linearCoefficient =
                        readLong(
                                reader,
                                output,
                                "Linear coefficient a: ",
                                Long.MIN_VALUE,
                                Long.MAX_VALUE);
                long constantTerm =
                        readLong(
                                reader,
                                output,
                                "Constant term b: ",
                                Long.MIN_VALUE,
                                Long.MAX_VALUE);
                boolean detailed =
                        readLong(
                                        reader,
                                        output,
                                        "Show recursive steps? [1=yes, 0=optimized]: ",
                                        0,
                                        1)
                                == 1;
                RecurrenceSolver solver =
                        new RecurrenceSolver(
                                divideInput,
                                reduction,
                                baseThreshold,
                                baseValue,
                                linearCoefficient,
                                constantTerm);
                solver.printProblem(inputSize, output);
                output.println();
                output.println("=== 2. EVALUATION ===");
                BigInteger result;
                if (detailed) {
                    result = solver.solveWithTrace(inputSize, output);
                } else {
                    if (!divideInput && inputSize > baseThreshold) {
                        solver.printSubtractionFormula(inputSize, output);
                    } else {
                        output.println("Exact evaluation with per-call output disabled.");
                    }
                    result = solver.solve(inputSize);
                }
                output.println();
                output.println("=== 3. RESULT ===");
                output.printf("T(%d) = %s%n", inputSize, result);
                solver.printAnalysis(inputSize, detailed, output);
            }
        } catch (EOFException exception) {
            output.println();
            output.println("Input ended. Goodbye.");
        } catch (IOException exception) {
            output.println();
            output.println("Unable to read input. Restart with lines of at most 80 characters.");
        }
    }

    private static long readLong(
            BufferedReader reader, PrintStream output, String prompt, long minimum, long maximum)
            throws IOException {
        while (true) {
            output.print(prompt);
            output.flush();
            String value = readBoundedLine(reader).trim();
            if (value.equalsIgnoreCase("q")) {
                throw new EOFException();
            }
            try {
                long number = Long.parseLong(value);
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException exception) {
                output.println("Invalid integer.");
                continue;
            }
            output.printf("Enter a value between %d and %d.%n", minimum, maximum);
        }
    }

    private static String readBoundedLine(BufferedReader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
            if (character == '\n') {
                return line.toString();
            }
            if (character == '\r') {
                reader.mark(1);
                if (reader.read() != '\n') {
                    reader.reset();
                }
                return line.toString();
            }
            if (line.length() == MAX_INPUT_LENGTH) {
                throw new IOException("Input length exceeded.");
            }
            line.append((char) character);
        }
        if (line.length() == 0) {
            throw new EOFException();
        }
        return line.toString();
    }

    public static void main(String[] arguments) {
        runConsole(new InputStreamReader(System.in, StandardCharsets.UTF_8), System.out);
    }
}
