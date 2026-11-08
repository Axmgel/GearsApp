package com.axmgel.gearsapp.core.machine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Un engrane de cambio que el taller tiene disponible.
 *
 * @property teeth número de dientes.
 * @property quantity cuántas piezas iguales hay de ese engrane (por ejemplo, dos de 20 dientes).
 */
@Serializable
data class ChangeGear(val teeth: Int, val quantity: Int = 1)

/**
 * Máquina que el usuario da de alta en "Configurar máquinas".
 * Cada taller captura los datos de sus propias máquinas, porque varían de un fabricante a otro.
 */
@Serializable
sealed interface Machine {
    val id: String
    val name: String
    val changeGears: List<ChangeGear>
}

/**
 * Fresadora universal con cabezal divisor.
 *
 * @property headRatio vueltas de la manivela para que el husillo del cabezal dé una vuelta completa
 *   (en la mayoría de los cabezales es 40).
 * @property tableLeadScrewPitchMm paso del tornillo (husillo) que mueve la mesa, en milímetros.
 * @property indexPlates platos divisores: cada plato es una lista con el número de agujeros de cada circunferencia.
 */
@Serializable
@SerialName("cabezal_divisor")
data class DividingHeadMill(
    override val id: String,
    override val name: String,
    val headRatio: Int = 40,
    val tableLeadScrewPitchMm: Double = 5.08,
    override val changeGears: List<ChangeGear> = DefaultGearSets.dividingHead,
    val indexPlates: List<List<Int>> = DefaultGearSets.brownSharpePlates,
) : Machine {
    /**
     * Paso de la máquina: el avance de la mesa cuando el husillo del cabezal da una vuelta
     * con engranes de cambio 1 a 1. Se obtiene multiplicando la relación del cabezal por el paso del husillo.
     * Ejemplo: 40 × 5.08 mm = 203.2 mm.
     */
    val machineLeadMm: Double get() = headRatio * tableLeadScrewPitchMm
}

/**
 * Generadora de engranajes (fresa madre).
 *
 * @property helixConstant constante del tren diferencial/hélice que da el fabricante de la máquina.
 *   Se usa en la fórmula: (a/b)·(c/d) = constante · sen(β) / (módulo · entradas de la fresa).
 */
@Serializable
@SerialName("generadora")
data class GearHobber(
    override val id: String,
    override val name: String,
    val helixConstant: Double = 8.355615,
    override val changeGears: List<ChangeGear> = DefaultGearSets.dividingHead,
) : Machine

/** Valores de ejemplo con los que se llena la app la primera vez. Todos se pueden cambiar. */
object DefaultGearSets {
    /** Juego de engranes del programa original Helice_Cabezal.py (20, 25 y 55 aparecen dos veces). */
    val dividingHead: List<ChangeGear> = listOf(
        20, 20, 21, 23, 24, 25, 25, 30, 33, 34, 35, 37, 40,
        41, 43, 45, 47, 48, 50, 53, 55, 55, 57, 58, 59, 60,
        61, 62, 65, 67, 70, 71, 73, 75, 79, 80, 83, 85, 89,
        90, 92, 95, 97, 98, 100,
    ).groupingBy { it }.eachCount().map { (teeth, qty) -> ChangeGear(teeth, qty) }

    /** Platos divisores Brown & Sharpe estándar (1, 2 y 3). */
    val brownSharpePlates: List<List<Int>> = listOf(
        listOf(15, 16, 17, 18, 19, 20),
        listOf(21, 23, 27, 29, 31, 33),
        listOf(37, 39, 41, 43, 47, 49),
    )
}
