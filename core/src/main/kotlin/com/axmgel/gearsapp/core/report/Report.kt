package com.axmgel.gearsapp.core.report

import com.axmgel.gearsapp.core.calc.CalcStep
import com.axmgel.gearsapp.core.calc.HelicalMilling
import com.axmgel.gearsapp.core.calc.Hobbing
import com.axmgel.gearsapp.core.calc.Indexing
import com.axmgel.gearsapp.core.calc.TrainSolution
import com.axmgel.gearsapp.core.calc.fmt
import com.axmgel.gearsapp.core.gear.GearResult

/**
 * Hoja de cálculo para imprimir: datos de entrada, resultados y explicación.
 * @property name nombre que el usuario le pone a la pieza (por ejemplo "Piñón de la caja de avances").
 * @property comments notas libres del usuario.
 */
data class Report(
    val title: String,
    val inputs: List<Pair<String, String>>,
    val sections: List<ReportSection>,
    val name: String = "",
    val comments: String = "",
) {
    /** Lo que se imprime: con nombre y comentarios, y sin la explicación "Cómo se calcula". */
    fun forPrint(name: String, comments: String): Report = copy(
        name = name.trim(),
        comments = comments.trim(),
        sections = sections.filterNot { it.heading == Reports.STEPS_HEADING },
    )
}

data class ReportSection(val heading: String, val lines: List<String>)

/** Arma los reportes de cada cálculo. */
object Reports {

    /** Máximo de combinaciones de engranes que se imprimen. */
    const val MAX_TRAINS = 20

    const val STEPS_HEADING = "Cómo se calcula"

    private fun stepsSection(steps: List<CalcStep>) = ReportSection(
        STEPS_HEADING,
        steps.mapIndexed { i, s -> "${i + 1}. ${s.title}: ${s.detail}" },
    )

    private fun trainsSection(solutions: List<TrainSolution>, extra: (TrainSolution) -> String? = { null }): ReportSection {
        val lines = if (solutions.isEmpty()) {
            listOf("No se encontraron combinaciones con los engranes de la máquina.")
        } else {
            val head = if (solutions.size > MAX_TRAINS) {
                listOf("Se muestran las $MAX_TRAINS mejores de ${solutions.size}. A y C conductores; B y D conducidos.")
            } else {
                listOf("A y C conductores; B y D conducidos.")
            }
            head + solutions.take(MAX_TRAINS).mapIndexed { i, s ->
                val more = extra(s)?.let { " · $it" } ?: ""
                "${i + 1}) ${s.train.describe()} · relación ${fmt(s.ratio, 8)} · error ${fmt(s.relativeError * 100, 5)} %$more"
            }
        }
        return ReportSection("Combinaciones de engranes", lines)
    }

    fun gear(title: String, inputs: List<Pair<String, String>>, result: GearResult) = Report(
        title = title,
        inputs = inputs,
        sections = listOf(
            ReportSection(
                "Medidas",
                result.dims.map { d -> "${d.name} (${d.symbol}): ${fmt(d.value, if (d.unit == "°") 4 else 3)} ${d.unit}".trimEnd() },
            ),
            stepsSection(result.steps),
        ),
    )

    fun helicalMilling(inputs: List<Pair<String, String>>, steps: List<CalcStep>, result: HelicalMilling.Result) = Report(
        title = "Sincronización helicoidal (cabezal divisor)",
        inputs = inputs + listOf(
            "Paso de la máquina" to "${fmt(result.machineLeadMm)} mm",
            "Paso de la hélice" to "${fmt(result.helixLeadMm, 4)} mm",
            "Relación buscada" to fmt(result.targetRatio, 8),
        ),
        sections = listOf(
            trainsSection(result.solutions) { "paso ${fmt(result.leadFor(it), 4)} mm" },
            stepsSection(steps),
        ),
    )

    fun indexing(inputs: List<Pair<String, String>>, result: Indexing.Result): Report {
        val sections = mutableListOf<ReportSection>()
        if (result.simple.isNotEmpty()) {
            sections += ReportSection("División simple", result.simple.map { it.describe() })
        }
        if (result.differential.isNotEmpty()) {
            sections += ReportSection(
                "División diferencial",
                result.differential.map { d ->
                    "Z' = ${d.approxDivisions}: ${d.train.describe()} · manivela: ${d.move.describe()} · plato " +
                        if (d.plateSameDirection) "en el mismo sentido que la manivela" else "en sentido contrario a la manivela"
                },
            )
        }
        sections += stepsSection(result.steps)
        return Report(
            title = "Encontrador de puntos (cabezal divisor)",
            inputs = inputs + ("Divisiones" to result.divisions.toString()),
            sections = sections,
        )
    }

    fun hobbing(title: String, inputs: List<Pair<String, String>>, result: Hobbing.Result) = Report(
        title = title,
        inputs = inputs + ("Relación buscada" to fmt(result.targetRatio, 8)),
        sections = listOf(trainsSection(result.solutions), stepsSection(result.steps)),
    )

    /** Versión en texto plano, por si se quiere copiar o mandar por mensaje. */
    fun toText(report: Report): String = buildString {
        if (report.name.isNotEmpty()) appendLine(report.name)
        appendLine(report.title)
        appendLine()
        report.inputs.forEach { (k, v) -> appendLine("$k: $v") }
        report.sections.forEach { s ->
            appendLine()
            appendLine(s.heading)
            s.lines.forEach { appendLine(it) }
        }
        if (report.comments.isNotEmpty()) {
            appendLine()
            appendLine("Comentarios")
            appendLine(report.comments)
        }
    }.trimEnd()
}
