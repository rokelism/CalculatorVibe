package lt.mif.miptcalculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class CalculatorEngine {

    companion object {
        val MATH_CONTEXT: MathContext = MathContext.DECIMAL64
        private const val MAX_DISPLAY_DIGITS = 16
    }

    var currentInput: String = "0"
        private set

    var accumulator: BigDecimal? = null
        private set

    var pendingOperator: Operator? = null
        private set

    var expressionText: String = ""
        private set

    var isError: Boolean = false
        private set

    var lastError: CalculatorError? = null
        private set

    var isNewInput: Boolean = true
        private set

    var isResultState: Boolean = false
        private set

    // For repeated equals operation (e.g. 5 + 2 = -> 7, = -> 9, = -> 11)
    private var lastOperator: Operator? = null
    private var lastOperand: BigDecimal? = null

    // Tracks whether pressing an operator can replace the pending operator (only immediately after an operator was pressed)
    private var canReplaceOperator: Boolean = false

    fun inputDigit(digit: Char) {
        if (!digit.isDigit()) return

        if (isError) {
            clearAll()
        }

        canReplaceOperator = false

        if (isResultState) {
            expressionText = ""
            lastOperator = null
            lastOperand = null
            accumulator = null
            currentInput = digit.toString()
            isNewInput = false
            isResultState = false
            return
        }

        if (isNewInput) {
            currentInput = digit.toString()
            isNewInput = false
            isResultState = false
        } else {
            if (currentInput == "0") {
                currentInput = digit.toString()
            } else {
                val digitCount = currentInput.count { it.isDigit() }
                if (digitCount < MAX_DISPLAY_DIGITS) {
                    currentInput += digit
                }
            }
        }
    }

    fun inputDecimalSeparator() {
        if (isError) {
            clearAll()
        }

        canReplaceOperator = false

        if (isResultState) {
            expressionText = ""
            lastOperator = null
            lastOperand = null
            accumulator = null
            currentInput = "0."
            isNewInput = false
            isResultState = false
            return
        }

        if (isNewInput) {
            currentInput = "0."
            isNewInput = false
            isResultState = false
        } else {
            if (!currentInput.contains('.')) {
                currentInput += "."
            }
        }
    }

    fun toggleSign() {
        if (isError) return

        val currentVal = parseCurrentInput()
        if (currentVal.compareTo(BigDecimal.ZERO) == 0 && !currentInput.contains('.')) {
            return
        }

        currentInput = if (currentInput.startsWith("-")) {
            currentInput.substring(1)
        } else {
            "-$currentInput"
        }
    }

    fun backspace() {
        if (isError) {
            clearAll()
            return
        }

        if (isNewInput || isResultState) {
            return
        }

        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            if (currentInput.isEmpty() || currentInput == "-") {
                currentInput = "0"
                isNewInput = true
            }
        }
    }

    fun clearEntry() {
        if (isResultState) {
            clearAll()
            return
        }
        currentInput = "0"
        isNewInput = true
        isError = false
        lastError = null
        if (pendingOperator != null) {
            canReplaceOperator = true
        }
    }

    fun clearAll() {
        currentInput = "0"
        accumulator = null
        pendingOperator = null
        expressionText = ""
        isError = false
        lastError = null
        isNewInput = true
        isResultState = false
        lastOperator = null
        lastOperand = null
        canReplaceOperator = false
    }

    fun setOperator(op: Operator) {
        if (isError) return

        // Entering new operator resets repeated equals state
        lastOperator = null
        lastOperand = null

        // If an operator was already selected and user immediately presses a different operator
        // without supplying any second operand
        if (canReplaceOperator && pendingOperator != null) {
            pendingOperator = op
            expressionText = "${formatNumber(accumulator!!)} ${op.symbol}"
            return
        }

        val currentVal = parseCurrentInput()

        if (pendingOperator == null) {
            // No previous operator pending: initialize binary operation with current value
            accumulator = currentVal
            pendingOperator = op
            expressionText = "${formatNumber(accumulator!!)} ${op.symbol}"
            isNewInput = true
            isResultState = false
            canReplaceOperator = true
        } else {
            // Immediate sequential execution (e.g. 2 + 3 * 4 -> computes 2 + 3 = 5 first)
            val result = executeBinary(accumulator!!, pendingOperator!!, currentVal)
            if (result == null) return

            accumulator = result
            currentInput = formatNumber(result)
            pendingOperator = op
            expressionText = "${formatNumber(accumulator!!)} ${op.symbol}"
            isNewInput = true
            isResultState = false
            canReplaceOperator = true
        }
    }

    fun calculateEquals(): HistoryItem? {
        if (isError) return null

        canReplaceOperator = false

        return when {
            // Case 1: Pending binary operation (e.g. 5 + 2 =)
            pendingOperator != null -> {
                val first = accumulator ?: parseCurrentInput()
                val second = parseCurrentInput()
                val op = pendingOperator!!

                val result = executeBinary(first, op, second) ?: return null

                val expr = if (expressionText.isNotEmpty() && !expressionText.endsWith("=") && expressionText != "${formatNumber(first)} ${op.symbol}") {
                    "$expressionText ="
                } else {
                    "${formatNumber(first)} ${op.symbol} ${formatNumber(second)} ="
                }

                expressionText = expr
                currentInput = formatNumber(result)
                accumulator = result
                pendingOperator = null

                // Save for repeated equals
                lastOperator = op
                lastOperand = second
                isNewInput = true
                isResultState = true

                HistoryItem(expr, formatNumber(result), result)
            }

            // Case 2: Repeated equals (e.g. after 5 + 2 = 7, pressing = again -> 7 + 2 = 9)
            lastOperator != null && lastOperand != null -> {
                val first = parseCurrentInput()
                val op = lastOperator!!
                val second = lastOperand!!

                val result = executeBinary(first, op, second) ?: return null

                val expr = "${formatNumber(first)} ${op.symbol} ${formatNumber(second)} ="
                expressionText = expr
                currentInput = formatNumber(result)
                accumulator = result
                isNewInput = true
                isResultState = true

                HistoryItem(expr, formatNumber(result), result)
            }

            // Case 3: No operation pending, just pressing =
            else -> {
                val current = parseCurrentInput()
                expressionText = "${formatNumber(current)} ="
                isNewInput = true
                isResultState = true
                null
            }
        }
    }

    fun applyPercent() {
        if (isError) return

        canReplaceOperator = false
        val currentVal = parseCurrentInput()
        val hundred = BigDecimal(100)

        when {
            // Case A: 200 + 10 % or 200 - 10 %
            // Percentage operand B becomes A * (B / 100)
            pendingOperator == Operator.ADD || pendingOperator == Operator.SUBTRACT -> {
                val base = accumulator ?: BigDecimal.ZERO
                val percentValue = base.multiply(currentVal, MATH_CONTEXT).divide(hundred, MATH_CONTEXT)
                currentInput = formatNumber(percentValue)
                expressionText = "${formatNumber(base)} ${pendingOperator!!.symbol} ${formatNumber(percentValue)}"
                isNewInput = true
                isResultState = false
            }

            // Case B: 200 * 10 % or 200 / 10 %
            // Percentage operand B becomes B / 100
            pendingOperator == Operator.MULTIPLY || pendingOperator == Operator.DIVIDE -> {
                val base = accumulator ?: BigDecimal.ONE
                val percentValue = currentVal.divide(hundred, MATH_CONTEXT)
                currentInput = formatNumber(percentValue)
                expressionText = "${formatNumber(base)} ${pendingOperator!!.symbol} ${formatNumber(percentValue)}"
                isNewInput = true
                isResultState = false
            }

            // Case C: Standalone 10 % -> 0.1
            else -> {
                val percentValue = currentVal.divide(hundred, MATH_CONTEXT)
                currentInput = formatNumber(percentValue)
                expressionText = formatNumber(percentValue)
                isNewInput = true
                isResultState = false
            }
        }
    }

    fun applyUnary(operation: UnaryOperation): HistoryItem? {
        if (isError) return null

        canReplaceOperator = false
        val currentVal = parseCurrentInput()
        val result: BigDecimal
        val formattedArg = formatNumber(currentVal)
        val unaryExprSnippet: String

        when (operation) {
            UnaryOperation.SQUARE -> {
                result = currentVal.multiply(currentVal, MATH_CONTEXT)
                unaryExprSnippet = "sqr($formattedArg)"
            }
            UnaryOperation.SQRT -> {
                if (currentVal.signum() < 0) {
                    setError(CalculatorError.INVALID_INPUT)
                    return null
                }
                result = computeSqrt(currentVal)
                unaryExprSnippet = "√($formattedArg)"
            }
            UnaryOperation.RECIPROCAL -> {
                if (currentVal.compareTo(BigDecimal.ZERO) == 0) {
                    setError(CalculatorError.DIVIDE_BY_ZERO)
                    return null
                }
                result = BigDecimal.ONE.divide(currentVal, MATH_CONTEXT)
                unaryExprSnippet = "1/($formattedArg)"
            }
        }

        currentInput = formatNumber(result)
        isNewInput = true

        return if (pendingOperator != null && accumulator != null) {
            expressionText = "${formatNumber(accumulator!!)} ${pendingOperator!!.symbol} $unaryExprSnippet"
            isResultState = false
            null
        } else {
            expressionText = unaryExprSnippet
            isResultState = true
            HistoryItem(unaryExprSnippet, formatNumber(result), result)
        }
    }

    fun loadFromHistory(item: HistoryItem) {
        currentInput = formatNumber(item.rawResult)
        isNewInput = true
        isResultState = true
        isError = false
        lastError = null
        canReplaceOperator = false

        if (pendingOperator != null && accumulator != null) {
            expressionText = "${formatNumber(accumulator!!)} ${pendingOperator!!.symbol} ${formatNumber(item.rawResult)}"
        } else {
            expressionText = item.expression
            accumulator = item.rawResult
            lastOperator = null
            lastOperand = null
        }
    }

    fun setDisplayValueDirect(value: BigDecimal) {
        currentInput = formatNumber(value)
        isNewInput = true
        isResultState = true
        isError = false
        lastError = null
        canReplaceOperator = false

        if (pendingOperator != null && accumulator != null) {
            expressionText = "${formatNumber(accumulator!!)} ${pendingOperator!!.symbol} ${formatNumber(value)}"
        }
    }

    fun getCurrentValue(): BigDecimal {
        return parseCurrentInput()
    }

    private fun executeBinary(first: BigDecimal, op: Operator, second: BigDecimal): BigDecimal? {
        return try {
            val res = when (op) {
                Operator.ADD -> first.add(second, MATH_CONTEXT)
                Operator.SUBTRACT -> first.subtract(second, MATH_CONTEXT)
                Operator.MULTIPLY -> first.multiply(second, MATH_CONTEXT)
                Operator.DIVIDE -> {
                    if (second.compareTo(BigDecimal.ZERO) == 0) {
                        setError(CalculatorError.DIVIDE_BY_ZERO)
                        return null
                    }
                    first.divide(second, MATH_CONTEXT)
                }
            }
            res
        } catch (e: ArithmeticException) {
            setError(CalculatorError.DIVIDE_BY_ZERO)
            null
        } catch (e: Exception) {
            setError(CalculatorError.OVERFLOW)
            null
        }
    }

    private fun setError(error: CalculatorError) {
        isError = true
        lastError = error
        isNewInput = true
        isResultState = false
        accumulator = null
        pendingOperator = null
        lastOperator = null
        lastOperand = null
        canReplaceOperator = false
    }

    private fun parseCurrentInput(): BigDecimal {
        return try {
            if (currentInput.isEmpty() || currentInput == "-" || currentInput == ".") {
                BigDecimal.ZERO
            } else {
                BigDecimal(currentInput)
            }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun computeSqrt(value: BigDecimal): BigDecimal {
        if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO

        val doubleVal = value.toDouble()
        if (!doubleVal.isInfinite() && !doubleVal.isNaN() && doubleVal > 0) {
            var x = BigDecimal(Math.sqrt(doubleVal), MATH_CONTEXT)
            val two = BigDecimal(2)
            for (i in 0 until 12) {
                x = x.add(value.divide(x, MATH_CONTEXT), MATH_CONTEXT).divide(two, MATH_CONTEXT)
            }
            return x
        }

        var x = value.divide(BigDecimal(2), MATH_CONTEXT)
        val two = BigDecimal(2)
        for (i in 0 until 40) {
            x = x.add(value.divide(x, MATH_CONTEXT), MATH_CONTEXT).divide(two, MATH_CONTEXT)
        }
        return x
    }

    fun formatNumber(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        // Check if scientific notation is needed for extremely large or tiny numbers
        val plain = stripped.toPlainString()
        if (plain == "0") return "0"
        if (plain.length > 20 || (stripped.abs() > BigDecimal("1E15")) || (stripped.abs() < BigDecimal("1E-9") && stripped.compareTo(BigDecimal.ZERO) != 0)) {
            return stripped.toString() // Scientific notation, e.g. 1.23E16
        }
        return plain
    }

    fun getFormattedDisplay(decimalSeparator: Char): String {
        return if (currentInput.contains('.')) {
            val parts = currentInput.split('.')
            val integerPart = parts[0]
            val fractionalPart = if (parts.size > 1) parts[1] else ""
            if (currentInput.endsWith('.')) {
                "$integerPart$decimalSeparator"
            } else {
                "$integerPart$decimalSeparator$fractionalPart"
            }
        } else {
            currentInput
        }
    }

    fun getFormattedExpression(decimalSeparator: Char): String {
        if (decimalSeparator == '.') return expressionText
        // Replace '.' in numbers with localized decimal separator
        val regex = Regex("""\b(\d+)\.(\d+)\b""")
        return regex.replace(expressionText) { matchResult ->
            "${matchResult.groupValues[1]}$decimalSeparator${matchResult.groupValues[2]}"
        }
    }

    fun restoreState(
        currentInput: String,
        accumulator: BigDecimal?,
        pendingOperator: Operator?,
        expressionText: String,
        isError: Boolean,
        lastError: CalculatorError?,
        isNewInput: Boolean,
        isResultState: Boolean,
        lastOperator: Operator?,
        lastOperand: BigDecimal?,
        canReplaceOperator: Boolean = false
    ) {
        this.currentInput = currentInput
        this.accumulator = accumulator
        this.pendingOperator = pendingOperator
        this.expressionText = expressionText
        this.isError = isError
        this.lastError = lastError
        this.isNewInput = isNewInput
        this.isResultState = isResultState
        this.lastOperator = lastOperator
        this.lastOperand = lastOperand
        this.canReplaceOperator = canReplaceOperator
    }
}
