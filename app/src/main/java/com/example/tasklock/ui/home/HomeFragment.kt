package com.example.tasklock.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasklock.AdicionarTarefaActivity
import com.example.tasklock.R
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.TarefaEntity
import com.example.tasklock.data.model.UserPreferences
import com.example.tasklock.databinding.FragmentHomeBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.firstOrNull

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tarefaAdapter: TarefaAdapter
    private var listaTarefas: MutableList<TarefaEntity> = mutableListOf()
    private var progressoJob: Job? = null
    private lateinit var adicionarTarefaLauncher: ActivityResultLauncher<Intent>
    private var emailUsuario: String = ""

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

        val prefs = UserPreferences(requireContext())
        emailUsuario = prefs.getEmailUsuarioLogado() ?: ""

        if (emailUsuario.isEmpty()) {
            Toast.makeText(requireContext(), "Erro: usuário não autenticado", Toast.LENGTH_LONG).show()
            return
        }

        atualizarNomeUsuario(view)
        requireActivity().findViewById<TextView>(R.id.toolbar_title)?.text = "Página Principal"

        adicionarTarefaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                carregarTarefas()
                atualizarProgressoDiasConsecutivos() // Atualiza progresso após adicionar
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
            atualizarProgressoDiasConsecutivos() // Atualiza progresso ao marcar tarefa
        }

        binding.recyclerTarefas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tarefaAdapter
        }

        carregarTarefas()
        atualizarProgressoDiasConsecutivos()
    }

    private fun atualizarNomeUsuario(view: View) {
        lifecycleScope.launch {
            val usuario = withContext(Dispatchers.IO) {
                AppUsageDatabase.getInstance(requireContext()).usuarioDao().buscarPorEmail(emailUsuario)
            }
            view.findViewById<TextView?>(R.id.txtNomeUsuario)?.text = usuario?.nome ?: "Usuário"
        }
    }

    private fun excluirTarefasSelecionadas(tarefas: List<TarefaEntity>) {
        lifecycleScope.launch {
            val dao = AppUsageDatabase.getInstance(requireContext()).tarefaDao()
            withContext(Dispatchers.IO) {
                tarefas.forEach { dao.deletarTarefa(it) }
            }
            tarefaAdapter.limparSelecao()
            carregarTarefas()
            atualizarProgressoDiasConsecutivos()
        }
    }
    private fun configurarBotoesFiltro() {
        val btnRealizadas = binding.btnFiltrarRealizadas
        val btnPendentes = binding.btnFiltrarPendentes

        val iconSize = 68  // tamanho desejado em pixels (ajustável)

        // Função para carregar e redimensionar o PNG
        fun getScaledDrawable(resourceId: Int, flipVertical: Boolean = false): Drawable? {
            val originalBitmap = BitmapFactory.decodeResource(resources, resourceId)

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, iconSize, iconSize, true)

            val finalBitmap = if (flipVertical) {
                val matrix = Matrix().apply { preScale(1f, -1f) }
                Bitmap.createBitmap(resizedBitmap, 0, 0, resizedBitmap.width, resizedBitmap.height, matrix, true)
            } else {
                resizedBitmap
            }

            return BitmapDrawable(resources, finalBitmap)
        }

        btnRealizadas.setOnClickListener {
            val filtradas = listaTarefas.filter { it.concluida }
            tarefaAdapter.atualizarLista(filtradas)

            // Ícone normal
            btnRealizadas.setCompoundDrawablesWithIntrinsicBounds(
                getScaledDrawable(R.drawable.ic_filtro_tarefas), null, null, null
            )
            btnPendentes.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        btnPendentes.setOnClickListener {
            val filtradas = listaTarefas.filter { !it.concluida }
            tarefaAdapter.atualizarLista(filtradas)

            // Ícone invertido verticalmente
            btnPendentes.setCompoundDrawablesWithIntrinsicBounds(
                getScaledDrawable(R.drawable.ic_filtro_tarefas, flipVertical = true), null, null, null
            )
            btnRealizadas.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }


    private fun configurarBotoesTipos() {
        binding.btnTipoEstudos.setOnClickListener { abrirAdicionarTarefa("Estudos") }
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

    private fun carregarTarefas() {
        lifecycleScope.launch {
            val db = AppUsageDatabase.getInstance(requireContext())
            val tarefas = withContext(Dispatchers.IO) {
                db.tarefaDao().listarTarefasPorUsuario(emailUsuario).firstOrNull() ?: emptyList()
            }

            listaTarefas = tarefas.toMutableList()
            tarefaAdapter.atualizarLista(tarefas)
            atualizarProgressoDiasConsecutivos() // Garantir atualização após carregamento
        }
    }

    private fun atualizarProgressoDiasConsecutivos() {
        progressoJob?.cancel()
        progressoJob = viewLifecycleOwner.lifecycleScope.launch {
            val db = AppUsageDatabase.getInstance(requireContext())
            val tarefas = withContext(Dispatchers.IO) {
                db.tarefaDao().listarTarefasPorUsuario(emailUsuario).firstOrNull() ?: emptyList()
            }

            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val hoje = formato.format(Date())

            val diasCompletos = tarefas
                .filter { it.concluida && ((it.data != null && it.data == hoje) || it.recorrente) }
                .mapNotNull { it.data ?: hoje }
                .toSet()

            val diasConsecutivos = contarDiasConsecutivos(diasCompletos)
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
            } else break
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
        carregarTarefas()
    }
}
