//package com.example.tasklock.ui.usodeapps
//
//import android.app.Application
//import androidx.lifecycle.*
//import com.example.tasklock.data.db.AppUsageDatabase
//import com.example.tasklock.data.model.AppUsageAggregated
//import com.example.tasklock.data.model.BlockedAppEntity
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class BloqueioAppsViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val dao = AppUsageDatabase.getInstance(application).appUsageDao()
//    private val daoBlocked = AppUsageDatabase.getInstance(application).blockedAppsDao()
//
//    private val _apps = MutableLiveData<List<AppUsageAggregated>>()
//    val apps: LiveData<List<AppUsageAggregated>> = _apps
//
//    fun carregarApps(appsBloqueados: List<String>, prefixosSistema: List<String>, appInfoManual: Map<String, Pair<String, Int>>) {
//        viewModelScope.launch {
//            val apps = withContext(Dispatchers.IO) { dao.getAllAgrupado() }
//            val filtrados = apps
//                .filter { appInfoManual.containsKey(it.packageName) }
//                .filterNot { prefixosSistema.any { prefix -> it.packageName.startsWith(prefix) } }
//                .filterNot { it.packageName.contains("launcher") || it.packageName.contains("setupwizard") }
//                .filterNot { appsBloqueados.contains(it.packageName) }
//                .distinctBy { it.packageName }
//                .sortedByDescending { it.totalTimeMs }
//            _apps.postValue(filtrados)
//        }
//    }
//
//    fun inserirOuAtualizarBloqueio(app: String, dailyLimit: Long) {
//        viewModelScope.launch {
//            val bloqueado = withContext(Dispatchers.IO) { daoBlocked.getByPackage(app) }
//            val entity = BlockedAppEntity(
//                packageName = app,
//                dailyLimitMs = dailyLimit,
//                usedTodayMs = 0L
//            )
//            withContext(Dispatchers.IO) {
//                daoBlocked.insertOrUpdate(entity)
//            }
//        }
//    }
//
//    suspend fun getAppsBloqueados(): List<String> {
//        return withContext(Dispatchers.IO) {
//            daoBlocked.getAll().map { it.packageName }
//        }
//    }
//}
