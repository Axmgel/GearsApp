package com.axmgel.gearsapp.core.calc

import com.axmgel.gearsapp.core.machine.ChangeGear
import com.axmgel.gearsapp.core.machine.DefaultGearSets
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import com.axmgel.gearsapp.core.machine.GearHobber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Los valores esperados salen de correr los programas originales de Python
 * (Helice_Cabezal.py y Generadora.py) con los mismos datos.
 * La "huella" es la suma de a·1000003 + b·10007 + c·101 + d de todas las combinaciones.
 */
class CalcTest {

    private fun fingerprint(trains: Collection<GearTrain>): Long =
        trains.sumOf { it.a * 1000003L + it.b * 10007L + it.c!! * 101L + it.d!! }

    /** El programa original no deja repetir un número de dientes, así que se usa una pieza de cada uno. */
    private val oneOfEach = DefaultGearSets.dividingHead.map { it.copy(quantity = 1) }

    private fun pythonCabezal(helixLead: Double): Set<GearTrain> =
        GearTrainSearch.search(
            target = 203.2 / helixLead, inventory = oneOfEach, tolerance = 0.0001,
            simple = false, uniqueOnly = false, maxResults = Int.MAX_VALUE,
        ).map { it.train }.toSet()

    @Test
    fun heliceCabezalIgualQuePython500() {
        val found = pythonCabezal(500.0)
        assertEquals(128, found.size)
        assertEquals(6119578224L, fingerprint(found))
    }

    @Test
    fun heliceCabezalIgualQuePython1234_5() {
        val found = pythonCabezal(1234.5)
        assertEquals(32, found.size)
        assertEquals(1066968240L, fingerprint(found))
    }

    @Test
    fun generadoraIgualQuePython() {
        // Generadora.py prueba cualquier número de 10 a 80 dientes y deja repetirlos.
        val anyTeeth = (10..80).map { ChangeGear(it, 4) }
        val target = 8.355615 * kotlin.math.sin(Math.toRadians(15.0)) / (3.0 * 20)
        val found = GearTrainSearch.search(
            target, anyTeeth, tolerance = 1e-4, relative = false,
            simple = false, uniqueOnly = false, maxResults = Int.MAX_VALUE,
        ).map { it.train }.toSet()
        assertEquals(478, found.size)
        assertEquals(6417359664L, fingerprint(found))
    }

    @Test
    fun respetaCantidades() {
        val gears = listOf(ChangeGear(20, 1), ChangeGear(40, 1))
        // 20/20 necesitaría dos de 20.
        assertTrue(GearTrainSearch.search(1.0, gears).isEmpty())
        val twoOf20 = listOf(ChangeGear(20, 2), ChangeGear(40, 1))
        assertTrue(GearTrainSearch.search(1.0, twoOf20).any { it.train == GearTrain(20, 20) })
    }

    @Test
    fun heliceCabezalUsaDosEngranesIgualesSiLosHay() {
        val mill = DividingHeadMill("1", "F")
        val result = HelicalMilling.compute(203.2, mill)
        assertTrue(result.solutions.any { it.train == GearTrain(20, 20) })
        assertEquals(203.2, result.leadFor(result.solutions.first()), 1e-9)
    }

    @Test
    fun pasoDesdeDiametroYAngulo() {
        // Ángulo de 45°: el paso es igual a la circunferencia.
        assertEquals(Math.PI * 100, HelicalMilling.leadFromDiameter(100.0, 45.0), 1e-9)
    }

    @Test
    fun divisionSimple24() {
        val moves = Indexing.simple(24, 40, DefaultGearSets.brownSharpePlates)
        // 40/24 = 1 2/3 → en la circunferencia de 15: 10 espacios.
        assertTrue(moves.contains(Indexing.CrankMove(1, 10, 15, 1)))
        assertTrue(moves.all { it.turns == 1 && it.holes * 3 == it.circle!! * 2 })
    }

    @Test
    fun divisionExacta() {
        assertEquals(listOf(Indexing.CrankMove(10, 0, null, null)), Indexing.simple(4, 40, DefaultGearSets.brownSharpePlates))
    }

    @Test
    fun divisionDiferencial127() {
        val mill = DividingHeadMill("1", "F")
        val result = Indexing.compute(127, mill)
        assertTrue(result.simple.isEmpty())
        assertFalse(result.differential.isEmpty())
        result.differential.forEach { d ->
            // La relación de los engranes es exacta: 40 × |Z' − 127| ÷ Z'.
            val expected = 40.0 * abs(d.approxDivisions - 127) / d.approxDivisions
            assertEquals(expected, d.train.ratio, 1e-12)
            assertEquals(d.approxDivisions > 127, d.plateSameDirection)
        }
        // El número más cercano que sirve es Z' = 128: 40/128 = 5/16 de vuelta (5 espacios en la de 16),
        // engranes con relación 40 × 1 ÷ 128 = 5/16 (por ejemplo 25/80), plato en el mismo sentido.
        val nearest = result.differential.first()
        assertEquals(128, nearest.approxDivisions)
        assertEquals(Indexing.CrankMove(0, 5, 16, 1), nearest.move)
        assertTrue(result.differential.any { it.train == GearTrain(25, 80) })
        assertTrue(nearest.plateSameDirection)
    }

    @Test
    fun generadoraTrenDeHelice() {
        val hob = GearHobber("1", "G")
        val result = Hobbing.helixTrain(3.0, 1, 15.0, hob)
        val expected = 8.355615 * kotlin.math.sin(Math.toRadians(15.0)) / 3.0
        assertEquals(expected, result.targetRatio, 1e-12)
        result.solutions.forEach { assertTrue(abs(it.relativeError) <= 1e-4) }
    }

    @Test
    fun generadoraSincronizacion() {
        val hob = GearHobber("1", "G", divisionConstant = 24.0)
        val result = Hobbing.indexTrain(teeth = 48, hobStarts = 1, divisionConstant = 24.0, machine = hob)
        assertEquals(0.5, result.targetRatio, 1e-12)
        assertTrue(result.solutions.first().relativeError == 0.0)
    }
}
