package com.example.tasklock

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.tasklock.data.db.AppUsageDatabase
import com.example.tasklock.data.model.TarefaEntity
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import androidx.appcompat.app.ActionBarDrawerToggle

class AdicionarTarefaActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var spinnerTipo: Spinner
    private lateinit var spinnerPrioridade: Spinner
    private lateinit var edtNomeTarefa: EditText
    private lateinit var edtDataConclusao: EditText
    private lateinit var checkRecorrente: CheckBox
    private lateinit var btnAdicionar: Button
    private lateinit var imgIlustracao: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_tarefa)

        // Infla o conteúdo da tela dentro do FrameLayout
        val container = findViewById<FrameLayout>(R.id.fragment_container_view)
        val contentView = layoutInflater.inflate(R.layout.layout_adicionar_tarefa_conteudo, container, false)
        container.addView(contentView)

        // Configura a Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<TextView>(R.id.toolbar_title).text = getString(R.string.menu_adicionartarefa)

        // Configura o Drawer e o menu lateral
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        configurarMenu(navView)

        // Configura o botão de menu (≡)
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Inicializa campos da tela
        inicializarCampos()
        configurarSpinners()
        configurarListeners()

        // Aplica tipo pré-definido, se veio via Intent
        intent.getStringExtra("tipoTarefaPredefinido")?.let { tipo ->
            val pos = resources.getStringArray(R.array.tipos_tarefa).indexOf(tipo)
            if (pos >= 0) {
                spinnerTipo.setSelection(pos)
                val drawableId = when (tipo) {
                    "Estudos" -> R.drawable.ic_estudos
                    "Exercício Físico" -> R.drawable.ic_exercicio
                    "Trabalho" -> R.drawable.ic_trabalho
                    "Esporte" -> R.drawable.ic_esporte
                    "Outras" -> R.drawable.ic_outras
                    else -> R.drawable.exemplo_foto
                }
                imgIlustracao.setImageResource(drawableId)
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState() // Garante funcionamento correto após recriação da tela
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        // Ao pressionar voltar, redireciona corretamente para a TelaPrincipalMenu
        startActivity(Intent(this, TelaPrincipalMenu::class.java).apply {
            putExtra("navigate_to", R.id.nav_home)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    private fun configurarMenu(navView: NavigationView) {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, TelaPrincipalMenu::class.java).apply {
                        putExtra("navigate_to", R.id.nav_home)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                }
                R.id.nav_usoapp -> {
                    startActivity(Intent(this, UsoApp::class.java))
                }
                R.id.nav_appsbloqueados -> {
                    startActivity(Intent(this, BlockedAppsActivity::class.java))
                }
                R.id.nav_adicionartarefa -> {
                    // Já está nessa tela
                    return@setNavigationItemSelectedListener true
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            finish()
            true
        }
    }

    private fun inicializarCampos() {
        spinnerTipo = findViewById(R.id.spinnerTipoTarefa)
        spinnerPrioridade = findViewById(R.id.spinnerPrioridade)
        edtNomeTarefa = findViewById(R.id.edtNomeTarefa)
        edtDataConclusao = findViewById(R.id.edtDataConclusao)
        checkRecorrente = findViewById(R.id.checkRecorrente)
        btnAdicionar = findViewById(R.id.btnAdicionarTarefa)
        imgIlustracao = findViewById(R.id.imgIlustracao)
    }

    private fun configurarSpinners() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tipos_tarefa,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipo.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.prioridades_tarefa,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPrioridade.adapter = adapter
        }
    }

    private fun configurarListeners() {
        checkRecorrente.setOnCheckedChangeListener { _, isChecked ->
            edtDataConclusao.isEnabled = !isChecked
            edtDataConclusao.alpha = if (isChecked) 0.5f else 1f
            if (isChecked) edtDataConclusao.setText("")
        }

        spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val tipoSelecionado = parent.getItemAtPosition(position).toString()
                val drawableId = when (tipoSelecionado) {
                    "Estudos" -> R.drawable.ic_estudos
                    "Exercício Físico" -> R.drawable.ic_exercicio
                    "Trabalho" -> R.drawable.ic_trabalho
                    "Esporte" -> R.drawable.ic_esporte
                    "Outras" -> R.drawable.ic_outras
                    else -> R.drawable.exemplo_foto
                }
                imgIlustracao.setImageResource(drawableId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnAdicionar.setOnClickListener {
            salvarTarefa()
        }
    }

    private fun salvarTarefa() {
        val nome = edtNomeTarefa.text.toString().trim()
        val tipo = spinnerTipo.selectedItem.toString()
        val prioridade = spinnerPrioridade.selectedItem.toString()
        val data = if (checkRecorrente.isChecked) null else edtDataConclusao.text.toString()
        val recorrente = checkRecorrente.isChecked

        if (nome.isEmpty()) {
            Toast.makeText(this, "Digite um nome para a tarefa", Toast.LENGTH_SHORT).show()
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

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppUsageDatabase.getInstance(this@AdicionarTarefaActivity)
            db.tarefaDao().inserirTarefa(novaTarefa)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AdicionarTarefaActivity, "Tarefa salva com sucesso!", Toast.LENGTH_SHORT).show()

                // AVISA AO FRAGMENT QUE A TAREFA FOI SALVA
                setResult(RESULT_OK)
                finish()

            }

        }
    }

    private fun limparCampos() {
        edtNomeTarefa.setText("")
        spinnerTipo.setSelection(0)
        spinnerPrioridade.setSelection(0)
        edtDataConclusao.setText("")
        checkRecorrente.isChecked = false
        imgIlustracao.setImageResource(R.drawable.exemplo_foto)
    }
}
