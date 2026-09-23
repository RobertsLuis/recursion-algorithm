import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecurrenceSolverTest {
    @ParameterizedTest
    @CsvSource({
        "false, 1, 1, 1, 2, 3, 1, 1",
        "false, 1, 1, 1, 2, 3, 2, 8",
        "false, 1, 1, 1, 2, 3, 3, 17",
        "false, 1, 1, 1, 2, 3, 4, 28",
        "false, 1, 1, 1, 2, 3, 5, 41",
        "false, 1, 1, 5, 2, 3, 1, 5",
        "false, 1, 1, 5, 2, 3, 2, 12",
        "false, 1, 1, 5, 2, 3, 3, 21",
        "false, 1, 1, 5, 2, 3, 4, 32",
        "false, 1, 1, 5, 2, 3, 5, 45",
        "false, 1, 1, 1, 0, 3, 5, 13",
        "false, 1, 0, 0, 1, 0, 5, 15",
        "true, 2, 1, 1, 0, 5, 16, 21",
        "true, 2, 1, 1, 0, 5, 32, 26",
        "true, 3, 1, 7, 0, 4, 81, 23"
    })
    void reproducesLectureSequencesAndParameterizedVariants(
            boolean divideInput,
            long reduction,
            long baseThreshold,
            long baseValue,
            long linearCoefficient,
            long constantTerm,
            long inputSize,
            String expected) {
        RecurrenceSolver solver =
                new RecurrenceSolver(
                        divideInput,
                        reduction,
                        baseThreshold,
                        baseValue,
                        linearCoefficient,
                        constantTerm);
        assertEquals(new BigInteger(expected), solver.solve(inputSize));
        assertEquals(new BigInteger(expected), solveSilentlyWithTrace(solver, inputSize));
    }

    @ParameterizedTest
    @CsvSource({
        "false, 3, 2, 7, 2, 1, 10, 52",
        "false, 5, 0, 4, 1, 0, 2, 6",
        "true, 2, 1, 1, 0, 5, 15, 16",
        "true, 2, 0, 0, 1, 0, 10, 18",
        "true, 100, 0, 8, 2, 3, 10, 31",
        "true, 2, 3, 5, 1, 0, 10, 20",
        "false, 2, 1, -7, -3, 4, 6, -31",
        "true, 2, 0, -1, -2, -3, 7, -32"
    })
    void handlesRoundingThresholdCrossingAndSignedSequences(
            boolean divideInput,
            long reduction,
            long baseThreshold,
            long baseValue,
            long linearCoefficient,
            long constantTerm,
            long inputSize,
            String expected) {
        RecurrenceSolver solver =
                new RecurrenceSolver(
                        divideInput,
                        reduction,
                        baseThreshold,
                        baseValue,
                        linearCoefficient,
                        constantTerm);
        assertEquals(new BigInteger(expected), solver.solve(inputSize));
        assertEquals(new BigInteger(expected), solveSilentlyWithTrace(solver, inputSize));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 4, 5})
    void returnsBaseValueAtOrBelowThreshold(long inputSize) {
        for (boolean divideInput : new boolean[] {false, true}) {
            RecurrenceSolver solver = new RecurrenceSolver(divideInput, 2, 5, -17, 8, 9);
            assertEquals(BigInteger.valueOf(-17), solver.solve(inputSize));
            assertEquals(0, solver.countExpansions(inputSize));
            assertEquals(BigInteger.valueOf(-17), solveSilentlyWithTrace(solver, inputSize));
        }
    }

    @Test
    void matchesIndependentIterationForTwoThousandGeneratedProblems() {
        Random random = new Random(20260923);
        for (int scenario = 0; scenario < 2000; scenario++) {
            boolean divideInput = random.nextBoolean();
            long reduction = random.nextInt(12) + (divideInput ? 2 : 1);
            long baseThreshold = random.nextInt(10);
            long baseValue = random.nextInt(201) - 100;
            long linearCoefficient = random.nextInt(41) - 20;
            long constantTerm = random.nextInt(41) - 20;
            long inputSize = random.nextInt(120);
            RecurrenceSolver solver =
                    new RecurrenceSolver(
                            divideInput,
                            reduction,
                            baseThreshold,
                            baseValue,
                            linearCoefficient,
                            constantTerm);
            BigInteger expected = BigInteger.valueOf(baseValue);
            long expansions = 0;
            for (long currentInput = inputSize;
                    currentInput > baseThreshold;
                    currentInput =
                            divideInput ? currentInput / reduction : currentInput - reduction) {
                expected =
                        expected.add(
                                        BigInteger.valueOf(linearCoefficient)
                                                .multiply(BigInteger.valueOf(currentInput)))
                                .add(BigInteger.valueOf(constantTerm));
                expansions++;
            }
            assertEquals(expected, solver.solve(inputSize), "Optimized scenario " + scenario);
            assertEquals(
                    expected,
                    solveSilentlyWithTrace(solver, inputSize),
                    "Recursive scenario " + scenario);
            assertEquals(expansions, solver.countExpansions(inputSize));
        }
    }

    @Test
    void computesSumAtMaximumLongWithoutOverflowOrLinearWork() {
        long inputSize = Long.MAX_VALUE;
        BigInteger input = BigInteger.valueOf(inputSize);
        BigInteger expected = input.multiply(input.add(BigInteger.ONE)).divide(BigInteger.TWO);
        RecurrenceSolver solver = new RecurrenceSolver(false, 1, 0, 0, 1, 0);
        assertEquals(expected, solver.solve(inputSize));
        assertEquals(inputSize, solver.countExpansions(inputSize));
        assertEquals(expected, solveSilentlyWithTrace(solver, inputSize));
    }

    @Test
    void convertsToBigIntegerBeforeMultiplyingExtremeCoefficients() {
        BigInteger maximum = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger minimum = BigInteger.valueOf(Long.MIN_VALUE);
        RecurrenceSolver solver =
                new RecurrenceSolver(
                        false, Long.MAX_VALUE, 0, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE);
        BigInteger expected = maximum.multiply(maximum).add(minimum.multiply(BigInteger.TWO));
        assertEquals(expected, solver.solve(Long.MAX_VALUE));
        assertEquals(expected, solveSilentlyWithTrace(solver, Long.MAX_VALUE));
    }

    @Test
    void handlesAProductOfStepAndCountThatExceedsLongRange() {
        long step = Long.MAX_VALUE - 1;
        RecurrenceSolver solver = new RecurrenceSolver(false, step, 0, 3, 1, 0);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(4));
        assertEquals(2, solver.countExpansions(Long.MAX_VALUE));
        assertEquals(expected, solver.solve(Long.MAX_VALUE));
        assertEquals(expected, solveSilentlyWithTrace(solver, Long.MAX_VALUE));
    }

    @Test
    void halvesMaximumLongInSixtyThreeExpansions() {
        RecurrenceSolver solver = new RecurrenceSolver(true, 2, 0, 0, 0, 1);
        assertEquals(63, solver.countExpansions(Long.MAX_VALUE));
        assertEquals(BigInteger.valueOf(63), solver.solve(Long.MAX_VALUE));
        assertEquals(BigInteger.valueOf(63), solveSilentlyWithTrace(solver, Long.MAX_VALUE));
    }

    @Test
    void handlesMaximumBaseThreshold() {
        RecurrenceSolver solver = new RecurrenceSolver(false, 1, Long.MAX_VALUE, 27, 8, 9);
        assertEquals(0, solver.countExpansions(Long.MAX_VALUE));
        assertEquals(BigInteger.valueOf(27), solver.solve(Long.MAX_VALUE));
    }

    @Test
    void printsDescentBaseAndUnwindingInExecutionOrder() {
        RecurrenceSolver solver = new RecurrenceSolver(false, 1, 1, 1, 2, 3);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BigInteger result =
                solver.solveWithTrace(3, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        String trace = bytes.toString(StandardCharsets.UTF_8);
        String expected =
                String.join(
                        System.lineSeparator(),
                        "[000] EXPAND T(3) = T(2) + (2 * 3 + 3) = T(2) + (9)",
                        "[001] EXPAND T(2) = T(1) + (2 * 2 + 3) = T(1) + (7)",
                        "[002] BASE   T(1) = 1",
                        "[001] RETURN T(2) = 1 + (7) = 8",
                        "[000] RETURN T(3) = 8 + (9) = 17",
                        "");
        assertEquals(expected, trace);
        assertEquals(BigInteger.valueOf(17), result);
    }

    @Test
    void executesFullRecursionAtTraceBoundary() {
        RecurrenceSolver solver = new RecurrenceSolver(false, 1, 0, 0, 0, 1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertEquals(
                BigInteger.valueOf(128),
                solver.solveWithTrace(128, new PrintStream(bytes, true, StandardCharsets.UTF_8)));
        String trace = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(257, trace.lines().count());
        assertTrue(trace.contains("[128] BASE"));
        assertFalse(trace.contains("individual calls are not executed"));
    }

    @ParameterizedTest
    @ValueSource(longs = {129, 1000000000, Long.MAX_VALUE})
    void summarizesLargeTracesWithoutClaimingToExecuteCalls(long inputSize) {
        RecurrenceSolver solver = new RecurrenceSolver(false, 1, 0, 0, 0, 1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertEquals(
                BigInteger.valueOf(inputSize),
                solver.solveWithTrace(
                        inputSize, new PrintStream(bytes, true, StandardCharsets.UTF_8)));
        String trace = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(trace.contains("individual calls are not executed"));
        assertTrue(trace.contains("k = " + inputSize));
        assertFalse(trace.contains("EXPAND"));
        assertTrue(trace.lines().count() < 12);
    }

    @ParameterizedTest
    @CsvSource({
        "false, 0, 0",
        "false, -1, 0",
        "true, 0, 0",
        "true, 1, 0",
        "true, -2, 0",
        "true, 2, -1",
        "false, 1, -1"
    })
    void rejectsNonterminatingRulesAndNegativeThresholds(
            boolean divideInput, long reduction, long baseThreshold) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecurrenceSolver(divideInput, reduction, baseThreshold, 1, 0, 3));
    }

    @Test
    void rejectsNegativeRequestedInputAcrossPublicOperations() {
        RecurrenceSolver solver = new RecurrenceSolver(false, 1, 1, 1, 0, 3);
        assertThrows(IllegalArgumentException.class, () -> solver.solve(-1));
        assertThrows(IllegalArgumentException.class, () -> solver.countExpansions(-1));
        assertThrows(IllegalArgumentException.class, () -> solveSilentlyWithTrace(solver, -1));
        assertThrows(NullPointerException.class, () -> solver.solveWithTrace(1, null));
    }

    @Test
    void solvesMultipleProblemsInOneConsoleSession() {
        String output = runConsole("1\n5\n1\n1\n1\n2\n3\n1\n2\n16\n1\n1\n2\n0\n5\n1\n0\n");
        assertTrue(output.contains("T(5) = 41"));
        assertTrue(output.contains("T(16) = 21"));
        assertTrue(output.contains("[004] BASE   T(1) = 1"));
        assertTrue(output.contains("Theta(n^2)"));
        assertTrue(output.contains("Theta(log n)"));
        assertTrue(output.endsWith("Goodbye." + System.lineSeparator()));
    }

    @Test
    void retriesMalformedAndOutOfRangeConsoleValues() {
        String output =
                runConsole("hello\n4\n1\n-1\n9223372036854775808\n5\n1\n1\n0\n1\n2\n3\n9\n0\n0\n");
        assertTrue(output.contains("Invalid integer."));
        assertTrue(output.contains("Enter a value between 0 and 2."));
        assertTrue(output.contains("Enter a value between 1 and 9223372036854775807."));
        assertTrue(output.contains("T(5) = 41"));
        assertTrue(output.contains("S = 14"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "q", "1\nq\n", "2\n16\n", "1\n0\n0\n0\n1\n0\n0\n0"})
    void exitsCleanlyWhenInputEndsOrUserQuits(String input) {
        String output = runConsole(input);
        assertTrue(output.contains("Input ended. Goodbye."));
        assertFalse(output.contains("Exception"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void acceptsCommonLineEndings(String separator) {
        String input = String.join(separator, "1", "5", "1", "1", "1", "2", "3", "0", "0");
        assertTrue(runConsole(input).contains("T(5) = 41"));
    }

    @Test
    void rejectsExcessiveInputBeforeAccumulatingTheWholeLine() {
        String output = runConsole("9".repeat(81));
        assertTrue(output.contains("at most 80 characters"));
        assertFalse(output.contains("Exception"));
    }

    @Test
    void permitsMaximumLengthLineAndWhitespace() {
        String output = runConsole(" ".repeat(79) + "0\n");
        assertTrue(output.endsWith("Goodbye." + System.lineSeparator()));
        assertFalse(output.contains("Unable"));
    }

    @Test
    void reportsSignedSequencesWithoutCallingThemExecutionCosts() {
        String output = runConsole("1\n5\n0\n0\n1\n-1\n0\n0\n0\n");
        assertTrue(output.contains("T(5) = -15"));
        assertTrue(output.contains("no running-time growth is inferred"));
    }

    private static BigInteger solveSilentlyWithTrace(RecurrenceSolver solver, long inputSize) {
        return solver.solveWithTrace(
                inputSize, new PrintStream(java.io.OutputStream.nullOutputStream()));
    }

    private static String runConsole(String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        RecurrenceSolver.runConsole(
                new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8));
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
