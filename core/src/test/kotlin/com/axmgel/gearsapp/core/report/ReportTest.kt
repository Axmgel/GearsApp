package com.axmgel.gearsapp.core.report

import com.axmgel.gearsapp.core.calc.HelicalMilling
import com.axmgel.gearsapp.core.calc.Indexing
import com.axmgel.gearsapp.core.gear.SpurGear
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {

    @Test
    fun reporteDeEngraneRecto() {
        val r = Reports.gear("Engrane recto", listOf("Módulo" to "2", "Dientes" to "30"), SpurGear.compute(2.0, 30))
        val text = Reports.toText(r)
        assertTrue(text.contains("Módulo: 2"))
        assertTrue(text.contains("Diámetro exterior (para tornear la pieza) (De): 64 mm"))
        assertTrue(text.contains("Cómo se calcula"))
    }

    @Test
    fun reporteHelicoidalLimitaCombinaciones() {
        val mill = DividingHeadMill("1", "F")
        val res = HelicalMilling.compute(500.0, mill, tolerance = 0.01)
        val r = Reports.helicalMilling(listOf("Máquina" to "F"), res.steps, res)
        val trains = r.sections.first { it.heading == "Combinaciones de engranes" }.lines
        assertTrue(res.solutions.size > Reports.MAX_TRAINS)
        assertTrue(trains.size == Reports.MAX_TRAINS + 1)
    }

    @Test
    fun reporteDeDivisionDiferencial() {
        val r = Reports.indexing(listOf("Máquina" to "F"), Indexing.compute(127, DividingHeadMill("1", "F")))
        assertTrue(r.sections.any { it.heading == "División diferencial" })
    }
}
