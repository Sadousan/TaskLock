package com.example.tasklock.ui.usodeapps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tasklock.databinding.FragmentUsodeappsBinding


class UsoDeAppsFragment : Fragment() {

    private var _binding: FragmentUsodeappsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val usodeappsBindingViewModel =
            ViewModelProvider(this).get(UsoDeAppsViewModel::class.java)

        _binding = FragmentUsodeappsBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val textView: TextView = binding.textUsodeapps
        usodeappsBindingViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}