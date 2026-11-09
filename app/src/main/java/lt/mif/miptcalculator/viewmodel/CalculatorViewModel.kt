package lt.mif.miptcalculator.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import lt.mif.miptcalculator.R
import lt.mif.miptcalculator.engine.CalculatorEngine
import lt.mif.miptcalculator.engine.CalculatorError
import lt.mif.miptcalculator.engine.HistoryItem
import lt.mif.miptcalculator.engine.MemoryManager
import lt.mif.miptcalculator.engine.Operator
import lt.mif.miptcalculator.engine.UnaryOperation
import java.math.BigDecimal

class CalculatorViewModel : ViewModel() {

    private val engine = CalculatorEngine()
    private val memoryManager = MemoryManager()

    var decimalSeparator: Char = ','
        set(value) {
            field = value
            updateUi()
        }

    private val _displayText = MutableLiveData<String>()
    val displayText: LiveData<String> = _displayText

    private val _expressionText = MutableLiveData<String>()
    val expressionText: LiveData<String> = _expressionText

    private val _isError = MutableLiveData<Boolean>()
    val isError: LiveData<Boolean> = _isError

    private val _errorMessageResId = MutableLiveData<Int?>()
    val errorMessageResId: LiveData<Int?> = _errorMessageResId

    private val _historyList = MutableLiveData<List<HistoryItem>>(emptyList())
    val historyList: LiveData<List<HistoryItem>> = _historyList

    private val _hasMemory = MutableLiveData<Boolean>(false)
    val hasMemory: LiveData<Boolean> = _hasMemory

    private val _memoryValue = MutableLiveData<String?>()
    val memoryValue: LiveData<String?> = _memoryValue

    private val _isHistoryOpen = MutableLiveData<Boolean>(false)
    val isHistoryOpen: LiveData<Boolean> = _isHistoryOpen

    init {
        updateUi()
    }

    private fun updateUi() {
        if (engine.isError) {
            _isError.value = true
            _displayText.value = ""
            _expressionText.value = ""
            _errorMessageResId.value = when (engine.lastError) {
                CalculatorError.DIVIDE_BY_ZERO -> R.string.error_divide_by_zero
                CalculatorError.INVALID_INPUT -> R.string.error_invalid_input
                CalculatorError.OVERFLOW, null -> R.string.error_overflow
            }
        } else {
            _isError.value = false
            _errorMessageResId.value = null
            _displayText.value = engine.getFormattedDisplay(decimalSeparator)
            _expressionText.value = engine.getFormattedExpression(decimalSeparator)
        }

        _hasMemory.value = memoryManager.hasMemory
        _memoryValue.value = memoryManager.memoryValue?.let {
            formatForDisplay(it)
        }
    }

    fun onDigit(digit: Char) {
        engine.inputDigit(digit)
        updateUi()
    }

    fun onDecimalSeparator() {
        engine.inputDecimalSeparator()
        updateUi()
    }

    fun onToggleSign() {
        engine.toggleSign()
        updateUi()
    }

    fun onBackspace() {
        engine.backspace()
        updateUi()
    }

    fun onClearEntry() {
        engine.clearEntry()
        updateUi()
    }

    fun onClearAll() {
        engine.clearAll()
        updateUi()
    }

    fun onOperator(op: Operator) {
        engine.setOperator(op)
        updateUi()
    }

    fun onEquals() {
        val historyItem = engine.calculateEquals()
        if (historyItem != null) {
            val updated = ArrayList(_historyList.value ?: emptyList())
            updated.add(0, historyItem)
            _historyList.value = updated
        }
        updateUi()
    }

    fun onPercent() {
        engine.applyPercent()
        updateUi()
    }

    fun onUnary(operation: UnaryOperation) {
        val historyItem = engine.applyUnary(operation)
        if (historyItem != null) {
            val updated = ArrayList(_historyList.value ?: emptyList())
            updated.add(0, historyItem)
            _historyList.value = updated
        }
        updateUi()
    }

    // Memory operations
    fun onMemoryClear() {
        memoryManager.clear()
        updateUi()
    }

    fun onMemoryRecall() {
        val mem = memoryManager.recall()
        if (mem != null) {
            engine.setDisplayValueDirect(mem)
            updateUi()
        }
    }

    fun onMemoryAdd() {
        if (!engine.isError) {
            memoryManager.add(engine.getCurrentValue())
            updateUi()
        }
    }

    fun onMemorySubtract() {
        if (!engine.isError) {
            memoryManager.subtract(engine.getCurrentValue())
            updateUi()
        }
    }

    fun onMemoryStore() {
        if (!engine.isError) {
            memoryManager.store(engine.getCurrentValue())
            updateUi()
        }
    }

    // History operations
    fun toggleHistory(open: Boolean? = null) {
        _isHistoryOpen.value = open ?: !(_isHistoryOpen.value ?: false)
    }

    fun onSelectHistoryItem(item: HistoryItem) {
        engine.loadFromHistory(item)
        _isHistoryOpen.value = false
        updateUi()
    }

    fun clearHistory() {
        _historyList.value = emptyList()
    }

    private fun formatForDisplay(value: BigDecimal): String {
        val str = engine.formatNumber(value)
        return if (decimalSeparator != '.') {
            str.replace('.', decimalSeparator)
        } else {
            str
        }
    }
}
