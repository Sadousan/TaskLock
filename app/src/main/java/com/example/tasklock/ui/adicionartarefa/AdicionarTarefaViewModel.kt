package com.example.tasklock.ui.adicionartarefa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AdicionarTarefaViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "tarefa é nossa"
    }
    val text: LiveData<String> = _text
}