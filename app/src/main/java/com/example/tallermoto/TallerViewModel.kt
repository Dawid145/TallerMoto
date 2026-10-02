package com.example.tallermoto

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Filtro(val etiqueta: String) {
    TODOS("Todos"), PENDIENTES("Pendientes"), TERMINADOS("Terminados")
}

data class Resumen(val pendiente: Double = 0.0, val cobrado: Double = 0.0)

class TallerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = TallerDb.get(app).trabajoDao()

    private val _filtro = MutableStateFlow(Filtro.TODOS)
    val filtro: StateFlow<Filtro> = _filtro.asStateFlow()

    val trabajos: StateFlow<List<Trabajo>> =
        combine(dao.observar(), _filtro) { lista, f ->
            when (f) {
                Filtro.TODOS -> lista
                Filtro.PENDIENTES -> lista.filter { !it.terminado }
                Filtro.TERMINADOS -> lista.filter { it.terminado }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resumen: StateFlow<Resumen> =
        dao.observar().map { lista ->
            Resumen(
                pendiente = lista.filter { !it.terminado }.sumOf { it.costo },
                cobrado = lista.filter { it.terminado }.sumOf { it.costo }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resumen())

    fun setFiltro(f: Filtro) { _filtro.value = f }

    fun agregar(cliente: String, moto: String, descripcion: String, costo: Double) {
        viewModelScope.launch {
            dao.insertar(Trabajo(cliente = cliente, moto = moto, descripcion = descripcion, costo = costo))
        }
    }

    fun alternarTerminado(t: Trabajo) {
        viewModelScope.launch { dao.actualizar(t.copy(terminado = !t.terminado)) }
    }

    fun borrar(t: Trabajo) {
        viewModelScope.launch { dao.borrar(t) }
    }
}
