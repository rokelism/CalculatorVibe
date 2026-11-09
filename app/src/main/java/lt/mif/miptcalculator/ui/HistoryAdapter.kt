package lt.mif.miptcalculator.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import lt.mif.miptcalculator.databinding.ItemHistoryBinding
import lt.mif.miptcalculator.engine.HistoryItem

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    var decimalSeparator: Char = ','

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            val formattedExpr = formatExpr(item.expression)
            val formattedRes = formatRes(item.result)

            binding.tvHistoryExpression.text = formattedExpr
            binding.tvHistoryResult.text = formattedRes

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun formatExpr(expr: String): String {
            if (decimalSeparator == '.') return expr
            val regex = Regex("""\b(\d+)\.(\d+)\b""")
            return regex.replace(expr) { matchResult ->
                "${matchResult.groupValues[1]}$decimalSeparator${matchResult.groupValues[2]}"
            }
        }

        private fun formatRes(res: String): String {
            return if (decimalSeparator != '.') {
                res.replace('.', decimalSeparator)
            } else {
                res
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                return oldItem.timestamp == newItem.timestamp
            }

            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
