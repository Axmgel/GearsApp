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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.axmgel.gearsapp.core.gear.DiameterKind
import com.axmgel.gearsapp.core.gear.ModuleFinder
import com.axmgel.gearsapp.core.gear.ModuleSearch
import com.axmgel.gearsapp.core.gear.GearResult
import com.axmgel.gearsapp.core.gear.HelicalGear
import com.axmgel.gearsapp.core.gear.SpurGear
import com.axmgel.gearsapp.core.gear.ToothSystem
import com.axmgel.gearsapp.core.report.Reports
import com.axmgel.gearsapp.report.ReportButtons
import com.axmgel.gearsapp.ui.screens.AppScaffold
import kotlin.math.abs

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
    var byDiameter by rememberSaveable { mutableStateOf(false) }
    var diameterKind by rememberSaveable { mutableStateOf(DiameterKind.PITCH) }
    var diameter by rememberSaveable { mutableStateOf("") }
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
    var search by remember { mutableStateOf<ModuleSearch?>(null) }
    // Diámetro que se buscó antes de pasar a "por módulo", para comparar.
    var searched by remember { mutableStateOf<Pair<DiameterKind, Double>?>(null) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    val bevel = kind == GearKind.BEVEL || kind == GearKind.SPIRAL_BEVEL
    val helical = kind == GearKind.HELICAL || kind == GearKind.SPIRAL_BEVEL

    fun system() = ToothSystem(
        pressureAngleDeg = parseDecimal(pressure) ?: error("Escribe el ángulo de presión."),
        addendum = parseDecimal(addendum) ?: error("Escribe el coeficiente de cabeza."),
        dedendum = parseDecimal(dedendum) ?: error("Escribe el coeficiente de pie."),
    )

    fun calculate() {
        errorText = null
        result = runCatching {
            val m = parseDecimal(module) ?: error("Escribe el módulo.")
            val z = teeth.toIntOrNull() ?: error("Escribe el número de dientes.")
            val zMate = mate.toIntOrNull()
            when (kind) {
                GearKind.SPUR -> SpurGear.compute(m, z, zMate, system())
                GearKind.HELICAL -> HelicalGear.compute(m, z, parseDecimal(angle) ?: error("Escribe el ángulo de hélice."), zMate, system())
                GearKind.BEVEL, GearKind.SPIRAL_BEVEL -> BevelGear.compute(
                    module = m,
                    teeth = z,
                    mateTeeth = zMate ?: error("Escribe los dientes del otro engrane; se necesitan para el ángulo del cono."),
                    shaftAngleDeg = parseDecimal(shaftAngle) ?: error("Escribe el ángulo entre ejes."),
                    faceWidth = if (faceWidth.isBlank()) null else parseDecimal(faceWidth) ?: error("El largo del diente no es un número."),
                    spiralAngleDeg = if (kind == GearKind.SPIRAL_BEVEL) parseDecimal(angle) ?: error("Escribe el ángulo de espiral.") else 0.0,
                    system = system(),
                )
            }
        }.onFailure { errorText = it.message ?: "Revisa los datos." }.getOrNull()
    }

    fun findModule() {
        errorText = null
        search = runCatching {
            val d = parseDecimal(diameter) ?: error("Escribe el diámetro.")
            val z = teeth.toIntOrNull() ?: error("Escribe el número de dientes.")
            when (kind) {
                GearKind.SPUR -> ModuleFinder.spur(d, diameterKind, z, system())
                GearKind.HELICAL -> ModuleFinder.helical(d, diameterKind, z, parseDecimal(angle) ?: error("Escribe el ángulo de hélice."), system())
                GearKind.BEVEL, GearKind.SPIRAL_BEVEL -> ModuleFinder.bevel(
                    d, diameterKind, z,
                    mateTeeth = mate.toIntOrNull() ?: error("Escribe los dientes del otro engrane; se necesitan para el ángulo del cono."),
                    shaftAngleDeg = parseDecimal(shaftAngle) ?: error("Escribe el ángulo entre ejes."),
                    system = system(),
                )
            }
        }.onFailure { errorText = it.message ?: "Revisa los datos." }.getOrNull()
    }

    /** Pasa a "por módulo" con el módulo sugerido ya puesto y calcula. */
    fun useModule(s: ModuleSearch, m: Double, exactAngle: Double?) {
        module = fmt(m)
        if (exactAngle != null) angle = fmt(exactAngle, 4)
        searched = s.kind to s.target
        byDiameter = false
        search = null
        calculate()
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !byDiameter,
                    onClick = { byDiameter = false; errorText = null },
                    label = { Text("Por módulo") },
                )
                FilterChip(
                    selected = byDiameter,
                    onClick = { byDiameter = true; errorText = null; result = null },
                    label = { Text("Por diámetro") },
                )
            }

            if (byDiameter) {
                Text(
                    "Escribe el diámetro que mediste o que necesitas y te digo qué módulo es y cuáles módulos " +
                        "normalizados (los de las fresas que sí se consiguen) quedan más cerca.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiameterKind.entries.forEach { k ->
                        FilterChip(selected = diameterKind == k, onClick = { diameterKind = k }, label = { Text(k.label) })
                    }
                }
                NumberInput(
                    diameter, { diameter = it },
                    label = "${diameterKind.label} ${diameterKind.symbol} (mm)" + if (bevel) ", extremo grande" else "",
                    help = if (diameterKind == DiameterKind.OUTSIDE) "Lo que mide la pieza por fuera, de punta a punta de los dientes." else "Diámetro donde se tocan los dos engranes.",
                )
            } else {
                NumberInput(
                    module, { module = it },
                    label = when {
                        bevel -> "Módulo (extremo grande)"
                        helical -> "Módulo normal (el de la fresa)"
                        else -> "Módulo"
                    },
                    help = "Tamaño del diente. Viene grabado en la fresa de módulo.",
                )
            }
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
                if (!byDiameter) {
                    NumberInput(faceWidth, { faceWidth = it }, "Largo del diente (mm, opcional)", "Si lo dejas vacío se usa un tercio de la generatriz.")
                }
            }

            TextButton(onClick = { advanced = !advanced }) {
                Text(if (advanced) "Ocultar forma del diente" else "Forma del diente (ángulo de presión, cabeza y pie)")
            }
            if (advanced) {
                NumberInput(pressure, { pressure = it }, "Ángulo de presión (grados)", "20° es el más común; los engranes antiguos usan 14.5°.")
                NumberInput(addendum, { addendum = it }, "Cabeza ÷ módulo", "Normalmente 1.")
                NumberInput(dedendum, { dedendum = it }, "Pie ÷ módulo", "1.25 en la norma ISO. Algunos libros usan 1.167.")
            }

            if (byDiameter) {
                Button(onClick = { findModule() }, modifier = Modifier.fillMaxWidth()) { Text("Buscar módulo") }
            } else {
                Button(onClick = { searched = null; calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calcular") }
            }
            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (byDiameter) {
                search?.let { s ->
                    ModuleSuggestions(s, helical && !bevel) { m, exactAngle -> useModule(s, m, exactAngle) }
                    StepsCard(s.steps)
                }
            }

            if (!byDiameter) result?.let { r ->
                searched?.let { (k, target) -> ComparisonCard(r, k, target) }
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
                    searched?.let { (k, target) -> add("${k.label} buscado" to "${fmt(target, 3)} mm") }
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

/** Módulos normalizados sugeridos; al tocar uno se calcula el engrane con ese módulo. */
@Composable
private fun ModuleSuggestions(search: ModuleSearch, showExactAngle: Boolean, onUse: (Double, Double?) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Módulo calculado: ${fmt(search.exactModule, 4)}", style = MaterialTheme.typography.titleMedium)
            Text("Módulos normalizados más cercanos. Toca uno para calcular el engrane con él.", style = MaterialTheme.typography.bodySmall)
            search.suggestions.forEach { s ->
                val sign = if (s.difference >= 0) "+" else ""
                OutlinedButton(onClick = { onUse(s.module, null) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Módulo ${fmt(s.module)}" + if (s.series == 1) "  (serie preferida)" else "  (serie 2)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${search.kind.symbol} = ${fmt(s.diameter, 3)} mm  ($sign${fmt(s.difference, 3)} mm)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                val exact = s.exactHelixAngleDeg
                if (showExactAngle && exact != null && abs(s.difference) > 1e-6) {
                    TextButton(onClick = { onUse(s.module, exact) }) {
                        Text("o con módulo ${fmt(s.module)} y β = ${fmt(exact, 3)}° para el diámetro exacto")
                    }
                }
            }
        }
    }
}

/** Compara el diámetro que se buscó con el que sale con el módulo elegido. */
@Composable
private fun ComparisonCard(result: GearResult, kind: DiameterKind, target: Double) {
    val got = result.dims.firstOrNull { it.symbol == kind.symbol }?.value ?: return
    val diff = got - target
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Comparación con lo que buscaste", style = MaterialTheme.typography.titleMedium)
            Text("${kind.label} buscado: ${fmt(target, 3)} mm")
            Text("${kind.label} con este módulo: ${fmt(got, 3)} mm")
            Text(
                "Cambio: ${if (diff >= 0) "+" else ""}${fmt(diff, 3)} mm (${if (diff >= 0) "+" else ""}${fmt(diff / target * 100, 2)} %)",
                fontWeight = FontWeight.SemiBold,
            )
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
