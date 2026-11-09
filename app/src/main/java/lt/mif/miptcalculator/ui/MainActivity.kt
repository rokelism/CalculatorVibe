package lt.mif.miptcalculator.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import lt.mif.miptcalculator.R
import lt.mif.miptcalculator.databinding.ActivityMainBinding
import lt.mif.miptcalculator.engine.Operator
import lt.mif.miptcalculator.engine.UnaryOperation
import lt.mif.miptcalculator.viewmodel.CalculatorViewModel
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupLocaleAndFormatting()
        setupListeners()
        setupObservers()
        setupBackPressHandler()
    }

    private fun setupLocaleAndFormatting() {
        val currentLocale = resources.configuration.locales.get(0) ?: Locale.getDefault()
        val decimalSeparator = if (currentLocale.language.equals("lt", ignoreCase = true)) {
            ','
        } else if (currentLocale.language.equals("en", ignoreCase = true)) {
            '.'
        } else {
            DecimalFormatSymbols.getInstance(currentLocale).decimalSeparator
        }

        binding.btnDecimal.text = decimalSeparator.toString()
        viewModel.decimalSeparator = decimalSeparator
        historyAdapter.decimalSeparator = decimalSeparator
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { selectedItem ->
            viewModel.onSelectHistoryItem(selectedItem)
        }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        // Digit buttons
        binding.btn0.setOnClickListener { viewModel.onDigit('0') }
        binding.btn1.setOnClickListener { viewModel.onDigit('1') }
        binding.btn2.setOnClickListener { viewModel.onDigit('2') }
        binding.btn3.setOnClickListener { viewModel.onDigit('3') }
        binding.btn4.setOnClickListener { viewModel.onDigit('4') }
        binding.btn5.setOnClickListener { viewModel.onDigit('5') }
        binding.btn6.setOnClickListener { viewModel.onDigit('6') }
        binding.btn7.setOnClickListener { viewModel.onDigit('7') }
        binding.btn8.setOnClickListener { viewModel.onDigit('8') }
        binding.btn9.setOnClickListener { viewModel.onDigit('9') }

        // Decimal and sign
        binding.btnDecimal.setOnClickListener { viewModel.onDecimalSeparator() }
        binding.btnToggleSign.setOnClickListener { viewModel.onToggleSign() }

        // Binary operators
        binding.btnAdd.setOnClickListener { viewModel.onOperator(Operator.ADD) }
        binding.btnSubtract.setOnClickListener { viewModel.onOperator(Operator.SUBTRACT) }
        binding.btnMultiply.setOnClickListener { viewModel.onOperator(Operator.MULTIPLY) }
        binding.btnDivide.setOnClickListener { viewModel.onOperator(Operator.DIVIDE) }

        // Equals
        binding.btnEquals.setOnClickListener { viewModel.onEquals() }

        // Percent
        binding.btnPercent.setOnClickListener { viewModel.onPercent() }

        // Unary operations
        binding.btnSquare.setOnClickListener { viewModel.onUnary(UnaryOperation.SQUARE) }
        binding.btnSqrt.setOnClickListener { viewModel.onUnary(UnaryOperation.SQRT) }
        binding.btnReciprocal.setOnClickListener { viewModel.onUnary(UnaryOperation.RECIPROCAL) }

        // Clear and backspace
        binding.btnCE.setOnClickListener { viewModel.onClearEntry() }
        binding.btnC.setOnClickListener { viewModel.onClearAll() }
        binding.btnBackspace.setOnClickListener { viewModel.onBackspace() }

        // Memory controls
        binding.btnMC.setOnClickListener { viewModel.onMemoryClear() }
        binding.btnMR.setOnClickListener { viewModel.onMemoryRecall() }
        binding.btnMPlus.setOnClickListener { viewModel.onMemoryAdd() }
        binding.btnMMinus.setOnClickListener { viewModel.onMemorySubtract() }
        binding.btnMS.setOnClickListener { viewModel.onMemoryStore() }
        binding.btnMView.setOnClickListener { showMemoryPreview() }

        // History controls
        binding.btnHistoryToggle.setOnClickListener {
            viewModel.toggleHistory()
        }
        binding.btnCloseHistory.setOnClickListener {
            viewModel.toggleHistory(false)
        }
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    private fun setupObservers() {
        viewModel.displayText.observe(this) { text ->
            binding.tvDisplay.text = text
        }

        viewModel.expressionText.observe(this) { expr ->
            binding.tvExpression.text = expr
        }

        viewModel.isError.observe(this) { isErr ->
            if (isErr) {
                binding.tvDisplay.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
            } else {
                binding.tvDisplay.visibility = View.VISIBLE
                binding.tvError.visibility = View.GONE
            }
        }

        viewModel.errorMessageResId.observe(this) { resId ->
            if (resId != null) {
                binding.tvError.setText(resId)
            }
        }

        viewModel.hasMemory.observe(this) { hasMem ->
            binding.btnMC.isEnabled = hasMem
            binding.btnMR.isEnabled = hasMem
            val colorRes = if (hasMem) R.color.calc_mem_text_enabled else R.color.calc_mem_text_disabled
            val color = getColor(colorRes)
            binding.btnMC.setTextColor(color)
            binding.btnMR.setTextColor(color)
        }

        viewModel.historyList.observe(this) { list ->
            historyAdapter.submitList(list)
            binding.tvEmptyHistory.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscape) {
            viewModel.isHistoryOpen.observe(this) { isOpen ->
                binding.historyPanel.visibility = if (isOpen) View.VISIBLE else View.GONE
            }
        } else {
            binding.historyPanel.visibility = View.VISIBLE
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (!isLandscape && viewModel.isHistoryOpen.value == true) {
                    viewModel.toggleHistory(false)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showMemoryPreview() {
        val memoryStr = viewModel.memoryValue.value
        val message = if (memoryStr != null) {
            "${getString(R.string.memory_title)}: $memoryStr"
        } else {
            getString(R.string.no_memory)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.memory_view)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
