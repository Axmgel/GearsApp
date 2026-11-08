package com.axmgel.gearsapp.core.calc

import com.axmgel.gearsapp.core.machine.ChangeGear
import java.util.Locale
import kotlin.math.abs

/**
 * Un tren de engranes de cambio.
 * Con 2 engranes: A conduce a B. Con 4 engranes: A conduce a B, y C (en el mismo eje que B) conduce a D.
 * La relación es (conductores) ÷ (conducidos) = (A × C) ÷ (B × D).
 */
data class GearTrain(val a: Int, val b: Int, val c: Int? = null, val d: Int? = null) {
    val isCompound: Boolean get() = c != null && d != null
    val drivers: Long get() = a.toLong() * (c ?: 1)
    val driven: Long get() = b.toLong() * (d ?: 1)
    val ratio: Double get() = drivers.toDouble() / driven.toDouble()

    fun describe(): String =
        if (isCompound) "A=$a  B=$b  C=$c  D=$d" else "A=$a  B=$b"
}

/**
 * Una combinación encontrada.
 * @property relativeError diferencia entre la relación que sale y la buscada, como fracción de la buscada.
 */
data class TrainSolution(val train: GearTrain, val ratio: Double, val relativeError: Double)

/** Paso de una explicación: un título corto y el detalle con los números ya sustituidos. */
data class CalcStep(val title: String, val detail: String)

/** Busca combinaciones de engranes de cambio que den una relación, usando sólo los engranes del taller. */
object GearTrainSearch {

    /**
     * @param target relación buscada: (conductores) ÷ (conducidos).
     * @param inventory engranes disponibles con sus cantidades. Nunca se usa un engrane más veces de las que hay.
     * @param tolerance error permitido. Si [relative] es true es una fracción de la relación buscada
     *   (0.0001 = 0.01 %); si es false es una diferencia absoluta en la relación.
     * @param simple incluir trenes de 2 engranes.
     * @param compound incluir trenes de 4 engranes.
     * @param uniqueOnly no repetir el mismo tren con los conductores (o los conducidos) intercambiados;
     *   A×C es igual que C×A, así que sólo se muestra uno.
     * @param maxResults cuántas combinaciones devolver como máximo (las de menor error primero).
     */
    fun search(
        target: Double,
        inventory: List<ChangeGear>,
        tolerance: Double = 0.0001,
        relative: Boolean = true,
        simple: Boolean = true,
        compound: Boolean = true,
        uniqueOnly: Boolean = true,
        maxResults: Int = 200,
    ): List<TrainSolution> {
        require(target > 0.0) { "La relación buscada debe ser mayor que cero." }
        val stock = inventory.filter { it.teeth > 0 && it.quantity > 0 }
            .groupBy { it.teeth }.mapValues { (_, g) -> g.sumOf { it.quantity } }
        val teeth = stock.keys.sorted().toIntArray()
        val qty = IntArray(teeth.size) { stock.getValue(teeth[it]) }
        val limit = if (relative) tolerance * target else tolerance

        val found = ArrayList<TrainSolution>()
        fun accept(train: GearTrain) {
            val r = train.ratio
            if (abs(r - target) <= limit) found += TrainSolution(train, r, (r - target) / target)
        }

        if (simple) {
            for (i in teeth.indices) for (j in teeth.indices) {
                if (i == j && qty[i] < 2) continue
                accept(GearTrain(teeth[i], teeth[j]))
            }
        }
        if (compound) {
            val used = IntArray(teeth.size)
            for (ia in teeth.indices) {
                used[ia]++
                for (ib in teeth.indices) {
                    if (used[ib] >= qty[ib]) continue
                    used[ib]++
                    for (ic in teeth.indices) {
                        if (uniqueOnly && ic < ia) continue
                        if (used[ic] >= qty[ic]) continue
                        used[ic]++
                        // Se acota D con la relación para no revisar combinaciones imposibles.
                        val partial = teeth[ia].toDouble() * teeth[ic] / teeth[ib]
                        for (id in teeth.indices) {
                            if (uniqueOnly && id < ib) continue
                            if (used[id] >= qty[id]) continue
                            val r = partial / teeth[id]
                            if (abs(r - target) <= limit) {
                                accept(GearTrain(teeth[ia], teeth[ib], teeth[ic], teeth[id]))
                            }
                        }
                        used[ic]--
                    }
                    used[ib]--
                }
                used[ia]--
            }
        }
        return found.sortedWith(compareBy({ abs(it.relativeError) }, { it.train.isCompound }))
            .take(maxResults)
    }
}

/** Formato de números para las explicaciones: punto decimal y sin ceros sobrantes. */
fun fmt(value: Double, decimals: Int = 6): String {
    val s = String.format(Locale.US, "%.${decimals}f", value)
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}
