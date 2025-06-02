package com.example.tasklock.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklock.AdicionarTarefaActivity
import com.example.tasklock.R
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.TarefaEntity
import com.example.tasklock.databinding.FragmentHomeBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tarefaAdapter: TarefaAdapter
    private var listaTarefas: MutableList<TarefaEntity> = mutableListOf()
    private var progressoJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<TextView>(R.id.toolbar_title)?.text = "Página Principal"

        adicionarTarefaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Recarregar as tarefas ao voltar
                carregarTarefas()
            }
        }

        configurarBotoesTipos()
        configurarBotoesFiltro()

        binding.btnLixeira.setOnClickListener {
            val selecionadas = tarefaAdapter.obterSelecionadas()
            if (selecionadas.isEmpty()) {
                Toast.makeText(requireContext(), "Nenhuma tarefa selecionada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Excluir tarefas")
                .setMessage("Tem certeza que deseja excluir ${selecionadas.size} tarefa(s)?")
                .setPositiveButton("Sim") { _, _ ->
                    excluirTarefasSelecionadas(selecionadas)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        tarefaAdapter = TarefaAdapter { tarefaAtualizada ->
            listaTarefas = listaTarefas.map {
                if (it.id == tarefaAtualizada.id) tarefaAtualizada else it
            }.toMutableList()
        }

        binding.recyclerTarefas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tarefaAdapter
        }

        carregarTarefas()
        atualizarProgressoDiasConsecutivos()
    }

    private fun excluirTarefasSelecionadas(tarefas: List<TarefaEntity>) {
        lifecycleScope.launch {
            val dao = AppUsageDatabase.getInstance(requireContext()).tarefaDao()
            withContext(Dispatchers.IO) {
                tarefas.forEach { dao.deletarTarefa(it) }
            }
            tarefaAdapter.limparSelecao()
            carregarTarefas()
        }
    }

    private fun configurarBotoesFiltro() {
        binding.btnFiltrarRealizadas.setOnClickListener {
            val filtradas = listaTarefas.filter { it.concluida }
            tarefaAdapter.atualizarLista(filtradas)
        }

        binding.btnFiltrarPendentes.setOnClickListener {
            val filtradas = listaTarefas.filter { !it.concluida }
            tarefaAdapter.atualizarLista(filtradas)
        }
    }

    private fun configurarBotoesTipos() {
        binding.btnTipoEstudos.setOnClickListener { abrirAdicionarTarefa("Estudos")}
        binding.btnTipoExercicio.setOnClickListener { abrirAdicionarTarefa("Exercício Físico") }
        binding.btnTipoTrabalho.setOnClickListener { abrirAdicionarTarefa("Trabalho") }
        binding.btnTipoEsporte.setOnClickListener { abrirAdicionarTarefa("Esporte") }
        binding.btnOutros.setOnClickListener { abrirAdicionarTarefa("Outras") }
    }

    private fun abrirAdicionarTarefa(tipo: String) {
        val intent = Intent(requireContext(), AdicionarTarefaActivity::class.java)
        intent.putExtra("tipoTarefaPredefinido", tipo)
        adicionarTarefaLauncher.launch(intent)
    }

    private lateinit var adicionarTarefaLauncher: ActivityResultLauncher<Intent>


    private fun carregarTarefas() {
        lifecycleScope.launch {
            val db = AppUsageDatabase.getInstance(requireContext())
            val tarefas = withContext(Dispatchers.IO) {
                db.tarefaDao().listarTarefasDireto()
            }
            listaTarefas = tarefas.toMutableList()
            tarefaAdapter.atualizarLista(tarefas)
        }
    }

    private fun atualizarProgressoDiasConsecutivos() {
        // Cancela job anterior se houver
        progressoJob?.cancel()
        progressoJob = viewLifecycleOwner.lifecycleScope.launch {
            val db = AppUsageDatabase.getInstance(requireContext())
            val tarefas = withContext(Dispatchers.IO) {
                db.tarefaDao().listarTarefasDireto()
            }

            val diasCompletos = tarefas
                .filter { it.concluida && (it.data != null || it.recorrente) }
                .mapNotNull { it.data ?: getHoje() }
                .toSet()

            val diasConsecutivos = contarDiasConsecutivos(diasCompletos)

            // Verifica se o binding ainda está disponível
            _binding?.txtDiasConsecutivos?.text = "$diasConsecutivos dias consecutivos"
        }
    }

    private fun contarDiasConsecutivos(dias: Set<String>): Int {
        if (dias.isEmpty()) return 0

        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoje = Calendar.getInstance()
        var count = 0

        while (true) {
            val dataStr = formato.format(hoje.time)
            if (dias.contains(dataStr)) {
                count++
                hoje.add(Calendar.DATE, -1)
            } else {
                break
            }
        }
        return count
    }

    private fun getHoje(): String {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formato.format(Date())
    }

    override fun onDestroyView() {
        progressoJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    fun forcarRecarregarLista() {
        lifecycleScope.launch {
            val db = AppUsageDatabase.getInstance(requireContext())
            val tarefas = withContext(Dispatchers.IO) {
                db.tarefaDao().listarTarefasDireto()
            }
            listaTarefas = tarefas.toMutableList()
            tarefaAdapter.atualizarLista(tarefas)
            atualizarProgressoDiasConsecutivos()
        }
    }
}
