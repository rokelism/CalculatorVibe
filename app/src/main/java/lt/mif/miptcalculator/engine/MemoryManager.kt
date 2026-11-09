package lt.mif.miptcalculator.engine

import java.math.BigDecimal
import java.math.MathContext

class MemoryManager {
    private val mathContext = MathContext.DECIMAL64
    var memoryValue: BigDecimal? = null
        private set

    val hasMemory: Boolean
        get() = memoryValue != null

    fun clear() {
        memoryValue = null
    }

    fun recall(): BigDecimal? {
        return memoryValue
    }

    fun store(value: BigDecimal) {
        memoryValue = value
    }

    fun add(value: BigDecimal) {
        memoryValue = if (memoryValue == null) {
            value
        } else {
            memoryValue!!.add(value, mathContext)
        }
    }

    fun subtract(value: BigDecimal) {
        memoryValue = if (memoryValue == null) {
            value.negate()
        } else {
            memoryValue!!.subtract(value, mathContext)
        }
    }

    fun restore(value: BigDecimal?) {
        memoryValue = value
    }
}
