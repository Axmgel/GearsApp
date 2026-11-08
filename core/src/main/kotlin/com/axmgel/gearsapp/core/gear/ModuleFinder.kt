package com.axmgel.gearsapp.core.gear

import com.axmgel.gearsapp.core.calc.CalcStep
import com.axmgel.gearsapp.core.calc.fmt
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Módulos normalizados (ISO 54 / DIN 780). La serie 1 es la preferida. */
object StandardModules {
    val series1 = listOf(
        0.5, 0.6, 0.8, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 16.0, 20.0, 25.0, 32.0, 40.0, 50.0,
    )
    val series2 = listOf(
        0.55, 0.7, 0.9, 1.125, 1.375, 1.75, 2.25, 2.75, 3.5, 4.5, 5.5, 7.0, 9.0, 11.0, 14.0, 18.0, 22.0, 28.0, 36.0, 45.0,
    )

    /** Los [count] módulos normalizados más cercanos a [module], primero los más cercanos. */
    fun nearest(module: Double, count: Int = 4): List<Pair<Double, Int>> =
        (series1.map { it to 1 } + series2.map { it to 2 })
            .sortedWith(compareBy({ abs(it.first - module) }, { it.second }))
            .take(count)
}

/** Qué diámetro conoce el usuario. */
enum class DiameterKind(val label: String, val symbol: String) {
    PITCH("Diámetro primitivo", "Dp"),
    OUTSIDE("Diámetro exterior", "De"),
}

/**
 * Un módulo normalizado sugerido.
 * @property diameter diámetro (del mismo tipo que el buscado) que sale con este módulo.
 * @property difference diámetro con este módulo − diámetro buscado.
 * @property exactHelixAngleDeg en helicoidales, el ángulo de hélice que da exactamente el diámetro buscado
 *   con este módulo (null si no existe o no aplica).
 */
data class ModuleSuggestion(
    val module: Double,
    val series: Int,
    val diameter: Double,
    val difference: Double,
    val exactHelixAngleDeg: Double? = null,
)

data class ModuleSearch(
    val exactModule: Double,
    val kind: DiameterKind,
    val target: Double,
    val suggestions: List<ModuleSuggestion>,
    val steps: List<CalcStep>,
)

/**
 * Busca el módulo a partir de un diámetro medido o deseado, y sugiere módulos normalizados cercanos.
 * Así se puede identificar un engrane que ya existe o ajustar un diseño a una fresa que sí se consigue.
 */
object ModuleFinder {

    private fun rad(d: Double) = Math.toRadians(d)

    /** Engrane recto. Dp = m·Z ; De = m·(Z + 2·cabeza). */
    fun spur(diameter: Double, kind: DiameterKind, teeth: Int, system: ToothSystem = ToothSystem()): ModuleSearch {
        check(diameter, teeth)
        val factor = when (kind) {
            DiameterKind.PITCH -> teeth.toDouble()
            DiameterKind.OUTSIDE -> teeth + 2 * system.addendum
        }
        val formula = when (kind) {
            DiameterKind.PITCH -> "m = Dp ÷ Z = ${fmt(diameter, 4)} ÷ $teeth"
            DiameterKind.OUTSIDE -> "m = De ÷ (Z + ${fmt(2 * system.addendum)}) = ${fmt(diameter, 4)} ÷ ${fmt(factor)}"
        }
        return build(diameter, kind, factor, formula, null)
    }

    /**
     * Engrane helicoidal con ángulo de hélice conocido.
     * Dp = mn·Z ÷ cos β ; De = Dp + 2·cabeza·mn.
     * Para cada módulo sugerido también calcula el ángulo que daría exactamente el diámetro buscado.
     */
    fun helical(
        diameter: Double,
        kind: DiameterKind,
        teeth: Int,
        helixAngleDeg: Double,
        system: ToothSystem = ToothSystem(),
    ): ModuleSearch {
        check(diameter, teeth)
        require(helixAngleDeg > 0.0 && helixAngleDeg < 60.0) { "El ángulo de hélice debe estar entre 0 y 60 grados." }
        val c = cos(rad(helixAngleDeg))
        val factor = when (kind) {
            DiameterKind.PITCH -> teeth / c
            DiameterKind.OUTSIDE -> teeth / c + 2 * system.addendum
        }
        val formula = when (kind) {
            DiameterKind.PITCH -> "mn = Dp × cos β ÷ Z = ${fmt(diameter, 4)} × ${fmt(c, 6)} ÷ $teeth"
            DiameterKind.OUTSIDE -> "mn = De ÷ (Z ÷ cos β + ${fmt(2 * system.addendum)}) = ${fmt(diameter, 4)} ÷ ${fmt(factor, 6)}"
        }
        val exactAngle = { mn: Double ->
            // Dp que se necesita con este módulo; luego cos β = mn·Z ÷ Dp.
            val dp = if (kind == DiameterKind.PITCH) diameter else diameter - 2 * system.addendum * mn
            val cosB = mn * teeth / dp
            if (dp > 0 && cosB > 0.5 && cosB < 1.0) Math.toDegrees(acos(cosB)) else null
        }
        return build(diameter, kind, factor, formula, exactAngle)
    }

    /**
     * Engrane cónico (medidas en el extremo grande). El ángulo del cono no depende del módulo.
     * Dp = m·Z ; De = m·(Z + 2·cabeza·cos δ).
     */
    fun bevel(
        diameter: Double,
        kind: DiameterKind,
        teeth: Int,
        mateTeeth: Int,
        shaftAngleDeg: Double = 90.0,
        system: ToothSystem = ToothSystem(),
    ): ModuleSearch {
        check(diameter, teeth)
        require(mateTeeth >= 6) { "El otro engrane debe tener al menos 6 dientes." }
        val sigma = rad(shaftAngleDeg)
        val delta = atan2(sin(sigma), mateTeeth.toDouble() / teeth + cos(sigma))
        val factor = when (kind) {
            DiameterKind.PITCH -> teeth.toDouble()
            DiameterKind.OUTSIDE -> teeth + 2 * system.addendum * cos(delta)
        }
        val formula = when (kind) {
            DiameterKind.PITCH -> "m = Dp ÷ Z = ${fmt(diameter, 4)} ÷ $teeth"
            DiameterKind.OUTSIDE ->
                "El cono es de ${fmt(Math.toDegrees(delta), 4)}°. m = De ÷ (Z + ${fmt(2 * system.addendum)} × cos δ) = " +
                    "${fmt(diameter, 4)} ÷ ${fmt(factor, 6)}"
        }
        return build(diameter, kind, factor, formula, null)
    }

    private fun check(diameter: Double, teeth: Int) {
        require(diameter > 0.0) { "El diámetro debe ser mayor que cero." }
        require(teeth >= 6) { "El engrane debe tener al menos 6 dientes." }
    }

    /** En todos los casos el diámetro es proporcional al módulo: diámetro = módulo × [factor]. */
    private fun build(
        diameter: Double,
        kind: DiameterKind,
        factor: Double,
        formula: String,
        exactAngle: ((Double) -> Double?)?,
    ): ModuleSearch {
        val exact = diameter / factor
        val suggestions = StandardModules.nearest(exact).map { (m, series) ->
            val d = m * factor
            ModuleSuggestion(m, series, d, d - diameter, exactAngle?.invoke(m))
        }
        val best = suggestions.first()
        val steps = listOf(
            CalcStep("Módulo que da ese diámetro", "$formula = ${fmt(exact, 4)}."),
            CalcStep(
                "Módulo normalizado",
                "Las fresas sólo vienen en ciertos módulos. El más cercano es ${fmt(best.module)} " +
                    "(serie ${best.series}${if (best.series == 1) ", la preferida" else ""}). Con él, ${kind.symbol} = " +
                    "${fmt(best.module)} × ${fmt(factor, 6)} = ${fmt(best.diameter, 4)} mm, " +
                    "${if (best.difference >= 0) "+" else ""}${fmt(best.difference, 3)} mm contra lo que buscaste.",
            ),
        )
        return ModuleSearch(exact, kind, diameter, suggestions, steps)
    }
}
