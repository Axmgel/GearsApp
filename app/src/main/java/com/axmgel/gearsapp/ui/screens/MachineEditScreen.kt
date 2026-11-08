package com.axmgel.gearsapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.axmgel.gearsapp.core.machine.ChangeGear
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import com.axmgel.gearsapp.core.machine.GearHobber
import com.axmgel.gearsapp.core.machine.Machine
import com.axmgel.gearsapp.core.machine.MachineValidation

/**
 * Pantalla para capturar o cambiar los datos de una máquina.
 * Cada campo trae una explicación corta para que cualquier tornero sepa qué poner.
 */
@Composable
fun MachineEditScreen(
    initial: Machine,
    isNew: Boolean,
    onBack: () -> Unit,
    onSave: (Machine) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    val gears = remember { mutableStateListOf<ChangeGear>().apply { addAll(initial.changeGears.sortedBy { it.teeth }) } }

    // Campos de la fresadora con cabezal divisor
    val mill = initial as? DividingHeadMill
    var headRatio by rememberSaveable { mutableStateOf(mill?.headRatio?.toString() ?: "40") }
    var leadScrew by rememberSaveable { mutableStateOf(mill?.let { format(it.tableLeadScrewPitchMm) } ?: "5.08") }
    val plates = remember {
        mutableStateListOf<String>().apply { mill?.indexPlates?.forEach { add(it.joinToString(", ")) } }
    }

    // Campos de la generadora
    val hobber = initial as? GearHobber
    var helixConstant by rememberSaveable { mutableStateOf(hobber?.let { format(it.helixConstant) } ?: "8.355615") }
    var divisionConstant by rememberSaveable { mutableStateOf(hobber?.divisionConstant?.let { format(it) } ?: "") }

    var confirmDelete by remember { mutableStateOf(false) }

    // Arma la máquina con lo que está escrito; si un número no se entiende, se avisa.
    val parseErrors = mutableListOf<String>()
    val draft: Machine = when (initial) {
        is DividingHeadMill -> {
            val ratio = headRatio.trim().toIntOrNull()
            if (ratio == null) parseErrors += "La relación del cabezal debe ser un número entero, por ejemplo 40."
            val pitch = parseDecimal(leadScrew)
            if (pitch == null) parseErrors += "El paso del husillo debe ser un número, por ejemplo 5.08."
            val parsedPlates = plates.mapIndexed { i, text ->
                parseIntList(text) ?: emptyList<Int>().also {
                    parseErrors += "En el plato ${i + 1} escribe los agujeros separados por comas, por ejemplo 15, 16, 17."
                }
            }
            initial.copy(
                name = name.trim(),
                headRatio = ratio ?: 0,
                tableLeadScrewPitchMm = pitch ?: 0.0,
                changeGears = gears.toList(),
                indexPlates = parsedPlates,
            )
        }
        is GearHobber -> {
            val constant = parseDecimal(helixConstant)
            if (constant == null) parseErrors += "La constante de hélice debe ser un número, por ejemplo 8.355615."
            val division = if (divisionConstant.isBlank()) null else parseDecimal(divisionConstant)
            if (divisionConstant.isNotBlank() && division == null) parseErrors += "La constante de división debe ser un número."
            initial.copy(
                name = name.trim(),
                helixConstant = constant ?: 0.0,
                divisionConstant = division,
                changeGears = gears.toList(),
            )
        }
    }
    val problems = parseErrors + MachineValidation.problems(draft)

    AppScaffold(title = if (isNew) "Nueva máquina" else "Editar máquina", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(machineTypeLabel(initial), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                supportingText = { Text("Cómo la reconoces en el taller, por ejemplo \"Fresadora Cincinnati\".") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            when (initial) {
                is DividingHeadMill -> {
                    NumberField(
                        value = headRatio,
                        onChange = { headRatio = it },
                        label = "Relación del cabezal",
                        help = "Cuántas vueltas das a la manivela para que la pieza dé una vuelta completa. " +
                            "En casi todos los cabezales es 40.",
                        decimal = false,
                    )
                    NumberField(
                        value = leadScrew,
                        onChange = { leadScrew = it },
                        label = "Paso del husillo de la mesa (mm)",
                        help = "Cuánto avanza la mesa en una vuelta del tornillo que la mueve. " +
                            "Si tu husillo es de 5 hilos por pulgada, el paso es 5.08 mm.",
                        decimal = true,
                    )
                    val ratio = headRatio.trim().toIntOrNull()
                    val pitch = parseDecimal(leadScrew)
                    if (ratio != null && pitch != null) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Paso de la máquina: ${format(ratio * pitch)} mm", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Se obtiene multiplicando la relación del cabezal por el paso del husillo: " +
                                        "$ratio × ${format(pitch)} = ${format(ratio * pitch)} mm. Es lo que avanza la mesa " +
                                        "mientras la pieza da una vuelta, si los engranes de cambio son iguales.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Platos divisores", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Escribe cuántos agujeros tiene cada circunferencia de cada plato, separados por comas.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    plates.forEachIndexed { i, text ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { plates[i] = it },
                                label = { Text("Plato ${i + 1}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { plates.removeAt(i) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar plato ${i + 1}")
                            }
                        }
                    }
                    OutlinedButton(onClick = { plates.add("") }) { Text("Agregar plato") }
                }
                is GearHobber -> {
                    NumberField(
                        value = helixConstant,
                        onChange = { helixConstant = it },
                        label = "Constante de hélice",
                        help = "Número que da el fabricante de tu generadora para el tren de hélice. " +
                            "Viene en el manual o en la placa de la máquina.",
                        decimal = true,
                    )
                    NumberField(
                        value = divisionConstant,
                        onChange = { divisionConstant = it },
                        label = "Constante de división (opcional)",
                        help = "Relación entre el cabezal divisor de la generadora y el chuck que sujeta la pieza (vueltas del sinfín por una vuelta del chuck). " +
                            "Se usa así: relación = constante × entradas de la fresa ÷ número de dientes.",
                        decimal = true,
                    )
                }
            }

            HorizontalDivider()
            GearSetEditor(gears)

            if (problems.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Falta corregir:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        problems.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            Button(
                onClick = { onSave(draft) },
                enabled = problems.isEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar") }

            if (onDelete != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Borrar máquina", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Borrar esta máquina?") },
            text = { Text("Se pierden sus datos y su lista de engranes.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

/** Lista de engranes de cambio con su cantidad. */
@Composable
private fun GearSetEditor(gears: MutableList<ChangeGear>) {
    var newTeeth by rememberSaveable { mutableStateOf("") }

    Text("Engranes de cambio", style = MaterialTheme.typography.titleMedium)
    Text(
        "Anota cada engrane que tienes y cuántos hay de ese número de dientes. " +
            "Si tienes dos de 20 dientes, pon cantidad 2: así la app sabe que puede usar los dos a la vez.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text("Total: ${gears.sumOf { it.quantity }} engranes", style = MaterialTheme.typography.bodyMedium)

    gears.forEachIndexed { i, gear ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("${gear.teeth} dientes", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = {
                if (gear.quantity > 1) gears[i] = gear.copy(quantity = gear.quantity - 1) else gears.removeAt(i)
            }) { Icon(Icons.Filled.Remove, contentDescription = "Uno menos de ${gear.teeth}") }
            Text("× ${gear.quantity}", modifier = Modifier.width(40.dp))
            IconButton(onClick = { gears[i] = gear.copy(quantity = gear.quantity + 1) }) {
                Icon(Icons.Filled.Add, contentDescription = "Uno más de ${gear.teeth}")
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newTeeth,
            onValueChange = { newTeeth = it.filter(Char::isDigit) },
            label = { Text("Dientes del engrane") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Button(
            enabled = (newTeeth.toIntOrNull() ?: 0) > 0,
            onClick = {
                val teeth = newTeeth.toInt()
                val index = gears.indexOfFirst { it.teeth == teeth }
                if (index >= 0) {
                    gears[index] = gears[index].copy(quantity = gears[index].quantity + 1)
                } else {
                    // Se inserta en orden para que la lista quede de menor a mayor.
                    val position = gears.indexOfFirst { it.teeth > teeth }.let { if (it < 0) gears.size else it }
                    gears.add(position, ChangeGear(teeth, 1))
                }
                newTeeth = ""
            },
        ) { Text("Agregar") }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, help: String, decimal: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(help) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Acepta punto o coma decimal ("5.08" o "5,08"). */
private fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

private fun parseIntList(text: String): List<Int>? {
    val parts = text.split(',', ' ', ';').filter { it.isNotBlank() }
    if (parts.isEmpty()) return null
    val numbers = parts.map { it.trim().toIntOrNull() ?: return null }
    return numbers
}
