package com.axmgel.gearsapp.core.calc

import com.axmgel.gearsapp.core.machine.GearHobber
import kotlin.math.sin

/** Cálculos para la generadora de engranajes (fresa madre). */
object Hobbing {

    data class Result(
        val targetRatio: Double,
        val steps: List<CalcStep>,
        val solutions: List<TrainSolution>,
    )

    /**
     * Tren de hélice (mismo cálculo que el programa Generadora.py):
     *   (a × c) ÷ (b × d) = constante de hélice × sen(β) ÷ (módulo normal × entradas de la fresa)
     */
    fun helixTrain(
        normalModule: Double,
        hobStarts: Int,
        helixAngleDeg: Double,
        machine: GearHobber,
        tolerance: Double = 0.0001,
        maxResults: Int = 200,
    ): Result {
        require(normalModule > 0.0) { "El módulo debe ser mayor que cero." }
        require(hobStarts > 0) { "Las entradas de la fresa deben ser 1 o más." }
        require(helixAngleDeg > 0.0 && helixAngleDeg < 90.0) { "El ángulo debe estar entre 0 y 90 grados." }
        val s = sin(Math.toRadians(helixAngleDeg))
        val target = machine.helixConstant * s / (normalModule * hobStarts)
        val solutions = GearTrainSearch.search(target, machine.changeGears, tolerance, relative = true, maxResults = maxResults)
        val steps = listOf(
            CalcStep(
                "Seno del ángulo de hélice",
                "sen(${fmt(helixAngleDeg, 4)}°) = ${fmt(s, 6)}. Mientras más inclinado el diente, más grande el seno " +
                    "y más tiene que girar de más la pieza.",
            ),
            CalcStep(
                "Relación del tren de hélice",
                "Relación = constante de la máquina × sen(β) ÷ (módulo × entradas de la fresa)\n" +
                    "= ${fmt(machine.helixConstant)} × ${fmt(s, 6)} ÷ (${fmt(normalModule)} × $hobStarts) = ${fmt(target, 8)}.",
            ),
            CalcStep(
                "Buscar engranes",
                "Se prueban tus engranes para que (a × c) ÷ (b × d) dé esa relación con un error menor a " +
                    "${fmt(tolerance * 100, 4)} %. a y c son conductores; b y d, conducidos. " +
                    resultLine(solutions),
            ),
        )
        return Result(target, steps, solutions)
    }

    /**
     * Sincronización (tren de división):
     *   (a × c) ÷ (b × d) = constante de división × entradas de la fresa ÷ número de dientes
     */
    fun indexTrain(
        teeth: Int,
        hobStarts: Int,
        divisionConstant: Double,
        machine: GearHobber,
        tolerance: Double = 0.0001,
        maxResults: Int = 200,
    ): Result {
        require(teeth > 0) { "El número de dientes debe ser mayor que cero." }
        require(hobStarts > 0) { "Las entradas de la fresa deben ser 1 o más." }
        require(divisionConstant > 0.0) { "La constante de división debe ser mayor que cero." }
        val target = divisionConstant * hobStarts / teeth
        val solutions = GearTrainSearch.search(target, machine.changeGears, tolerance, relative = true, maxResults = maxResults)
        val steps = listOf(
            CalcStep(
                "Qué hace este tren",
                "Por cada vuelta de la fresa, la pieza debe girar lo que ocupan tantos dientes como entradas tiene la fresa. " +
                    "Así, cuando la pieza da una vuelta completa, la fresa ya cortó los $teeth dientes.",
            ),
            CalcStep(
                "Relación del tren de división",
                "Relación = constante de división × entradas ÷ dientes = ${fmt(divisionConstant)} × $hobStarts ÷ $teeth = " +
                    "${fmt(target, 8)}.",
            ),
            CalcStep(
                "Buscar engranes",
                "Se prueban tus engranes para que (a × c) ÷ (b × d) dé esa relación con un error menor a " +
                    "${fmt(tolerance * 100, 4)} %. a y c son conductores; b y d, conducidos. " +
                    resultLine(solutions),
            ),
        )
        return Result(target, steps, solutions)
    }

    private fun resultLine(solutions: List<TrainSolution>): String =
        if (solutions.isEmpty()) {
            "No se encontró ninguna combinación con tus engranes. Prueba con una tolerancia mayor o agrega engranes."
        } else {
            "La mejor es ${solutions.first().train.describe()} (relación ${fmt(solutions.first().ratio, 8)})."
        }
}
