package com.example.tasklock

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import com.example.tasklock.data.model.AppUsageEntity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate

class AppUsageChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val chart: PieChart = PieChart(context)

    // Dados para dialog
    private var entryMap = mutableMapOf<String, AppUsageEntity>()
    private var totalTime: Long = 0L

    init {
        addView(chart)
        chart.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        chart.setUsePercentValues(true)
        chart.description.isEnabled = false
        chart.isDrawHoleEnabled = true
        chart.setHoleColor(Color.TRANSPARENT)
        chart.transparentCircleRadius = 61f

        // Remove textos nas fatias (percentuais)
        chart.setDrawEntryLabels(false)

        // ✅ Legenda externa elegante
        chart.legend.isEnabled = true
        chart.legend.isWordWrapEnabled = true
        chart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        chart.legend.textSize = 12f

        // ✅ Listener de clique
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: Highlight?) {
                if (e is PieEntry) {
                    val label = e.label
                    val entry = entryMap[label]
                    if (entry != null) {
                        val timeFormatted = formatTime(entry.totalTimeMs)
                        val percentage = String.format("%.1f%%", entry.totalTimeMs.toFloat() / totalTime * 100f)

                        AlertDialog.Builder(context)
                            .setTitle(label)
                            .setMessage("Tempo de uso: $timeFormatted\nUso proporcional: $percentage")
                            .setPositiveButton("Ok", null)
                            .show()
                    } else if (label == "Outros") {
                        val outrosTime = calculateOutrosTime()
                        val timeFormatted = formatTime(outrosTime)
                        val percentage = String.format("%.1f%%", outrosTime.toFloat() / totalTime * 100f)

                        AlertDialog.Builder(context)
                            .setTitle("Outros")
                            .setMessage("Tempo de uso: $timeFormatted\nUso proporcional: $percentage")
                            .setPositiveButton("Ok", null)
                            .show()
                    }
                }
            }

            override fun onNothingSelected() {}
        })
    }

    fun setData(apps: List<AppUsageEntity>, mapFormatado: Map<String, Pair<String, Int>>) {
        totalTime = apps.sumOf { it.totalTimeMs }
        entryMap.clear()

        val entries = mutableListOf<PieEntry>()
        var outrosTime = 0L

        for (app in apps) {
            val formatted = mapFormatado[app.packageName]
            if (formatted != null) {
                val percentage = app.totalTimeMs.toFloat() / totalTime * 100f
                entries.add(PieEntry(percentage, formatted.first))
                entryMap[formatted.first] = app
            } else {
                outrosTime += app.totalTimeMs
            }
        }

        if (outrosTime > 0) {
            val percentage = outrosTime.toFloat() / totalTime * 100f
            entries.add(PieEntry(percentage, "Outros"))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 8f

        val data = PieData(dataSet)
        data.setDrawValues(false) // Remove valores das fatias
        chart.data = data
        chart.invalidate()
    }

    private fun calculateOutrosTime(): Long {
        val totalMapped = entryMap.values.sumOf { it.totalTimeMs }
        return totalTime - totalMapped
    }

    private fun formatTime(ms: Long): String {
        val sec = ms / 1000
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "${h}h ${m}m ${s}s"
    }
}
