package com.axmgel.gearsapp.core.machine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MachinesTest {

    @Test
    fun pasoDeMaquinaCoincideConElProgramaOriginal() {
        val mill = DividingHeadMill(id = "1", name = "Fresadora")
        assertEquals(203.2, mill.machineLeadMm, 1e-9)
    }

    @Test
    fun juegoPorDefectoCuentaRepetidos() {
        val gears = DefaultGearSets.dividingHead.associate { it.teeth to it.quantity }
        assertEquals(2, gears[20])
        assertEquals(2, gears[25])
        assertEquals(2, gears[55])
        assertEquals(1, gears[100])
        assertEquals(45, DefaultGearSets.dividingHead.sumOf { it.quantity })
    }

    @Test
    fun maquinasPorDefectoSonValidas() {
        assertTrue(MachineValidation.problems(DividingHeadMill("1", "Fresadora")).isEmpty())
        assertTrue(MachineValidation.problems(GearHobber("2", "Generadora")).isEmpty())
    }

    @Test
    fun detectaDatosInvalidos() {
        val bad = DividingHeadMill(
            id = "1", name = " ", headRatio = 0, tableLeadScrewPitchMm = -1.0,
            changeGears = listOf(ChangeGear(20), ChangeGear(20, 0)), indexPlates = emptyList(),
        )
        val problems = MachineValidation.problems(bad)
        assertEquals(6, problems.size)
    }

    @Test
    fun guardarYLeerConservaLosDatos() {
        val machines = listOf(
            DividingHeadMill("1", "Fresadora", tableLeadScrewPitchMm = 6.0),
            GearHobber("2", "Generadora", helixConstant = 7.5, changeGears = listOf(ChangeGear(30, 2))),
        )
        val text = MachineStorageFormat.encode(machines)
        assertEquals(machines, MachineStorageFormat.decode(text))
    }
}
