package lt.mif.miptcalculator.engine

import java.math.BigDecimal

enum class Operator(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷");

    companion object {
        fun fromSymbol(symbol: String): Operator? {
            return entries.firstOrNull { it.symbol == symbol || (it == SUBTRACT && symbol == "-") || (it == MULTIPLY && symbol == "*") || (it == DIVIDE && symbol == "/") }
        }
    }
}

enum class UnaryOperation {
    SQUARE,
    SQRT,
    RECIPROCAL
}

enum class CalculatorError {
    DIVIDE_BY_ZERO,
    INVALID_INPUT,
    OVERFLOW
}

data class HistoryItem(
    val expression: String,
    val result: String,
    val rawResult: BigDecimal,
    val timestamp: Long = System.currentTimeMillis()
)

data class EngineState(
    val displayValue: String,
    val expressionText: String,
    val isError: Boolean,
    val error: CalculatorError? = null
)
