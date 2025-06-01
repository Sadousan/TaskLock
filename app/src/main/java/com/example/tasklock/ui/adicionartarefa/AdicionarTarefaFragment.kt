package com.example.tasklock.ui.adicionartarefa

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tasklock.R
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.TarefaEntity
import com.example.tasklock.databinding.FragmentAdicionartarefaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdicionarTarefaFragment : Fragment() {

    private var _binding: FragmentAdicionartarefaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdicionartarefaBinding.inflate(inflater, container, false)
        val root: View = binding.root

        configurarSpinners()
        configurarCheckRecorrente()
        configurarListeners()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<TextView>(R.id.toolbar_title)?.text =
            getString(R.string.menu_adicionartarefa)


        // Verifica se foi passado tipo pré-definido
        val tipoPredefinido = arguments?.getString("tipoTarefaPredefinido")
        tipoPredefinido?.let { tipo ->
            val pos = resources.getStringArray(R.array.tipos_tarefa).indexOf(tipo)
            if (pos >= 0) {
                binding.spinnerTipoTarefa.setSelection(pos)
                atualizarIconeTipoTarefa(tipo)
            }
        }
    }


    private fun configurarSpinners() {
        // Adapter para o spinner de tipo de tarefa
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tipos_tarefa,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTipoTarefa.adapter = adapter
        }

        // Adapter para o spinner de prioridade
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.prioridades_tarefa,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPrioridade.adapter = adapter
        }
    }

    private fun configurarCheckRecorrente() {
        binding.checkRecorrente.setOnCheckedChangeListener { _, isChecked ->
            binding.edtDataConclusao.isEnabled = !isChecked
            binding.edtDataConclusao.alpha = if (isChecked) 0.5f else 1f
            if (isChecked) binding.edtDataConclusao.setText("")
        }
    }

    private fun configurarListeners() {
        // Ícone muda conforme seleção do tipo
        binding.spinnerTipoTarefa.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val tipoSelecionado = parent.getItemAtPosition(position).toString()
                    atualizarIconeTipoTarefa(tipoSelecionado)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        // Clique no botão para salvar tarefa
        binding.btnAdicionarTarefa.setOnClickListener {
            salvarTarefa()
        }
    }

    private fun atualizarIconeTipoTarefa(tipo: String) {
        val drawableId = when (tipo) {
            "Estudos" -> R.drawable.ic_estudos
            "Exercício Físico" -> R.drawable.ic_exercicio
            "Trabalho" -> R.drawable.ic_trabalho
            "Esporte" -> R.drawable.ic_esporte
            else -> R.drawable.exemplo_foto
        }
        binding.imgIlustracao.setImageResource(drawableId)
    }

    private fun salvarTarefa() {
        val nome = binding.edtNomeTarefa.text.toString().trim()
        val tipo = binding.spinnerTipoTarefa.selectedItem.toString()
        val prioridade = binding.spinnerPrioridade.selectedItem.toString()
        val data =
            if (binding.checkRecorrente.isChecked) null else binding.edtDataConclusao.text.toString()
        val recorrente = binding.checkRecorrente.isChecked

        if (nome.isEmpty()) {
            Toast.makeText(requireContext(), "Digite um nome para a tarefa", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val bonus = when (prioridade) {
            "Leve" -> 5 * 60 * 1000L
            "Moderada" -> 10 * 60 * 1000L
            "Alta" -> 20 * 60 * 1000L
            else -> 0L
        }

        val novaTarefa = TarefaEntity(
            nome = nome,
            tipo = tipo,
            prioridade = prioridade,
            data = data,
            recorrente = recorrente,
            concluida = false,
            bonusMs = bonus
        )

        lifecycleScope.launch {
            val db = AppUsageDatabase.getInstance(requireContext())
            db.tarefaDao().inserirTarefa(novaTarefa)

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Tarefa salva com sucesso!", Toast.LENGTH_SHORT)
                    .show()
                limparCampos()
            }
        }
    }

    private fun limparCampos() {
        binding.edtNomeTarefa.setText("")
        binding.spinnerTipoTarefa.setSelection(0)
        binding.spinnerPrioridade.setSelection(0)
        binding.edtDataConclusao.setText("")
        binding.checkRecorrente.isChecked = false
        binding.imgIlustracao.setImageResource(R.drawable.exemplo_foto)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
