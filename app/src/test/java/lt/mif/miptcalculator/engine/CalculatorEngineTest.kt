package lt.mif.miptcalculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CalculatorEngineTest {

    private lateinit var engine: CalculatorEngine

    @Before
    fun setUp() {
        engine = CalculatorEngine()
    }

    private fun inputNumber(numberStr: String) {
        for (ch in numberStr) {
            if (ch == '.') {
                engine.inputDecimalSeparator()
            } else if (ch.isDigit()) {
                engine.inputDigit(ch)
            }
        }
    }

    @Test
    fun testBasicAddition() {
        inputNumber("2")
        engine.setOperator(Operator.ADD)
        inputNumber("3")
        val item = engine.calculateEquals()

        assertEquals("5", engine.currentInput)
        assertEquals("2 + 3 =", engine.expressionText)
        assertNotNull(item)
        assertEquals("5", item?.result)
    }

    @Test
    fun testFloatingPointPrecision() {
        // 0.1 + 0.2 must be strictly 0.3 without 0.30000000000000004
        inputNumber("0.1")
        engine.setOperator(Operator.ADD)
        inputNumber("0.2")
        engine.calculateEquals()

        assertEquals("0.3", engine.currentInput)
    }

    @Test
    fun testSequentialExecutionWithoutOperatorPrecedence() {
        // Standard calculator: 2 + 3 * 4 = 20 (not 14)
        inputNumber("2")
        engine.setOperator(Operator.ADD)
        inputNumber("3")
        engine.setOperator(Operator.MULTIPLY) // Intermediate result 5
        assertEquals("5", engine.currentInput)

        inputNumber("4")
        engine.calculateEquals()
        assertEquals("20", engine.currentInput)
    }

    @Test
    fun testPercentageAddition() {
        // 200 + 10 % = 220
        inputNumber("200")
        engine.setOperator(Operator.ADD)
        inputNumber("10")
        engine.applyPercent()
        assertEquals("20", engine.currentInput)

        engine.calculateEquals()
        assertEquals("220", engine.currentInput)
    }

    @Test
    fun testPercentageSubtraction() {
        // 200 - 10 % = 180
        inputNumber("200")
        engine.setOperator(Operator.SUBTRACT)
        inputNumber("10")
        engine.applyPercent()
        assertEquals("20", engine.currentInput)

        engine.calculateEquals()
        assertEquals("180", engine.currentInput)
    }

    @Test
    fun testPercentageMultiplication() {
        // 200 * 10 % = 20
        inputNumber("200")
        engine.setOperator(Operator.MULTIPLY)
        inputNumber("10")
        engine.applyPercent()
        assertEquals("0.1", engine.currentInput)

        engine.calculateEquals()
        assertEquals("20", engine.currentInput)
    }

    @Test
    fun testPercentageDivision() {
        // 200 / 10 % = 2000
        inputNumber("200")
        engine.setOperator(Operator.DIVIDE)
        inputNumber("10")
        engine.applyPercent()
        assertEquals("0.1", engine.currentInput)

        engine.calculateEquals()
        assertEquals("2000", engine.currentInput)
    }

    @Test
    fun testStandalonePercentage() {
        // Standalone 10 % = 0.1
        inputNumber("10")
        engine.applyPercent()
        assertEquals("0.1", engine.currentInput)
    }

    @Test
    fun testReciprocal() {
        // 1 / 8 = 0.125
        inputNumber("8")
        val item = engine.applyUnary(UnaryOperation.RECIPROCAL)
        assertEquals("0.125", engine.currentInput)
        assertEquals("1/(8)", engine.expressionText)
        assertNotNull(item)
    }

    @Test
    fun testSquare() {
        // 5 ^ 2 = 25
        inputNumber("5")
        val item = engine.applyUnary(UnaryOperation.SQUARE)
        assertEquals("25", engine.currentInput)
        assertEquals("sqr(5)", engine.expressionText)
        assertNotNull(item)
    }

    @Test
    fun testSquareRoot() {
        // sqrt(9) = 3
        inputNumber("9")
        val item = engine.applyUnary(UnaryOperation.SQRT)
        assertEquals("3", engine.currentInput)
        assertEquals("√(9)", engine.expressionText)
        assertNotNull(item)
    }

    @Test
    fun testUnaryInsideBinaryOperation() {
        // 4 + 9 sqrt = 7
        inputNumber("4")
        engine.setOperator(Operator.ADD)
        inputNumber("9")
        engine.applyUnary(UnaryOperation.SQRT)
        assertEquals("3", engine.currentInput)
        assertEquals("4 + √(9)", engine.expressionText)

        engine.calculateEquals()
        assertEquals("7", engine.currentInput)
    }

    @Test
    fun testRepeatedEquals() {
        // 5 + 2 = -> 7; = -> 9; = -> 11
        inputNumber("5")
        engine.setOperator(Operator.ADD)
        inputNumber("2")
        engine.calculateEquals()
        assertEquals("7", engine.currentInput)

        engine.calculateEquals()
        assertEquals("9", engine.currentInput)

        engine.calculateEquals()
        assertEquals("11", engine.currentInput)
    }

    @Test
    fun testDivideByZero() {
        inputNumber("10")
        engine.setOperator(Operator.DIVIDE)
        inputNumber("0")
        val item = engine.calculateEquals()

        assertTrue(engine.isError)
        assertEquals(CalculatorError.DIVIDE_BY_ZERO, engine.lastError)
        assertNull(item) // Should not record error in history

        // Should recover cleanly on new input
        engine.inputDigit('5')
        assertFalse(engine.isError)
        assertEquals("5", engine.currentInput)
    }

    @Test
    fun testReciprocalZero() {
        inputNumber("0")
        val item = engine.applyUnary(UnaryOperation.RECIPROCAL)

        assertTrue(engine.isError)
        assertEquals(CalculatorError.DIVIDE_BY_ZERO, engine.lastError)
        assertNull(item)
    }

    @Test
    fun testNegativeSquareRoot() {
        inputNumber("9")
        engine.toggleSign()
        assertEquals("-9", engine.currentInput)

        val item = engine.applyUnary(UnaryOperation.SQRT)
        assertTrue(engine.isError)
        assertEquals(CalculatorError.INVALID_INPUT, engine.lastError)
        assertNull(item)
    }

    @Test
    fun testClearEntryVsClearAll() {
        inputNumber("50")
        engine.setOperator(Operator.ADD)
        inputNumber("30")
        engine.clearEntry()
        assertEquals("0", engine.currentInput)
        assertEquals(Operator.ADD, engine.pendingOperator)
        assertEquals(BigDecimal("50"), engine.accumulator)

        inputNumber("10")
        engine.calculateEquals()
        assertEquals("60", engine.currentInput)

        // Clear All
        engine.clearAll()
        assertEquals("0", engine.currentInput)
        assertNull(engine.accumulator)
        assertNull(engine.pendingOperator)
        assertEquals("", engine.expressionText)
    }

    @Test
    fun testBackspace() {
        inputNumber("123")
        engine.backspace()
        assertEquals("12", engine.currentInput)

        engine.backspace()
        assertEquals("1", engine.currentInput)

        engine.backspace()
        assertEquals("0", engine.currentInput)

        // Negative sign backspace
        inputNumber("7")
        engine.toggleSign()
        assertEquals("-7", engine.currentInput)
        engine.backspace()
        assertEquals("0", engine.currentInput)
    }

    @Test
    fun testToggleSign() {
        inputNumber("0")
        engine.toggleSign()
        assertEquals("0", engine.currentInput)

        inputNumber("42")
        engine.toggleSign()
        assertEquals("-42", engine.currentInput)
        engine.toggleSign()
        assertEquals("42", engine.currentInput)
    }

    @Test
    fun testLocalizedFormatting() {
        inputNumber("0.125")
        val ltFormatted = engine.getFormattedDisplay(',')
        val enFormatted = engine.getFormattedDisplay('.')

        assertEquals("0,125", ltFormatted)
        assertEquals("0.125", enFormatted)
    }

    @Test
    fun testChainingOperatorAfterEqualsDoesNotCrash() {
        // Regression test for Crash when pressing binary operator after '='
        // 2 + 3 = 5, then + 4 = 9, then * 2 = 18
        inputNumber("2")
        engine.setOperator(Operator.ADD)
        inputNumber("3")
        engine.calculateEquals()
        assertEquals("5", engine.currentInput)

        // Press '+' directly on result 5 without crash
        engine.setOperator(Operator.ADD)
        assertEquals("5 +", engine.expressionText)

        inputNumber("4")
        engine.calculateEquals()
        assertEquals("9", engine.currentInput)
        assertEquals("5 + 4 =", engine.expressionText)

        // Press '*' directly on result 9
        engine.setOperator(Operator.MULTIPLY)
        assertEquals("9 ×", engine.expressionText)

        inputNumber("2")
        engine.calculateEquals()
        assertEquals("18", engine.currentInput)
        assertEquals("9 × 2 =", engine.expressionText)
    }

    @Test
    fun testNewCalculationAfterEqualsWithoutClear() {
        // After 5 + 2 = 7, typing a new number 8 starts fresh calculation
        inputNumber("5")
        engine.setOperator(Operator.ADD)
        inputNumber("2")
        engine.calculateEquals()
        assertEquals("7", engine.currentInput)

        // Start typing new number 8
        inputNumber("8")
        assertEquals("8", engine.currentInput)
        assertEquals("", engine.expressionText)

        engine.setOperator(Operator.ADD)
        inputNumber("3")
        engine.calculateEquals()
        assertEquals("11", engine.currentInput)
        assertEquals("8 + 3 =", engine.expressionText)
    }

    @Test
    fun testRepeatedEqualsResetOnNewNumber() {
        // After 5 + 2 = 7, typing 9 and pressing = does not execute + 2
        inputNumber("5")
        engine.setOperator(Operator.ADD)
        inputNumber("2")
        engine.calculateEquals()
        assertEquals("7", engine.currentInput)

        inputNumber("9")
        engine.calculateEquals()
        assertEquals("9", engine.currentInput)
    }

    @Test
    fun testChainingOperatorAfterPercentage() {
        // Regression test: 200 + 10 % + 5 = 225
        // Previously, pressing '+' after '%' discarded the calculated percent operand (20)
        inputNumber("200")
        engine.setOperator(Operator.ADD)
        inputNumber("10")
        engine.applyPercent()
        assertEquals("20", engine.currentInput)
        assertEquals("200 + 20", engine.expressionText)

        // Continue with '+'
        engine.setOperator(Operator.ADD)
        assertEquals("220", engine.currentInput)
        assertEquals("220 +", engine.expressionText)

        inputNumber("5")
        engine.calculateEquals()
        assertEquals("225", engine.currentInput)
        assertEquals("220 + 5 =", engine.expressionText)
    }

    @Test
    fun testChainingOperatorAfterUnary() {
        // Regression test: 4 + 9 sqrt + 5 = 12
        // Previously, pressing '+' after sqrt discarded the sqrt result (3)
        inputNumber("4")
        engine.setOperator(Operator.ADD)
        inputNumber("9")
        engine.applyUnary(UnaryOperation.SQRT)
        assertEquals("3", engine.currentInput)
        assertEquals("4 + √(9)", engine.expressionText)

        // Continue with '+'
        engine.setOperator(Operator.ADD)
        assertEquals("7", engine.currentInput)
        assertEquals("7 +", engine.expressionText)

        inputNumber("5")
        engine.calculateEquals()
        assertEquals("12", engine.currentInput)
        assertEquals("7 + 5 =", engine.expressionText)
    }

    @Test
    fun testUnaryExpressionInEqualsHistory() {
        // 4 + 9 sqrt = 7 should preserve expression as '4 + √(9) ='
        inputNumber("4")
        engine.setOperator(Operator.ADD)
        inputNumber("9")
        engine.applyUnary(UnaryOperation.SQRT)
        val item = engine.calculateEquals()
        assertNotNull(item)
        assertEquals("4 + √(9) =", item?.expression)
        assertEquals("7", item?.result)
    }

    @Test
    fun testChainingOperatorAfterMemoryRecall() {
        // Regression test: 5 + [MR=10] + 2 = 17
        inputNumber("5")
        engine.setOperator(Operator.ADD)
        engine.setDisplayValueDirect(BigDecimal("10"))
        assertEquals("10", engine.currentInput)

        // Continue with '+'
        engine.setOperator(Operator.ADD)
        assertEquals("15", engine.currentInput)
        assertEquals("15 +", engine.expressionText)

        inputNumber("2")
        engine.calculateEquals()
        assertEquals("17", engine.currentInput)
        assertEquals("15 + 2 =", engine.expressionText)
    }

    @Test
    fun testOperatorReplacement() {
        // 5 + - * 3 = 15 (operators replaced without operand entered)
        inputNumber("5")
        engine.setOperator(Operator.ADD)
        assertEquals("5 +", engine.expressionText)

        engine.setOperator(Operator.SUBTRACT)
        assertEquals("5 −", engine.expressionText)

        engine.setOperator(Operator.MULTIPLY)
        assertEquals("5 ×", engine.expressionText)

        inputNumber("3")
        engine.calculateEquals()
        assertEquals("15", engine.currentInput)
    }
}
