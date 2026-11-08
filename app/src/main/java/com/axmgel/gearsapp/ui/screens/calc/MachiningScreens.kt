package com.axmgel.gearsapp.ui.screens.calc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axmgel.gearsapp.core.calc.CalcStep
import com.axmgel.gearsapp.core.calc.HelicalMilling
import com.axmgel.gearsapp.core.calc.Hobbing
import com.axmgel.gearsapp.core.calc.Indexing
import com.axmgel.gearsapp.core.calc.fmt
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import com.axmgel.gearsapp.core.machine.GearHobber
import com.axmgel.gearsapp.core.report.Reports

private const val NO_MILL = "No tienes ninguna fresadora con cabezal divisor. Agrégala en Configurar máquinas."
private const val NO_HOBBER = "No tienes ninguna generadora. Agrégala en Configurar máquinas."

/** Cabezal divisor → Sincronización helicoidal. */
@Composable
fun HelicalMillingScreen(
    machines: List<DividingHeadMill>?,
    onBack: () -> Unit,
    initialLead: String = "",
    initialAngle: String = "",
) {
    var byDiameter by rememberSaveable { mutableStateOf(false) }
    var lead by rememberSaveable { mutableStateOf(initialLead) }
    var leadAngle by rememberSaveable { mutableStateOf(initialAngle) }
    var diameter by rememberSaveable { mutableStateOf("") }
    var angle by rememberSaveable { mutableStateOf("") }
    var tolerance by rememberSaveable { mutableStateOf("0.01") }

    data class Out(val steps: List<CalcStep>, val result: HelicalMilling.Result, val inputs: List<Pair<String, String>>)

    fun tableStep(angleDeg: Double) = CalcStep(
        "Inclinar la mesa",
        "Gira la mesa de la fresadora ${fmt(angleDeg, 4)}° (el ángulo de la hélice). Hacia la izquierda para hélice " +
            "derecha y hacia la derecha para hélice izquierda, viendo la máquina de frente.",
    )

    CalcScreen(
        title = "Sincronización helicoidal",
        intro = "Calcula los engranes de cambio entre el husillo de la mesa y el cabezal divisor para cortar una hélice " +
            "(dientes helicoidales, ranuras de brocas, etc.).",
        machines = machines,
        noMachineText = NO_MILL,
        onBack = onBack,
        inputs = {
            Row {
                FilterChip(selected = !byDiameter, onClick = { byDiameter = false }, label = { Text("Ya sé el paso") })
                FilterChip(
                    selected = byDiameter,
                    onClick = { byDiameter = true },
                    label = { Text("Diámetro y ángulo") },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (byDiameter) {
                NumberInput(diameter, { diameter = it }, "Diámetro primitivo (mm)", "El diámetro donde se mide el ángulo de la hélice.")
                NumberInput(angle, { angle = it }, "Ángulo de la hélice (grados)", "Inclinación del diente respecto al eje de la pieza.")
            } else {
                NumberInput(lead, { lead = it }, "Paso de la hélice (mm)", "Lo que avanza la hélice en una vuelta completa de la pieza.")
                NumberInput(leadAngle, { leadAngle = it }, "Ángulo de la hélice (grados, opcional)", "Si lo pones, te digo cuánto inclinar la mesa.")
            }
            ToleranceInput(tolerance) { tolerance = it }
        },
        compute = { mill ->
            val tol = toleranceFraction(tolerance)
            val machineInput = listOf("Máquina" to mill.name, "Error permitido" to "$tolerance %")
            if (byDiameter) {
                val d = parseDecimal(diameter) ?: error("Escribe el diámetro.")
                val a = parseDecimal(angle) ?: error("Escribe el ángulo.")
                val l = HelicalMilling.leadFromDiameter(d, a)
                val r = HelicalMilling.compute(l, mill, tol)
                Out(
                    HelicalMilling.leadSteps(d, a) + r.steps + tableStep(a),
                    r,
                    machineInput + listOf("Diámetro primitivo" to "${fmt(d)} mm", "Ángulo de la hélice" to "${fmt(a)}°"),
                )
            } else {
                val l = parseDecimal(lead) ?: error("Escribe el paso de la hélice.")
                val a = parseDecimal(leadAngle)
                val r = HelicalMilling.compute(l, mill, tol)
                Out(
                    r.steps + listOfNotNull(a?.let { tableStep(it) }),
                    r,
                    machineInput + listOfNotNull(a?.let { "Ángulo de la hélice" to "${fmt(it)}°" }),
                )
            }
        },
        results = { out ->
            StepsCard(out.steps)
            TrainResults(out.result.solutions, extra = { "Paso que resulta: ${fmt(out.result.leadFor(it), 4)} mm" })
        },
        report = { _, out -> Reports.helicalMilling(out.inputs, out.steps, out.result) },
    )
}

/** Cabezal divisor → Encontrador de puntos para divisiones. */
@Composable
fun IndexingScreen(machines: List<DividingHeadMill>?, onBack: () -> Unit, initialDivisions: String = "") {
    var divisions by rememberSaveable { mutableStateOf(initialDivisions) }

    CalcScreen(
        title = "Encontrador de puntos",
        intro = "Dice cuántas vueltas y cuántos agujeros mover la manivela del cabezal en cada división. " +
            "Si tus platos no alcanzan, calcula la división diferencial.",
        machines = machines,
        noMachineText = NO_MILL,
        onBack = onBack,
        inputs = {
            NumberInput(divisions, { divisions = it.filter(Char::isDigit) }, "Número de divisiones", "Por ejemplo, el número de dientes del engrane.", decimal = false)
        },
        compute = { mill ->
            val z = divisions.toIntOrNull() ?: error("Escribe el número de divisiones.")
            require(z >= 2) { "El número de divisiones debe ser 2 o más." }
            Indexing.compute(z, mill)
        },
        report = { mill, r -> Reports.indexing(listOf("Máquina" to mill.name), r) },
        results = { r ->
            StepsCard(r.steps)
            if (r.simple.isNotEmpty()) {
                Text("Opciones con división simple", style = MaterialTheme.typography.titleMedium)
                r.simple.forEach { m ->
                    Card(Modifier.fillMaxWidth()) { Text(m.describe(), Modifier.padding(12.dp)) }
                }
            }
            if (r.differential.isNotEmpty()) {
                Text("Opciones con división diferencial", style = MaterialTheme.typography.titleMedium)
                r.differential.forEach { d ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Z' = ${d.approxDivisions}: ${d.train.describe()}", style = MaterialTheme.typography.titleSmall)
                            Text("Manivela: ${d.move.describe()}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (d.plateSameDirection) "Plato en el mismo sentido que la manivela" else "Plato en sentido contrario a la manivela",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
    )
}

/** Generadora → Tren de hélice. */
@Composable
fun HobberHelixScreen(
    machines: List<GearHobber>?,
    onBack: () -> Unit,
    initialModule: String = "",
    initialAngle: String = "",
) {
    var module by rememberSaveable { mutableStateOf(initialModule) }
    var starts by rememberSaveable { mutableStateOf("1") }
    var angle by rememberSaveable { mutableStateOf(initialAngle) }
    var tolerance by rememberSaveable { mutableStateOf("0.01") }

    CalcScreen(
        title = "Tren de hélice",
        intro = "Calcula los engranes del tren de hélice (diferencial) de la generadora para cortar dientes helicoidales.",
        machines = machines,
        noMachineText = NO_HOBBER,
        onBack = onBack,
        inputs = {
            NumberInput(module, { module = it }, "Módulo normal", "El módulo medido perpendicular al diente.")
            NumberInput(starts, { starts = it.filter(Char::isDigit) }, "Entradas de la fresa madre", "Casi siempre 1. Viene grabado en la fresa.", decimal = false)
            NumberInput(angle, { angle = it }, "Ángulo de hélice β (grados)", "Inclinación del diente respecto al eje de la pieza.")
            ToleranceInput(tolerance) { tolerance = it }
        },
        compute = { hob ->
            val m = parseDecimal(module) ?: error("Escribe el módulo.")
            val k = starts.toIntOrNull() ?: error("Escribe las entradas de la fresa.")
            val b = parseDecimal(angle) ?: error("Escribe el ángulo de hélice.")
            Hobbing.helixTrain(m, k, b, hob, toleranceFraction(tolerance))
        },
        report = { hob, r ->
            Reports.hobbing(
                "Tren de hélice (generadora)",
                listOf(
                    "Máquina" to hob.name, "Módulo normal" to module, "Entradas de la fresa" to starts,
                    "Ángulo de hélice" to "$angle°", "Error permitido" to "$tolerance %",
                ),
                r,
            )
        },
        results = { r ->
            StepsCard(r.steps)
            TrainResults(r.solutions)
        },
    )
}

/** Generadora → Sincronización (número de divisiones). */
@Composable
fun HobberSyncScreen(machines: List<GearHobber>?, onBack: () -> Unit) {
    var teeth by rememberSaveable { mutableStateOf("") }
    var starts by rememberSaveable { mutableStateOf("1") }
    var constant by rememberSaveable { mutableStateOf("") }
    var tolerance by rememberSaveable { mutableStateOf("0.01") }

    CalcScreen(
        title = "Sincronización de la generadora",
        intro = "Calcula el tren de división: los engranes que hacen girar la pieza al ritmo de la fresa para cortar " +
            "el número de dientes que quieres.",
        machines = machines,
        noMachineText = NO_HOBBER,
        onBack = onBack,
        inputs = { hob ->
            NumberInput(teeth, { teeth = it.filter(Char::isDigit) }, "Número de dientes a cortar", decimal = false)
            NumberInput(starts, { starts = it.filter(Char::isDigit) }, "Entradas de la fresa madre", "Casi siempre 1.", decimal = false)
            val saved = hob.divisionConstant
            if (saved != null) {
                Text("Constante de división de esta máquina: ${fmt(saved)}", style = MaterialTheme.typography.bodyMedium)
            } else {
                NumberInput(
                    constant, { constant = it }, "Constante de división",
                    "Es la relación entre el cabezal divisor de la generadora y el chuck que sujeta la pieza (vueltas del sinfín por una vuelta del chuck). Guárdala en Configurar máquinas para no escribirla cada vez.",
                )
            }
            ToleranceInput(tolerance) { tolerance = it }
        },
        compute = { hob ->
            val z = teeth.toIntOrNull() ?: error("Escribe el número de dientes.")
            val k = starts.toIntOrNull() ?: error("Escribe las entradas de la fresa.")
            val c = hob.divisionConstant ?: parseDecimal(constant) ?: error("Escribe la constante de división.")
            Hobbing.indexTrain(z, k, c, hob, toleranceFraction(tolerance))
        },
        report = { hob, r ->
            Reports.hobbing(
                "Sincronización (generadora)",
                listOf(
                    "Máquina" to hob.name, "Dientes" to teeth, "Entradas de la fresa" to starts,
                    "Constante de división" to (hob.divisionConstant?.let { fmt(it) } ?: constant),
                    "Error permitido" to "$tolerance %",
                ),
                r,
            )
        },
        results = { r ->
            StepsCard(r.steps)
            TrainResults(r.solutions)
        },
    )
}
