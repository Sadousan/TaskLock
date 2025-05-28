//package com.example.tasklock.ui.usodeapps
//
//import android.content.Context
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.*
//import androidx.appcompat.app.AlertDialog
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.tasklock.AppUsageChart
//import com.example.tasklock.data.db.AppUsageDatabase
//import com.example.tasklock.databinding.FragmentBloqueioappsBinding
//import com.example.tasklock.utils.AppInfoProvider.appInfoManual
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//
//class BloqueioAppsFragment : Fragment() {
//
//    private var _binding: FragmentBloqueioappsBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var viewModel: BloqueioAppsViewModel
//    private lateinit var chart: AppUsageChart
//
//    private val prefixosSistema = listOf(
//        "com.google.android.gms", "com.android.systemui", "com.android.phone", "com.android.settings",
//        "com.miui.", "com.samsung.", "com.sec.android", "com.motorola.",
//        "com.google.android.inputmethod", "com.xiaomi.", "com.oppo.", "com.coloros.",
//        "com.realme.", "com.vivo.", "com.huawei.", "android"
//    )
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentBloqueioappsBinding.inflate(inflater, container, false)
//        viewModel = ViewModelProvider(
//            this,
//            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
//        )[BloqueioAppsViewModel::class.java]
//
//        chart = binding.chart
//
//        binding.btnBlock.setOnClickListener { bloquearAppsSelecionados() }
//        binding.btnHelp.setOnClickListener { showHelpDialog() }
//
//        setupObservers()
//
//        return binding.root
//    }
//
//    override fun onResume() {
//        super.onResume()
//        checarEDefinirResetDiario()
//
//        GlobalScope.launch(Dispatchers.Main) {
//            val appsBloqueados = viewModel.getAppsBloqueados()
//            viewModel.carregarApps(appsBloqueados, prefixosSistema, appInfoManual)
//        }
//    }
//
//    private fun setupObservers() {
//        viewModel.apps.observe(viewLifecycleOwner) { apps ->
//            val container = binding.appListContainer
//            container.removeAllViews()
//
//            chart.setData(apps, appInfoManual)
//
//            apps.forEach { app ->
//                val view = layoutInflater.inflate(
//                    com.example.tasklock.R.layout.activity_item_app_block,
//                    container,
//                    false
//                )
//
//                view.tag = app.packageName
//
//                val manual = appInfoManual[app.packageName]
//                if (manual != null) {
//                    view.findViewById<ImageView>(com.example.tasklock.R.id.appIcon)
//                        .setImageResource(manual.second)
//                    view.findViewById<TextView>(com.example.tasklock.R.id.appName).text =
//                        "${manual.first} (${formatTime(app.totalTimeMs)})"
//                }
//
//                setupTimerButton(view, app.packageName, manual?.first ?: app.packageName)
//
//                container.addView(view)
//            }
//        }
//    }
//
//    private fun bloquearAppsSelecionados() {
//        val container = binding.appListContainer
//
//        val appsParaBloquear = mutableListOf<String>()
//        for (i in 0 until container.childCount) {
//            val itemView = container.getChildAt(i)
//            val checkBox = itemView.findViewById<CheckBox>(com.example.tasklock.R.id.appCheckBox)
//
//            val packageName = itemView.tag as String
//            if (checkBox.isChecked) {
//                appsParaBloquear.add(packageName)
//            }
//        }
//
//        if (appsParaBloquear.isEmpty()) {
//            Toast.makeText(requireContext(), "Nenhum app selecionado para bloqueio.", Toast.LENGTH_SHORT).show()
//        } else {
//            AlertDialog.Builder(requireContext())
//                .setTitle("Confirmar bloqueio")
//                .setMessage(
//                    "Você deseja bloquear os apps?\n\n" +
//                            appsParaBloquear.joinToString("\n- ", "- ") { pkg ->
//                                appInfoManual[pkg]?.first ?: pkg
//                            }
//                )
//                .setPositiveButton("Sim") { dialog, _ ->
//                    appsParaBloquear.forEach { packageName ->
//                        viewModel.inserirOuAtualizarBloqueio(
//                            packageName,
//                            getLimitFromPreferences(packageName)
//                        )
//                    }
//                    dialog.dismiss()
//                }
//                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
//                .show()
//        }
//    }
//
//    private fun setupTimerButton(view: View, pkg: String, name: String) {
//        val btn = view.findViewById<Button>(com.example.tasklock.R.id.appTimerButton)
//        val options = arrayOf("15 min", "30 min", "1 h", "2 h")
//
//        val label = requireContext().getSharedPreferences("AppTempos", Context.MODE_PRIVATE)
//            .getString(pkg, "30 min")
//        btn.text = label
//
//        btn.setOnClickListener {
//            AlertDialog.Builder(requireContext())
//                .setTitle("Definir tempo de uso para $name")
//                .setItems(options) { _, i ->
//                    val chosen = options[i]
//                    btn.text = chosen
//
//                    requireContext().getSharedPreferences("AppTempos", Context.MODE_PRIVATE)
//                        .edit().putString(pkg, chosen).apply()
//
//                    viewModel.inserirOuAtualizarBloqueio(
//                        pkg,
//                        getLimitFromPreferences(pkg)
//                    )
//                }
//                .show()
//        }
//    }
//
//    private fun showHelpDialog() {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Algo de errado? ✍(◔◡◔)")
//            .setMessage(
//                "O TaskLock começará a registrar seu uso de aplicativos.\n\n" +
//                        "Os dados se tornarão mais precisos ao longo do dia.\n\n" +
//                        "Retorne após alguns instantes, para acompanhar suas estatísticas!"
//            )
//            .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
//            .show()
//    }
//
//    private fun checarEDefinirResetDiario() {
//        val prefs = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
//        val ultimaData = prefs.getString("ultima_data", null)
//
//        val hoje = SimpleDateFormat("yyyyMMdd").format(System.currentTimeMillis())
//
//        if (ultimaData == null || ultimaData != hoje) {
//            GlobalScope.launch {
//                val dao = AppUsageDatabase.getInstance(requireContext()).appUsageDao()
//                dao.deleteAll()
//            }
//            prefs.edit().putString("ultima_data", hoje).apply()
//        }
//    }
//
//    private fun getLimitFromPreferences(pkg: String): Long {
//        val label = requireContext().getSharedPreferences("AppTempos", Context.MODE_PRIVATE)
//            .getString(pkg, "30 min")
//        return when (label) {
//            "15 min" -> 15 * 60 * 1000L
//            "30 min" -> 30 * 60 * 1000L
//            "1 h" -> 60 * 60 * 1000L
//            "2 h" -> 120 * 60 * 1000L
//            else -> 30 * 60 * 1000L
//        }
//    }
//
//    private fun formatTime(ms: Long): String {
//        val sec = ms / 1000
//        val h = sec / 3600
//        val m = (sec % 3600) / 60
//        val s = sec % 60
//        return "%dh %02dm %02ds".format(h, m, s)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
