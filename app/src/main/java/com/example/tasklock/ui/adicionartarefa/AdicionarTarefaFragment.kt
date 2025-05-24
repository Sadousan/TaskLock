package com.example.tasklock.ui.adicionartarefa

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tasklock.databinding.FragmentAdicionartarefaBinding


class AdicionarTarefaFragment : Fragment() {

    private var _binding: FragmentAdicionartarefaBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val adicionarTarefaViewModel =
            ViewModelProvider(this).get(AdicionarTarefaViewModel::class.java)

        _binding = FragmentAdicionartarefaBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.edtNomeTarefa
        adicionarTarefaViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}