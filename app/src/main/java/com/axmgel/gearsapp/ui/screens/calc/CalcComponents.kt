package com.axmgel.gearsapp.ui.screens.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.axmgel.gearsapp.core.calc.CalcStep
import com.axmgel.gearsapp.core.calc.TrainSolution
import com.axmgel.gearsapp.core.calc.fmt
import com.axmgel.gearsapp.core.machine.Machine
import com.axmgel.gearsapp.ui.screens.AppScaffold
import com.axmgel.gearsapp.ui.screens.LoadingScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Acepta punto o coma decimal ("5.08" o "5,08"). */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/**
 * Estructura común de una pantalla de cálculo: elegir máquina, capturar datos, botón Calcular
 * y debajo la explicación y los resultados.
 *
 * @param compute recibe la máquina elegida y devuelve el resultado, o lanza un error con un mensaje para el usuario.
 *   Se corre fuera del hilo de la pantalla porque la búsqueda de engranes puede tardar un poco.
 */
@Composable
fun <M : Machine, R : Any> CalcScreen(
    title: String,
    intro: String,
    machines: List<M>?,
    noMachineText: String,
    onBack: () -> Unit,
    inputs: @Composable (M) -> Unit,
    compute: (M) -> R,
    results: @Composable (R) -> Unit,
) {
    AppScaffold(title = title, onBack = onBack) { padding ->
        if (machines == null) {
            LoadingScreen()
            return@AppScaffold
        }
        var selectedId by rememberSaveable { mutableStateOf(machines.firstOrNull()?.id) }
        val machine = machines.firstOrNull { it.id == selectedId } ?: machines.firstOrNull()
        var result by androidx.compose.runtime.remember { mutableStateOf<R?>(null) }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        var busy by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(intro, style = MaterialTheme.typography.bodyMedium)

            if (machine == null) {
                Text(noMachineText, color = MaterialTheme.colorScheme.error)
                return@Column
            }

            if (machines.size > 1) {
                Text("Máquina", style = MaterialTheme.typography.titleSmall)
                machines.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = m.id == machine.id, role = Role.RadioButton) {
                                selectedId = m.id
                                result = null
                            },
                    ) {
                        RadioButton(selected = m.id == machine.id, onClick = null)
                        Text(m.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            } else {
                Text("Máquina: ${machine.name}", style = MaterialTheme.typography.titleSmall)
            }

            inputs(machine)

            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        val outcome = withContext(Dispatchers.Default) { runCatching { compute(machine) } }
                        outcome.onSuccess { result = it }.onFailure {
                            result = null
                            error = it.message ?: "Revisa los datos."
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Calcular") }

            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text("Buscando combinaciones…")
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { results(it) }
        }
    }
}

@Composable
fun NumberInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    help: String? = null,
    decimal: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = help?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ToleranceInput(value: String, onChange: (String) -> Unit) {
    NumberInput(
        value = value,
        onChange = onChange,
        label = "Error permitido (%)",
        help = "Qué tanto puede diferir la relación de los engranes de la exacta. 0.01 % es como en tus programas originales.",
    )
}

/** Lee la tolerancia en % y la regresa como fracción, o lanza un error entendible. */
fun toleranceFraction(text: String): Double {
    val pct = parseDecimal(text) ?: error("Escribe el error permitido, por ejemplo 0.01.")
    require(pct > 0.0) { "El error permitido debe ser mayor que cero." }
    return pct / 100.0
}

/** Explicación paso a paso del cálculo. */
@Composable
fun StepsCard(steps: List<CalcStep>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cómo se calcula", style = MaterialTheme.typography.titleMedium)
            steps.forEachIndexed { i, step ->
                Column {
                    Text("${i + 1}. ${step.title}", style = MaterialTheme.typography.titleSmall)
                    Text(step.detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * Lista de combinaciones de engranes.
 * @param extra texto adicional por combinación (por ejemplo, el paso que resulta).
 */
@Composable
fun TrainResults(solutions: List<TrainSolution>, extra: (TrainSolution) -> String? = { null }, shown: Int = 60) {
    Text(
        if (solutions.isEmpty()) "Sin combinaciones." else "Combinaciones encontradas: ${solutions.size}" +
            if (solutions.size > shown) " (se muestran las $shown mejores)" else "",
        style = MaterialTheme.typography.titleMedium,
    )
    if (solutions.isNotEmpty()) {
        Text(
            "Ordenadas de menor a mayor error. A y C son conductores; B y D, conducidos.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    solutions.take(shown).forEachIndexed { i, s ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("${i + 1})  ${s.train.describe()}", style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                Text(
                    "Relación ${fmt(s.ratio, 8)} · error ${fmt(s.relativeError * 100, 5)} %",
                    style = MaterialTheme.typography.bodySmall,
                )
                extra(s)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun SectionDivider() = HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
