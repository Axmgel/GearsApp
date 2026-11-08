package com.axmgel.gearsapp.ui.screens.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axmgel.gearsapp.core.calc.fmt
import com.axmgel.gearsapp.core.gear.BevelGear
import com.axmgel.gearsapp.core.gear.GearResult
import com.axmgel.gearsapp.core.gear.HelicalGear
import com.axmgel.gearsapp.core.gear.SpurGear
import com.axmgel.gearsapp.core.gear.ToothSystem
import com.axmgel.gearsapp.core.report.Reports
import com.axmgel.gearsapp.report.ReportButtons
import com.axmgel.gearsapp.ui.screens.AppScaffold

enum class GearKind(val title: String, val intro: String) {
    SPUR("Engranes rectos", "Medidas para tornear y fresar un engrane de dientes rectos con el sistema de módulo."),
    HELICAL("Engranes helicoidales", "Medidas de un engrane helicoidal de ejes paralelos y el paso de hélice para el cabezal divisor."),
    BEVEL("Engranes cónicos", "Medidas de un engrane cónico de dientes rectos: ángulos para tornear el cono y para inclinar el cabezal."),
    SPIRAL_BEVEL("Engranes cónicos helicoidales", "Medidas de un engrane cónico con dientes inclinados (espiral)."),
}

/** Acciones para pasar los datos del engrane a los cálculos de maquinaria. */
class GearLinks(
    val toIndexing: (teeth: Int) -> Unit,
    val toHelicalMilling: (leadMm: Double, angleDeg: Double) -> Unit,
    val toHobberHelix: (module: Double, angleDeg: Double) -> Unit,
)

@Composable
fun GearScreen(kind: GearKind, links: GearLinks, onBack: () -> Unit) {
    var module by rememberSaveable { mutableStateOf("") }
    var teeth by rememberSaveable { mutableStateOf("") }
    var mate by rememberSaveable { mutableStateOf("") }
    var angle by rememberSaveable { mutableStateOf("") }
    var shaftAngle by rememberSaveable { mutableStateOf("90") }
    var faceWidth by rememberSaveable { mutableStateOf("") }
    var pressure by rememberSaveable { mutableStateOf("20") }
    var addendum by rememberSaveable { mutableStateOf("1") }
    var dedendum by rememberSaveable { mutableStateOf("1.25") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<GearResult?>(null) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    val bevel = kind == GearKind.BEVEL || kind == GearKind.SPIRAL_BEVEL
    val helical = kind == GearKind.HELICAL || kind == GearKind.SPIRAL_BEVEL

    fun calculate() {
        errorText = null
        result = runCatching {
            val m = parseDecimal(module) ?: error("Escribe el módulo.")
            val z = teeth.toIntOrNull() ?: error("Escribe el número de dientes.")
            val zMate = mate.toIntOrNull()
            val system = ToothSystem(
                pressureAngleDeg = parseDecimal(pressure) ?: error("Escribe el ángulo de presión."),
                addendum = parseDecimal(addendum) ?: error("Escribe el coeficiente de cabeza."),
                dedendum = parseDecimal(dedendum) ?: error("Escribe el coeficiente de pie."),
            )
            when (kind) {
                GearKind.SPUR -> SpurGear.compute(m, z, zMate, system)
                GearKind.HELICAL -> HelicalGear.compute(m, z, parseDecimal(angle) ?: error("Escribe el ángulo de hélice."), zMate, system)
                GearKind.BEVEL, GearKind.SPIRAL_BEVEL -> BevelGear.compute(
                    module = m,
                    teeth = z,
                    mateTeeth = zMate ?: error("Escribe los dientes del otro engrane; se necesitan para el ángulo del cono."),
                    shaftAngleDeg = parseDecimal(shaftAngle) ?: error("Escribe el ángulo entre ejes."),
                    faceWidth = if (faceWidth.isBlank()) null else parseDecimal(faceWidth) ?: error("El largo del diente no es un número."),
                    spiralAngleDeg = if (kind == GearKind.SPIRAL_BEVEL) parseDecimal(angle) ?: error("Escribe el ángulo de espiral.") else 0.0,
                    system = system,
                )
            }
        }.onFailure { errorText = it.message ?: "Revisa los datos." }.getOrNull()
    }

    AppScaffold(title = kind.title, onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(kind.intro, style = MaterialTheme.typography.bodyMedium)

            NumberInput(
                module, { module = it },
                label = when {
                    bevel -> "Módulo (extremo grande)"
                    helical -> "Módulo normal (el de la fresa)"
                    else -> "Módulo"
                },
                help = "Tamaño del diente. Viene grabado en la fresa de módulo.",
            )
            NumberInput(teeth, { teeth = it.filter(Char::isDigit) }, "Número de dientes (Z)", decimal = false)
            NumberInput(
                mate, { mate = it.filter(Char::isDigit) },
                label = if (bevel) "Dientes del otro engrane" else "Dientes del otro engrane (opcional)",
                help = if (bevel) "Necesarios para calcular el ángulo del cono." else "Para calcular la distancia entre centros.",
                decimal = false,
            )
            if (helical) {
                NumberInput(
                    angle, { angle = it },
                    label = if (bevel) "Ángulo de espiral β (grados)" else "Ángulo de hélice β (grados)",
                    help = "Inclinación del diente respecto al eje (en cónicos, a la mitad del diente).",
                )
            }
            if (bevel) {
                NumberInput(shaftAngle, { shaftAngle = it }, "Ángulo entre ejes (grados)", "Casi siempre 90°.")
                NumberInput(faceWidth, { faceWidth = it }, "Largo del diente (mm, opcional)", "Si lo dejas vacío se usa un tercio de la generatriz.")
            }

            TextButton(onClick = { advanced = !advanced }) {
                Text(if (advanced) "Ocultar forma del diente" else "Forma del diente (ángulo de presión, cabeza y pie)")
            }
            if (advanced) {
                NumberInput(pressure, { pressure = it }, "Ángulo de presión (grados)", "20° es el más común; los engranes antiguos usan 14.5°.")
                NumberInput(addendum, { addendum = it }, "Cabeza ÷ módulo", "Normalmente 1.")
                NumberInput(dedendum, { dedendum = it }, "Pie ÷ módulo", "1.25 en la norma ISO. Algunos libros usan 1.167.")
            }

            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calcular") }
            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            result?.let { r ->
                DimsCard(r)
                StepsCard(r.steps)
                val inputs = buildList {
                    add("Módulo" to module)
                    add("Dientes" to teeth)
                    if (mate.isNotBlank()) add("Dientes del otro engrane" to mate)
                    if (helical) add((if (bevel) "Ángulo de espiral" else "Ángulo de hélice") to "$angle°")
                    if (bevel) add("Ángulo entre ejes" to "$shaftAngle°")
                    add("Ángulo de presión" to "$pressure°")
                    add("Cabeza / pie (× módulo)" to "$addendum / $dedendum")
                }
                ReportButtons(Reports.gear(kind.title, inputs, r))
                Text("Siguiente paso: maquinar", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { links.toIndexing(r.teeth) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Divisiones en el cabezal (${r.teeth})")
                }
                val lead = r.helixLeadMm
                val beta = r.helixAngleDeg
                if (lead != null && beta != null) {
                    OutlinedButton(onClick = { links.toHelicalMilling(lead, beta) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Engranes de la hélice en el cabezal")
                    }
                }
                val mn = r.normalModule
                if (mn != null && beta != null && kind == GearKind.HELICAL) {
                    OutlinedButton(onClick = { links.toHobberHelix(mn, beta) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tren de hélice en la generadora")
                    }
                }
            }
        }
    }
}

@Composable
private fun DimsCard(result: GearResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Medidas", style = MaterialTheme.typography.titleMedium)
            result.dims.forEach { d ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${d.name} (${d.symbol})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${fmt(d.value, if (d.unit == "°") 4 else 3)} ${d.unit}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
