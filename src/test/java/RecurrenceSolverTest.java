import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecurrenceSolverTest {
    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "T(n-1)+n | 1 | 1",
                "T(n-1)+n | 2 | 3",
                "T(n-1)+n | 5 | 15",
                "T(n-1)+n | 10 | 55",
                "T(n-1)+3 | 1 | 1",
                "T(n-1)+3 | 2 | 4",
                "T(n-1)+3 | 5 | 13",
                "T(n-1)+3 | 10 | 28",
                "2*T(n/2)+n | 1 | 1",
                "2*T(n/2)+n | 2 | 4",
                "2*T(n/2)+n | 4 | 12",
                "2*T(n/2)+n | 8 | 32",
                "2*T(n/2)+n | 16 | 80",
                "4*T(n/2)+n | 1 | 1",
                "4*T(n/2)+n | 2 | 6",
                "4*T(n/2)+n | 4 | 28",
                "4*T(n/2)+n | 8 | 120",
                "4*T(n/2)+n | 16 | 496",
                "2*T(n/2)+3*n | 1 | 1",
                "2*T(n/2)+3*n | 2 | 8",
                "2*T(n/2)+3*n | 4 | 28",
                "2*T(n/2)+3*n | 8 | 80",
                "4*T(n/2)+2*n | 1 | 1",
                "4*T(n/2)+2*n | 2 | 8",
                "4*T(n/2)+2*n | 4 | 40",
                "4*T(n/2)+2*n | 8 | 176",
                "3*T(n/3)+n^2 | 3 | 12",
                "3*T(n/3)+n^2 | 9 | 117",
                "3*T(n/3)+2*n^2 | 1 | 1",
                "3*T(n/3)+2*n^2 | 3 | 21",
                "3*T(n/3)+2*n^2 | 9 | 225",
                "3*T(n/3)+2*n^2 | 27 | 2133",
                "2*T(n/4)+1 | 4 | 3",
                "2*T(n/4)+1 | 16 | 7",
                "2*T(n/4)+3 | 1 | 1",
                "2*T(n/4)+3 | 4 | 5",
                "2*T(n/4)+3 | 16 | 13",
                "2*T(n/4)+3 | 64 | 29",
                "8*T(n/2)+1000*n^2 | 1 | 1",
                "8*T(n/2)+1000*n^2 | 2 | 4008",
                "8*T(n/2)+1000*n^2 | 4 | 48064",
                "8*T(n/2)+1000*n^2 | 8 | 448512",
                "9*T(n/3)+n | 1 | 1",
                "9*T(n/3)+n | 3 | 12",
                "9*T(n/3)+n | 9 | 117",
                "9*T(n/3)+n | 27 | 1080",
                "4*T(n/2)+n*n | 2 | 8",
                "4*T(n/2)+n*n | 4 | 48",
                "4*T(n/2)+n*n | 8 | 256",
                "16*T(n/4)+5*n+10 | 1 | 1",
                "16*T(n/4)+5*n+10 | 4 | 46",
                "16*T(n/4)+5*n+10 | 16 | 826",
                "16*T(n/4)+5*n+10 | 64 | 13546",
                "T(n-1)+2*n+3 | 5 | 41",
                "T(n/2)+5 | 16 | 21",
                "T(n/2)+5 | 15 | 16"
            })
    void evaluatesSubtractionAndDivideAndConquerSequences(
            String expression, long inputSize, String expected) {
        RecurrenceSolver solver = new RecurrenceSolver(expression, 1, "1");
        assertValue(expected, solver.solve(inputSize));
        assertValue(expected, solveSilentlyWithTrace(solver, inputSize));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "2*T(n/2)+8*T(n/4)+2 | 2 | 1 | 4 | 12",
                "2*T(n/2)+8*T(n/4)+2 | 2 | 1 | 8 | 34",
                "T(n-1)+T(n/2)+n | 1 | 1 | 2 | 4",
                "T(n-1)+T(n/2)+n | 1 | 1 | 4 | 16",
                "T(n-1)+T(n/2)+n | 1 | 1 | 8 | 78",
                "T(floor((n-1)/2))+T(ceil((n-1)/2))+1 | 0 | 0 | 8 | 8",
                "T(floor((n-1)/2))+T(ceil((n-1)/2))+1 | 0 | 0 | 9 | 9",
                "T(n-1)+T(n-2) | 1 | n | 0 | 0",
                "T(n-1)+T(n-2) | 1 | n | 1 | 1",
                "T(n-1)+T(n-2) | 1 | n | 10 | 55",
                "T(n-1)+T(n-2) | 1 | n | 50 | 12586269025",
                "2*T(n-1)+1 | 0 | 0 | 10 | 1023",
                "if(n%2==0,T(n/2)+1,T(n-1)+1) | 0 | 0 | 13 | 6",
                "if(n%2==0,T(n/2)+1,T(n-1)+1) | 0 | 0 | 16 | 5",
                "n*T(n-1) | 0 | 1 | 10 | 3628800",
                "T(n-1)^2+1 | 0 | 1 | 3 | 26",
                "T(n-1)/2+0.25 | 0 | 1 | 3 | 0.5625",
                "T(n-1)-2*n+3 | 1 | -1 | 5 | -17"
            })
    void evaluatesMixedRecursiveTermsVariableBasesAndNonlinearCombinations(
            String expression,
            long baseThreshold,
            String baseExpression,
            long inputSize,
            String expected) {
        RecurrenceSolver solver = new RecurrenceSolver(expression, baseThreshold, baseExpression);
        assertValue(expected, solver.solve(inputSize));
        assertValue(expected, solveSilentlyWithTrace(solver, inputSize));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "2+3*4 | 14",
                "(2+3)*4 | 20",
                "20/2/5 | 2",
                "20-3-2 | 15",
                "2^3^2 | 512",
                "-2^2 | -4",
                "(-2)^2 | 4",
                "2^-3 | 0.125",
                "+2+-3 | -1",
                "10%3 | 1",
                "0.1+0.2 | 0.3",
                "(1/3)*3 | 1",
                "floor((1/3)*3) | 1",
                "floor(-7/3) | -3",
                "ceil(-7/3) | -2",
                "ceil(1/3) | 1",
                "abs(-7/3)*3 | 7",
                "min(7/3,2) | 2",
                "max(7/3,2)*3 | 7",
                "if(2<3,7,9) | 7",
                "if(2<=2,7,9) | 7",
                "if(3>2,7,9) | 7",
                "if(3>=3,7,9) | 7",
                "if(3==3,7,9) | 7",
                "if(3!=3,7,9) | 9",
                "if(3<2,7,9) | 9",
                "if(1/3*3==1,7,9) | 7",
                "if(2==2,if(3>2,7,9),11) | 7"
            })
    void followsArithmeticPrecedenceExactFractionsAndComparisonRules(
            String expression, String expected) {
        assertValue(expected, new RecurrenceSolver(expression, 0, "0").solve(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10, 31})
    void matchesMaximumSearchOperationCounts(int inputSize) {
        long operations = 2;
        int maximum = 0;
        for (int index = 1; index < inputSize; index++) {
            operations++;
            operations++;
            if (index > maximum) {
                maximum = index;
                operations++;
            }
            operations++;
        }
        operations++;
        assertValue(
                Long.toString(operations),
                new RecurrenceSolver("T(n-1)+4", 1, "3").solve(inputSize));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 31})
    void matchesMatrixElementVisitCounts(int inputSize) {
        long visitedElements = 0;
        for (int row = 0; row < inputSize; row++) {
            for (int column = 0; column < inputSize; column++) {
                visitedElements++;
            }
        }
        assertValue(
                Long.toString(visitedElements),
                new RecurrenceSolver("T(n-1)+2*n-1", 0, "0").solve(inputSize));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 31})
    void matchesRepeatedMenuAndPrintingOperationCounts(int inputSize) {
        int attempts = 2;
        long operations = 0;
        for (int attempt = 0; attempt < attempts; attempt++) {
            operations += 3;
        }
        operations++;
        for (int index = 0; index < inputSize; index++) {
            operations++;
            operations++;
            operations++;
        }
        operations++;
        operations++;
        assertValue(
                Long.toString(operations),
                new RecurrenceSolver("T(n-1)+3", 0, "9").solve(inputSize));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 5, 10, 31})
    void matchesSelectionSortComparisonCounts(int inputSize) {
        int[] values = new int[inputSize];
        for (int index = 0; index < inputSize; index++) {
            values[index] = inputSize - index;
        }
        long comparisons = 0;
        for (int index = 0; index < values.length - 1; index++) {
            int smallest = index;
            for (int candidate = index + 1; candidate < values.length; candidate++) {
                comparisons++;
                if (values[candidate] < values[smallest]) {
                    smallest = candidate;
                }
            }
            int previous = values[index];
            values[index] = values[smallest];
            values[smallest] = previous;
        }
        assertValue(
                Long.toString(comparisons),
                new RecurrenceSolver("T(n-1)+n-1", 1, "0").solve(inputSize));
    }

    @Test
    void roundsOnlyTheFinalFractionToDecimal128() {
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+1/3", 0, "0");
        assertEquals(
                0,
                BigDecimal.ONE
                        .divide(BigDecimal.valueOf(3), MathContext.DECIMAL128)
                        .compareTo(solver.solve(1)));
        assertValue("1", solver.solve(3));
        assertValue("10", solver.solve(30));
    }

    @Test
    void floorsRecursiveArgumentsAfterExactFractionalArithmetic() {
        RecurrenceSolver solver = new RecurrenceSolver("T(n/3*3-1)+1", 0, "0");
        assertValue("10", solver.solve(10));
        assertValue("1", new RecurrenceSolver("T(n/2)", 2, "n").solve(3));
        assertValue("2", new RecurrenceSolver("T(ceil(n/2))", 2, "n").solve(3));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 4, 5})
    void evaluatesBaseExpressionWithoutEvaluatingUnusedRecursiveExpression(long inputSize) {
        RecurrenceSolver solver = new RecurrenceSolver("T(n)+1/0", 5, "n^2-7");
        assertValue(Long.toString(inputSize * inputSize - 7), solver.solve(inputSize));
    }

    @Test
    void permitsMaximumBaseThresholdAndInputWithoutOverflow() {
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+1", Long.MAX_VALUE, "n*n");
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).pow(2);
        assertValue(expected.toString(), solver.solve(Long.MAX_VALUE));
    }

    @Test
    void halvesMaximumLongUsingExactIntegerArguments() {
        RecurrenceSolver solver = new RecurrenceSolver("T(n/2)+1", 0, "0");
        assertValue("63", solver.solve(Long.MAX_VALUE));
        assertValue("63", solveSilentlyWithTrace(solver, Long.MAX_VALUE));
    }

    @Test
    void preservesLargeIntegerConstantsAndIntermediateProducts() {
        String coefficient = "922337203685477580812345678901234567890";
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+" + coefficient + "*n", 0, "0");
        BigInteger expected = new BigInteger(coefficient).multiply(BigInteger.valueOf(6));
        assertValue(expected.toString(), solver.solve(3));
        assertValue(
                BigInteger.TWO.pow(100).subtract(BigInteger.ONE).toString(),
                new RecurrenceSolver("2*T(n-1)+1", 0, "0").solve(100));
    }

    @Test
    void evaluatesDeepSubtractionChainsWithoutOverflowingTheJavaStack() {
        long inputSize = 10000;
        BigInteger expected =
                BigInteger.valueOf(inputSize)
                        .multiply(BigInteger.valueOf(inputSize + 1))
                        .divide(BigInteger.TWO);
        assertValue(expected.toString(), new RecurrenceSolver("T(n-1)+n", 0, "0").solve(inputSize));
    }

    @Test
    void computesLargeDivideAndConquerResultsUsingIndependentClosedForms() {
        long inputSize = 1L << 40;
        assertValue(
                BigInteger.valueOf(inputSize).multiply(BigInteger.valueOf(41)).toString(),
                new RecurrenceSolver("2*T(n/2)+n", 1, "1").solve(inputSize));
        BigInteger input = BigInteger.valueOf(inputSize);
        assertValue(
                input.pow(2).multiply(BigInteger.TWO).subtract(input).toString(),
                new RecurrenceSolver("4*T(n/2)+n", 1, "1").solve(inputSize));
    }

    @Test
    void matchesBottomUpEvaluationForTwoThousandGeneratedMixedRecurrences() {
        Random random = new Random(20260923);
        for (int scenario = 0; scenario < 2000; scenario++) {
            int inputSize = 2 + random.nextInt(70);
            int firstCoefficient = 1 + random.nextInt(3);
            int secondCoefficient = random.nextInt(4);
            int divisor = 2 + random.nextInt(5);
            int linearCoefficient = random.nextInt(11) - 5;
            int constantTerm = random.nextInt(11) - 5;
            int baseValue = random.nextInt(11) - 5;
            BigInteger[] values = new BigInteger[inputSize + 1];
            values[0] = BigInteger.valueOf(baseValue);
            values[1] = BigInteger.valueOf(baseValue);
            for (int index = 2; index <= inputSize; index++) {
                values[index] =
                        values[index - 1]
                                .multiply(BigInteger.valueOf(firstCoefficient))
                                .add(
                                        values[index / divisor].multiply(
                                                BigInteger.valueOf(secondCoefficient)))
                                .add(
                                        BigInteger.valueOf(
                                                (long) linearCoefficient * index + constantTerm));
            }
            String expression =
                    firstCoefficient
                            + "*T(n-1)+"
                            + secondCoefficient
                            + "*T(n/"
                            + divisor
                            + ")+ ("
                            + linearCoefficient
                            + ")*n+("
                            + constantTerm
                            + ")";
            RecurrenceSolver solver =
                    new RecurrenceSolver(expression, 1, Integer.toString(baseValue));
            assertEquals(
                    0,
                    new BigDecimal(values[inputSize]).compareTo(solver.solve(inputSize)),
                    "Scenario " + scenario + ": " + expression);
            if (scenario % 100 == 0) {
                assertValue(
                        values[inputSize].toString(), solveSilentlyWithTrace(solver, inputSize));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "if(n>0,7,1/0)",
                "if(n>0,7,T(n))",
                "if(n<0,T(-1),7)",
                "if(0,1/0,7)",
                "if(-2,7,T(n))"
            })
    void evaluatesOnlyTheSelectedConditionalBranch(String expression) {
        assertValue("7", new RecurrenceSolver(expression, 0, "0").solve(2));
    }

    @Test
    void acceptsFullEquationsAndWhitespace() {
        RecurrenceSolver solver = new RecurrenceSolver("  T(n) = 2 * T(n / 2) + n  ", 1, " 1 ");
        assertValue("32", solver.solve(8));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                " ",
                "T(n-1)+",
                "T(n-1",
                "T()",
                "T(n,n)",
                "unknown(n)",
                "x+1",
                "2**3",
                "2..3",
                "T(n-1) garbage",
                "floor()",
                "ceil(1,2)",
                "abs()",
                "min(1)",
                "max(1,2,3)",
                "if(n>1,2)",
                "if(n>1,2,3,4)",
                "if(n>1,,3)",
                "T(x)=T(n-1)",
                "n=2",
                "T(n)=",
                "T(T(n-1))",
                "if(T(n-1)>0,1,2)",
                "2;System.exit(0)"
            })
    void rejectsMalformedOrUnsupportedExpressions(String expression) {
        assertThrows(
                IllegalArgumentException.class, () -> new RecurrenceSolver(expression, 1, "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"T(n)", "n+T(0)", "if(n==0,0,T(n-1))"})
    void rejectsRecursiveCallsInTheBaseExpression(String baseExpression) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecurrenceSolver("T(n-1)+1", 1, baseExpression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"T(n)+1", "T(n+1)", "T(2*n)", "T(ceil(n/1))", "T(-1)", "T(n-5)"})
    void rejectsCallsThatDoNotReduceToAnAllowedInput(String expression) {
        RecurrenceSolver solver = new RecurrenceSolver(expression, 0, "0");
        assertThrows(IllegalArgumentException.class, () -> solver.solve(2));
        assertThrows(IllegalArgumentException.class, () -> solveSilentlyWithTrace(solver, 2));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1/0",
                "1%0",
                "2^0.5",
                "0^-1",
                "1.5%1",
                "2^10001",
                "2^-10001",
                "T(9223372036854775808)"
            })
    void rejectsUndefinedArithmeticAndUnrepresentableArguments(String expression) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecurrenceSolver(expression, 0, "0").solve(1));
    }

    @Test
    void rejectsNegativeThresholdAndRequestedInput() {
        assertThrows(IllegalArgumentException.class, () -> new RecurrenceSolver("T(n-1)", -1, "0"));
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+1", 0, "0");
        assertThrows(IllegalArgumentException.class, () -> solver.solve(-1));
        assertThrows(IllegalArgumentException.class, () -> solveSilentlyWithTrace(solver, -1));
        assertThrows(NullPointerException.class, () -> solver.solveWithTrace(1, null));
    }

    @Test
    void permitsRepeatedIndependentEvaluationsOnTheSameSolver() {
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+T(n-2)", 1, "n");
        assertValue("55", solver.solve(10));
        assertValue("5", solver.solve(5));
        assertValue("55", solveSilentlyWithTrace(solver, 10));
        assertValue("0", solver.solve(0));
    }

    @Test
    void rejectsExcessiveExpressionsAndExpressionNesting() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RecurrenceSolver(
                                "1".repeat(RecurrenceSolver.MAX_EXPRESSION_LENGTH + 1), 0, "0"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecurrenceSolver("(".repeat(100) + "1" + ")".repeat(100), 0, "0"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RecurrenceSolver(
                                "1", 0, "1".repeat(RecurrenceSolver.MAX_EXPRESSION_LENGTH + 1)));
    }

    @Test
    void rejectsExcessiveNumbersAndTooManyDistinctInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecurrenceSolver("(2^10000)^4", 0, "0").solve(1));
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+1", 0, "0");
        assertThrows(
                IllegalArgumentException.class,
                () -> solver.solve((long) RecurrenceSolver.MAX_EVALUATED_STATES + 1));
        assertValue("5", solver.solve(5));
    }

    @Test
    void boundsTraceOutputWhileCompletingDeepRecurrences() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BigDecimal result =
                new RecurrenceSolver("T(n-1)+1", 0, "0")
                        .solveWithTrace(
                                10000, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        assertValue("10000", result);
        String trace = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(trace.contains("calculation continues"));
        assertTrue(trace.lines().count() <= RecurrenceSolver.MAX_TRACE_EVENTS + 3L);
    }

    @Test
    void reportsRoundedFinalFractionsWithoutRoundingIntermediateValues() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+1/3", 0, "0");
        solver.solveWithTrace(1, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("rounded"));
        bytes.reset();
        assertValue(
                "1",
                solver.solveWithTrace(3, new PrintStream(bytes, true, StandardCharsets.UTF_8)));
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("rounded"));
    }

    @Test
    void tracesBaseCasesCompletedResultsAndReuseUsingPlainKeyboardCharacters() {
        RecurrenceSolver solver = new RecurrenceSolver("T(n-1)+T(n-2)", 1, "n");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertValue(
                "5",
                solver.solveWithTrace(5, new PrintStream(bytes, true, StandardCharsets.UTF_8)));
        String trace = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(trace.contains("T(0)"));
        assertTrue(trace.contains("T(1)"));
        assertTrue(trace.contains("T(5)"));
        assertTrue(trace.toLowerCase().contains("reus"));
        assertTrue(trace.indexOf("T(1)") < trace.lastIndexOf("T(5)"));
        assertTrue(trace.chars().allMatch(character -> character < 128));
        assertFalse(trace.contains("==="));
        assertFalse(trace.contains("---"));
    }

    @Test
    void solvesMultipleExpressionsInOneConsoleSession() {
        String output = runConsole("2*T(n/2)+n\n8\n1\n1\n1\nT(n-1)+2*n+3\n5\n1\n1\n0\nq\n");
        assertTrue(output.contains("T(8) = 32"));
        assertTrue(output.contains("T(5) = 41"));
        assertTrue(output.chars().allMatch(character -> character < 128));
        assertFalse(output.contains("==="));
        assertFalse(output.contains("---"));
        assertFalse(output.contains("Exception"));
    }

    @Test
    void retriesInvalidNumericFieldsAndTraceChoices() {
        String output =
                runConsole("2*T(n/2)+n\nabc\n-1\n9223372036854775808\n8\n-1\n1\n1\n2\n0\nq\n");
        assertTrue(output.contains("T(8) = 32"));
        assertTrue(output.contains("nonnegative integer"));
        assertTrue(output.contains("Enter 1"));
        assertFalse(output.contains("Exception"));
    }

    @Test
    void recoversFromMalformedExpressionsAndNonterminatingCalls() {
        String output =
                runConsole(
                        "T(n-1)+\n5\n1\n1\n0\nT(n)+1\n5\n1\n1\n0\nT(n-1)+2*n+3\n5\n1\n1\n0\nq\n");
        assertEquals(2, output.lines().filter(line -> line.contains("Cannot calculate:")).count());
        assertTrue(output.contains("T(5) = 41"));
        assertFalse(output.contains("Exception"));
    }

    @Test
    void recoversAfterAnOversizedConsoleLine() {
        String output =
                runConsole(
                        "1".repeat(RecurrenceSolver.MAX_EXPRESSION_LENGTH + 1)
                                + "\nT(n-1)+2*n+3\n5\n1\n1\n0\nq\n");
        assertTrue(output.contains("too long"));
        assertTrue(output.contains("T(5) = 41"));
        assertFalse(output.contains("Exception"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "q\n",
                "T(n-1)+1\nq\n",
                "T(n-1)+1\n5\nq\n",
                "T(n-1)+1\n5\n1\nq\n",
                "T(n-1)+1\n5\n1\n1\nq\n"
            })
    void permitsQuittingAtEveryConsolePrompt(String input) {
        String output = runConsole(input);
        assertTrue(output.endsWith("Goodbye." + System.lineSeparator()));
        assertFalse(output.contains("Exception"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "q\n",
                "T(n-1)+1\n",
                "T(n-1)+1\n5\n",
                "T(n-1)+1\n5\n1\n",
                "T(n-1)+1\n5\n1\n1\n"
            })
    void exitsCleanlyWhenInputEnds(String input) {
        String output = runConsole(input);
        assertFalse(output.contains("Exception"));
        assertFalse(output.contains("at RecurrenceSolver"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void acceptsCommonLineEndings(String separator) {
        String input = String.join(separator, "T(n-1)+2*n+3", "5", "1", "1", "0", "q", "");
        assertTrue(runConsole(input).contains("T(5) = 41"));
    }

    private static void assertValue(String expected, BigDecimal actual) {
        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal solveSilentlyWithTrace(RecurrenceSolver solver, long inputSize) {
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
