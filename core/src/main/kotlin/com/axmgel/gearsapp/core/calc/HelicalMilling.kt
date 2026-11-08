package com.axmgel.gearsapp.core.calc

import com.axmgel.gearsapp.core.machine.DividingHeadMill
import kotlin.math.PI
import kotlin.math.tan

/**
 * Sincronización helicoidal en la fresadora universal con cabezal divisor
 * (mismo cálculo que el programa Helice_Cabezal.py).
 *
 * Los engranes de cambio unen el husillo de la mesa con el cabezal. Para que la pieza gire
 * una vuelta mientras la mesa avanza un paso de hélice:
 *
 *   (A × C) ÷ (B × D) = paso de la máquina ÷ paso de la hélice
 *
 * donde A y C son conductores y B y D son conducidos.
 */
object HelicalMilling {

    data class Result(
        val machineLeadMm: Double,
        val helixLeadMm: Double,
        val targetRatio: Double,
        val steps: List<CalcStep>,
        val solutions: List<TrainSolution>,
    ) {
        /** Paso que realmente se corta con una combinación. */
        fun leadFor(solution: TrainSolution): Double = machineLeadMm / solution.ratio
    }

    /**
     * Paso de la hélice a partir del diámetro primitivo y el ángulo de la hélice:
     * paso = π × diámetro ÷ tan(ángulo).
     */
    fun leadFromDiameter(pitchDiameterMm: Double, helixAngleDeg: Double): Double {
        require(pitchDiameterMm > 0.0) { "El diámetro debe ser mayor que cero." }
        require(helixAngleDeg > 0.0 && helixAngleDeg < 90.0) { "El ángulo debe estar entre 0 y 90 grados." }
        return PI * pitchDiameterMm / tan(Math.toRadians(helixAngleDeg))
    }

    fun leadSteps(pitchDiameterMm: Double, helixAngleDeg: Double): List<CalcStep> {
        val lead = leadFromDiameter(pitchDiameterMm, helixAngleDeg)
        val t = tan(Math.toRadians(helixAngleDeg))
        return listOf(
            CalcStep(
                "Paso de la hélice",
                "Si desenrollas una vuelta de la hélice queda un triángulo: abajo la circunferencia (π × diámetro) " +
                    "y de lado el paso. Por eso: paso = π × diámetro ÷ tan(ángulo).\n" +
                    "paso = 3.1416 × ${fmt(pitchDiameterMm, 4)} ÷ tan(${fmt(helixAngleDeg, 4)}°) = " +
                    "${fmt(PI * pitchDiameterMm, 4)} ÷ ${fmt(t, 6)} = ${fmt(lead, 4)} mm.",
            ),
        )
    }

    fun compute(
        helixLeadMm: Double,
        machine: DividingHeadMill,
        tolerance: Double = 0.0001,
        maxResults: Int = 200,
    ): Result {
        require(helixLeadMm > 0.0) { "El paso de la hélice debe ser mayor que cero." }
        val machineLead = machine.machineLeadMm
        val target = machineLead / helixLeadMm
        val solutions = GearTrainSearch.search(
            target = target,
            inventory = machine.changeGears,
            tolerance = tolerance,
            relative = true,
            maxResults = maxResults,
        )
        val steps = listOf(
            CalcStep(
                "Paso de la máquina",
                "Es lo que avanza la mesa mientras la pieza da una vuelta con engranes iguales. " +
                    "Relación del cabezal × paso del husillo = ${machine.headRatio} × " +
                    "${fmt(machine.tableLeadScrewPitchMm)} = ${fmt(machineLead)} mm.",
            ),
            CalcStep(
                "Relación de los engranes",
                "Los engranes deben hacer que la pieza dé una vuelta mientras la mesa avanza el paso de la hélice. " +
                    "Relación = paso de la máquina ÷ paso de la hélice = ${fmt(machineLead)} ÷ ${fmt(helixLeadMm)} = " +
                    "${fmt(target, 8)}.\n" +
                    "Si sale menor que 1, los conducidos son más grandes que los conductores; si sale mayor que 1, al revés.",
            ),
            CalcStep(
                "Buscar engranes",
                "Se prueban todas las combinaciones de tus engranes en las que (A × C) ÷ (B × D) da esa relación, " +
                    "con un error menor a ${fmt(tolerance * 100, 4)} %. A y C son conductores; B y D, conducidos. " +
                    "Si eliges un tren de 2 engranes, sólo se usan A y B.",
            ),
            CalcStep(
                "Comprobar",
                "Para cada combinación, el paso que realmente cortas es: paso de la máquina ÷ relación obtenida. " +
                    if (solutions.isEmpty()) {
                        "No se encontró ninguna combinación con tus engranes. Prueba con una tolerancia mayor o agrega engranes."
                    } else {
                        val best = solutions.first()
                        "Con la mejor (${best.train.describe()}): ${fmt(machineLead)} ÷ ${fmt(best.ratio, 8)} = " +
                            "${fmt(machineLead / best.ratio, 4)} mm."
                    },
            ),
        )
        return Result(machineLead, helixLeadMm, target, steps, solutions)
    }
}
