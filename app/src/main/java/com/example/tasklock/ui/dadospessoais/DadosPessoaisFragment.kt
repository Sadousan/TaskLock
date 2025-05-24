package com.example.tasklock.ui.dadospessoais

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tasklock.databinding.FragmentDadospessoaisBinding


class DadosPessoaisFragment : Fragment() {

    private var _binding: FragmentDadospessoaisBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dadosPessoaisViewModel =
            ViewModelProvider(this).get(DadosPessoaisViewModel::class.java)

        _binding = FragmentDadospessoaisBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDadospessoais
        dadosPessoaisViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}