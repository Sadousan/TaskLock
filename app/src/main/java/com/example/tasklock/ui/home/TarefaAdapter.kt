package com.example.tasklock.ui.home

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklock.R
import com.example.tasklock.bonus.BonusManager
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.TarefaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TarefaAdapter(
    private val onTarefaAtualizada: (TarefaEntity) -> Unit
) : RecyclerView.Adapter<TarefaAdapter.TarefaViewHolder>() {

    private var listaTarefas: MutableList<TarefaEntity> = mutableListOf()
    private val tarefasSelecionadas = mutableSetOf<TarefaEntity>()

    fun atualizarLista(novaLista: List<TarefaEntity>) {
        listaTarefas = novaLista.toMutableList()
        tarefasSelecionadas.clear()
        notifyDataSetChanged()
    }

    fun obterSelecionadas(): List<TarefaEntity> = tarefasSelecionadas.toList()

    fun limparSelecao() {
        tarefasSelecionadas.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarefa, parent, false)
        return TarefaViewHolder(view)
    }

    override fun getItemCount(): Int = listaTarefas.size

    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) {
        val tarefa = listaTarefas[position]
        holder.bind(tarefa)
    }

    inner class TarefaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtNome: TextView = itemView.findViewById(R.id.txtNomeTarefa)
        private val imgIcone: ImageView = itemView.findViewById(R.id.imgIconeTarefa)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkConcluir)

        fun bind(tarefa: TarefaEntity) {
            txtNome.text = tarefa.nome
            atualizarCores(tarefa.concluida)

            val iconRes = when (tarefa.tipo) {
                "Estudos" -> R.drawable.ic_estudos
                "Exercício Físico" -> R.drawable.ic_exercicio
                "Trabalho" -> R.drawable.ic_trabalho
                "Esporte" -> R.drawable.ic_esporte
                "Outras" -> R.drawable.ic_outras
                else -> R.drawable.exemplo_foto
            }
            imgIcone.setImageResource(iconRes)

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = tarefa.concluida
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != tarefa.concluida) {
                    mostrarConfirmacao(itemView.context, tarefa, isChecked)
                }
            }

            // Seleção via clique longo para exclusão
            itemView.setBackgroundColor(
                if (tarefasSelecionadas.contains(tarefa)) Color.LTGRAY else Color.TRANSPARENT
            )

            itemView.setOnLongClickListener {
                if (tarefasSelecionadas.contains(tarefa)) {
                    tarefasSelecionadas.remove(tarefa)
                    itemView.setBackgroundColor(Color.TRANSPARENT)
                } else {
                    tarefasSelecionadas.add(tarefa)
                    itemView.setBackgroundColor(Color.LTGRAY)
                }
                true
            }
        }

        private fun atualizarCores(concluida: Boolean) {
            val cor = if (concluida) "#2E7D32" else "#C62828"
            txtNome.setTextColor(Color.parseColor(cor))
        }

        private fun mostrarConfirmacao(context: Context, tarefa: TarefaEntity, novaConclusao: Boolean) {
            val mensagem = if (novaConclusao)
                "Deseja marcar esta tarefa como concluída?"
            else
                "Deseja marcar esta tarefa como pendente?"

            AlertDialog.Builder(context)
                .setTitle("Confirmar alteração")
                .setMessage(mensagem)
                .setPositiveButton("Sim") { _, _ ->
                    val tarefaAtualizada = tarefa.copy(concluida = novaConclusao)
                    listaTarefas[adapterPosition] = tarefaAtualizada
                    notifyItemChanged(adapterPosition)
                    onTarefaAtualizada(tarefaAtualizada)

                    GlobalScope.launch(Dispatchers.IO) {
                        val db = AppUsageDatabase.getInstance(context)
                        db.tarefaDao().atualizarTarefa(tarefaAtualizada)

                        // Se a tarefa foi marcada como concluída e tem bônus
                        if (novaConclusao && tarefaAtualizada.bonusMs > 0) {
                            val sucesso = BonusManager.processarConclusaoTarefa(context, tarefaAtualizada.bonusMs)

                            withContext(Dispatchers.Main) {
                                if (sucesso) {
                                    Toast.makeText(context, "Bônus aplicado com sucesso!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Limite de bônus diário atingido.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                }
                .setNegativeButton("Cancelar") { _, _ ->
                    notifyItemChanged(adapterPosition)
                }
                .setCancelable(false)
                .show()
        }
    }
}
