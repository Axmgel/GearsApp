package com.axmgel.gearsapp.core.calc

import com.axmgel.gearsapp.core.machine.ChangeGear
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import kotlin.math.abs

/**
 * Encontrador de puntos para divisiones con el cabezal divisor.
 *
 * División simple: vueltas de la manivela = relación del cabezal ÷ número de divisiones.
 * Si no hay un plato que sirva, división diferencial: se divide como si fueran Z' divisiones
 * y unos engranes de cambio hacen girar el plato para corregir la diferencia.
 */
object Indexing {

    /** Movimiento de la manivela: vueltas completas más [holes] espacios en la circunferencia de [circle] agujeros. */
    data class CrankMove(val turns: Int, val holes: Int, val circle: Int?, val plate: Int?) {
        fun describe(): String = when {
            holes == 0 -> "$turns vueltas completas"
            turns == 0 -> "$holes espacios en la circunferencia de $circle agujeros (plato $plate)"
            else -> "$turns vueltas y $holes espacios en la circunferencia de $circle agujeros (plato $plate)"
        }
    }

    data class Differential(
        val approxDivisions: Int,
        val move: CrankMove,
        val train: GearTrain,
        /** true: el plato gira en el mismo sentido que la manivela. */
        val plateSameDirection: Boolean,
    )

    data class Result(
        val divisions: Int,
        val steps: List<CalcStep>,
        val simple: List<CrankMove>,
        val differential: List<Differential>,
    )

    /** Todas las formas de hacer la división simple con los platos disponibles. */
    fun simple(divisions: Int, headRatio: Int, plates: List<List<Int>>): List<CrankMove> {
        require(divisions > 0) { "El número de divisiones debe ser mayor que cero." }
        val turns = headRatio / divisions
        val rest = headRatio % divisions
        if (rest == 0) return listOf(CrankMove(turns, 0, null, null))
        val out = mutableListOf<CrankMove>()
        plates.forEachIndexed { p, circles ->
            for (circle in circles) {
                if ((rest.toLong() * circle) % divisions == 0L) {
                    out += CrankMove(turns, (rest.toLong() * circle / divisions).toInt(), circle, p + 1)
                }
            }
        }
        return out
    }

    /**
     * División diferencial. Busca números aproximados Z' cercanos a Z que sí se pueden dividir
     * con los platos, y engranes que den exactamente:
     *   (conductores) ÷ (conducidos) = relación del cabezal × |Z' − Z| ÷ Z'
     */
    fun differential(
        divisions: Int,
        headRatio: Int,
        plates: List<List<Int>>,
        gears: List<ChangeGear>,
        maxApprox: Int = 3,
        searchRange: Int = 20,
    ): List<Differential> {
        val out = mutableListOf<Differential>()
        var approxFound = 0
        for (delta in 1..searchRange) {
            for (approx in listOf(divisions - delta, divisions + delta)) {
                if (approx < 2) continue
                val moves = simple(approx, headRatio, plates)
                if (moves.isEmpty()) continue
                val num = headRatio.toLong() * abs(approx - divisions)
                val den = approx.toLong()
                val trains = exactTrains(num, den, gears)
                if (trains.isEmpty()) continue
                // Se prefiere el movimiento con la circunferencia de menos agujeros (más fácil de contar).
                val move = moves.minBy { it.circle ?: 0 }
                trains.forEach { out += Differential(approx, move, it, plateSameDirection = approx > divisions) }
                approxFound++
                if (approxFound >= maxApprox) return out
            }
        }
        return out
    }

    /** Trenes de 2 o 4 engranes que dan exactamente num ÷ den. Primero los de 2 engranes. */
    private fun exactTrains(num: Long, den: Long, gears: List<ChangeGear>, max: Int = 10): List<GearTrain> {
        val stock = gears.filter { it.quantity > 0 }.groupBy { it.teeth }.mapValues { (_, g) -> g.sumOf { it.quantity } }
        val teeth = stock.keys.sorted()
        fun available(vararg t: Int): Boolean = t.groupBy { it }.all { (k, v) -> v.size <= (stock[k] ?: 0) }

        val out = mutableListOf<GearTrain>()
        for (a in teeth) for (b in teeth) {
            if (a.toLong() * den == b.toLong() * num && available(a, b)) out += GearTrain(a, b)
        }
        for (a in teeth) for (b in teeth) for (c in teeth) {
            if (c < a) continue
            for (d in teeth) {
                if (d < b) continue
                if (a.toLong() * c * den == b.toLong() * d * num && available(a, b, c, d)) {
                    out += GearTrain(a, b, c, d)
                    if (out.size >= max) return out
                }
            }
        }
        return out
    }

    fun compute(divisions: Int, machine: DividingHeadMill): Result {
        val k = machine.headRatio
        val simple = simple(divisions, k, machine.indexPlates)
        val differential = if (simple.isEmpty()) {
            differential(divisions, k, machine.indexPlates, machine.changeGears)
        } else {
            emptyList()
        }
        val turns = k / divisions
        val rest = k % divisions
        val g = gcd(rest, divisions)

        val steps = mutableListOf(
            CalcStep(
                "Vueltas de la manivela",
                "Con $k vueltas de la manivela la pieza da una vuelta completa. Para $divisions divisiones, " +
                    "en cada división das: $k ÷ $divisions vueltas" +
                    if (rest == 0) " = $turns vueltas completas." else " = $turns vueltas completas y sobran $rest/$divisions de vuelta.",
            ),
        )
        if (rest != 0) {
            steps += CalcStep(
                "Fracción de vuelta",
                "La fracción $rest/$divisions se simplifica a ${rest / g}/${divisions / g}. Sirve cualquier circunferencia " +
                    "cuyo número de agujeros sea múltiplo de ${divisions / g}: el número de espacios es " +
                    "(agujeros de la circunferencia) × ${rest / g} ÷ ${divisions / g}.",
            )
        }
        if (simple.isNotEmpty() && rest != 0) {
            val m = simple.first()
            steps += CalcStep(
                "Resultado",
                "Por ejemplo, en la circunferencia de ${m.circle}: ${m.circle} × ${rest / g} ÷ ${divisions / g} = ${m.holes} espacios. " +
                    "Ajusta el compás (las tijeras) del plato a ${m.holes} espacios, sin contar el agujero donde está el perno.",
            )
        }
        if (simple.isEmpty()) {
            steps += CalcStep(
                "No hay plato para división simple",
                "Ninguna circunferencia de tus platos es múltiplo de ${divisions / g}. Se usa división diferencial: " +
                    "se divide como si fueran Z' divisiones (un número cercano que sí se puede) y unos engranes entre el " +
                    "husillo del cabezal y el plato lo hacen girar poco a poco para corregir la diferencia. " +
                    "Hay que soltar el seguro del plato.",
            )
            val first = differential.firstOrNull()
            if (first == null) {
                steps += CalcStep(
                    "Sin solución",
                    "No se encontraron engranes exactos con tu inventario para números cercanos. Agrega engranes o platos.",
                )
            } else {
                val z1 = first.approxDivisions
                steps += CalcStep(
                    "Número aproximado Z' = $z1",
                    "Manivela: $k ÷ $z1 → ${first.move.describe()}.",
                )
                steps += CalcStep(
                    "Engranes diferenciales",
                    "Relación = $k × (Z' − Z) ÷ Z' = $k × ($z1 − $divisions) ÷ $z1 = ${k * (z1 - divisions)}/$z1. " +
                        "Con ${first.train.describe()}: (conductores) ÷ (conducidos) = ${first.train.drivers}/${first.train.driven}, " +
                        "que es la misma relación.",
                )
                steps += CalcStep(
                    "Sentido de giro del plato",
                    if (first.plateSameDirection) {
                        "Como Z' es mayor que Z, el plato debe girar en el mismo sentido que la manivela."
                    } else {
                        "Como Z' es menor que Z, el plato debe girar en sentido contrario a la manivela."
                    } + " Pon los engranes locos (intermedios) que tu cabezal necesite para lograr ese sentido.",
                )
            }
        }
        return Result(divisions, steps, simple, differential)
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd(b, a % b)
}
