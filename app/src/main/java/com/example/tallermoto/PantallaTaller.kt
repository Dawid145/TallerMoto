package com.example.tallermoto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val moneda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTaller(vm: TallerViewModel = viewModel()) {
    val trabajos by vm.trabajos.collectAsStateWithLifecycle()
    val resumen by vm.resumen.collectAsStateWithLifecycle()
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    var mostrarDialogo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Taller de motos") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar trabajo")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TarjetaResumen("Pendiente", resumen.pendiente, Modifier.weight(1f))
                TarjetaResumen("Cobrado", resumen.cobrado, Modifier.weight(1f))
            }
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Filtro.entries.forEach { f ->
                    FilterChip(
                        selected = f == filtro,
                        onClick = { vm.setFiltro(f) },
                        label = { Text(f.etiqueta) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trabajos, key = { it.id }) { t ->
                    TarjetaTrabajo(
                        t,
                        onAlternar = { vm.alternarTerminado(t) },
                        onBorrar = { vm.borrar(t) }
                    )
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoNuevo(
            onCancelar = { mostrarDialogo = false },
            onGuardar = { c, m, d, costo ->
                vm.agregar(c, m, d, costo)
                mostrarDialogo = false
            }
        )
    }
}

@Composable
private fun TarjetaResumen(titulo: String, monto: Double, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(
                moneda.format(monto),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TarjetaTrabajo(t: Trabajo, onAlternar: () -> Unit, onBorrar: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (t.terminado) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${t.moto} · ${t.cliente}", fontWeight = FontWeight.Bold)
                if (t.descripcion.isNotBlank()) Text(t.descripcion)
                Text(
                    "${formatoFecha.format(Date(t.fecha))} · ${moneda.format(t.costo)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onAlternar) {
                Icon(Icons.Default.Check, contentDescription = "Marcar terminado")
            }
            IconButton(onClick = onBorrar) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar")
            }
        }
    }
}

@Composable
private fun DialogoNuevo(
    onCancelar: () -> Unit,
    onGuardar: (String, String, String, Double) -> Unit
) {
    var cliente by remember { mutableStateOf("") }
    var moto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var costo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo trabajo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(cliente, { cliente = it }, label = { Text("Cliente") }, singleLine = true)
                OutlinedTextField(moto, { moto = it }, label = { Text("Moto") }, singleLine = true)
                OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Trabajo a realizar") })
                OutlinedTextField(
                    costo, { costo = it },
                    label = { Text("Costo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = cliente.isNotBlank() && moto.isNotBlank(),
                onClick = {
                    onGuardar(
                        cliente.trim(), moto.trim(), descripcion.trim(),
                        costo.replace(',', '.').toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
