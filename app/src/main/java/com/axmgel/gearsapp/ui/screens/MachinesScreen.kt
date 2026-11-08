package com.axmgel.gearsapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import com.axmgel.gearsapp.core.machine.GearHobber
import com.axmgel.gearsapp.core.machine.Machine
import java.util.Locale
import java.util.UUID

const val TYPE_MILL = "cabezal"
const val TYPE_HOBBER = "generadora"

fun newMachine(type: String): Machine {
    val id = UUID.randomUUID().toString()
    return if (type == TYPE_HOBBER) GearHobber(id = id, name = "") else DividingHeadMill(id = id, name = "")
}

fun machineTypeLabel(machine: Machine): String = when (machine) {
    is DividingHeadMill -> "Fresadora universal con cabezal divisor"
    is GearHobber -> "Generadora de engranajes"
}

@Composable
fun MachinesScreen(
    machines: List<Machine>?,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onAdd: (String) -> Unit,
) {
    var choosingType by remember { mutableStateOf(false) }

    AppScaffold(
        title = "Configurar máquinas",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { choosingType = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agregar máquina") },
            )
        },
    ) { padding ->
        if (machines == null) {
            LoadingScreen()
            return@AppScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Aquí capturas los datos de las máquinas de tu taller. Los cálculos usan estos datos " +
                        "para darte engranes que sí tienes y vueltas que sí puedes dar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (machines.isEmpty()) {
                item { Text("Todavía no tienes máquinas. Toca \"Agregar máquina\".") }
            }
            items(machines, key = { it.id }) { machine ->
                Card(onClick = { onOpen(machine.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(machine.name.ifBlank { "(sin nombre)" }, style = MaterialTheme.typography.titleLarge)
                        Text(machineTypeLabel(machine), style = MaterialTheme.typography.bodyMedium)
                        Text(summary(machine), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (choosingType) {
        AlertDialog(
            onDismissRequest = { choosingType = false },
            title = { Text("¿Qué máquina quieres agregar?") },
            text = { Text("Elige el tipo de máquina.") },
            confirmButton = {
                TextButton(onClick = { choosingType = false; onAdd(TYPE_MILL) }) { Text("Fresadora con cabezal") }
            },
            dismissButton = {
                TextButton(onClick = { choosingType = false; onAdd(TYPE_HOBBER) }) { Text("Generadora") }
            },
        )
    }
}

private fun summary(machine: Machine): String {
    val gears = machine.changeGears.sumOf { it.quantity }
    return when (machine) {
        is DividingHeadMill ->
            "Paso de la máquina ${format(machine.machineLeadMm)} mm · $gears engranes de cambio"
        is GearHobber ->
            "Constante de hélice ${format(machine.helixConstant)} · $gears engranes de cambio"
    }
}

internal fun format(value: Double): String =
    String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
