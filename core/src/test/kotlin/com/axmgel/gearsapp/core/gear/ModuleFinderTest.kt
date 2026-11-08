package com.axmgel.gearsapp.core.gear

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleFinderTest {

    @Test
    fun rectoPorDiametroExterior() {
        val s = ModuleFinder.spur(64.0, DiameterKind.OUTSIDE, 30)
        assertEquals(2.0, s.exactModule, 1e-12)
        assertEquals(2.0, s.suggestions.first().module, 0.0)
        assertEquals(0.0, s.suggestions.first().difference, 1e-12)
    }

    @Test
    fun rectoSugiereModuloNormalizado() {
        // 36.6 ÷ 30 = 1.22 → el normalizado más cercano es 1.25, que da Dp = 37.5 (+0.9 mm).
        val s = ModuleFinder.spur(36.6, DiameterKind.PITCH, 30)
        assertEquals(1.22, s.exactModule, 1e-12)
        val best = s.suggestions.first()
        assertEquals(1.25, best.module, 0.0)
        assertEquals(1, best.series)
        assertEquals(37.5, best.diameter, 1e-12)
        assertEquals(0.9, best.difference, 1e-12)
    }

    @Test
    fun helicoidalIdaYVuelta() {
        val g = HelicalGear.compute(3.0, 20, 15.0)
        val de = g.dims.first { it.symbol == "De" }.value
        val s = ModuleFinder.helical(de, DiameterKind.OUTSIDE, 20, 15.0)
        assertEquals(3.0, s.exactModule, 1e-9)
        // Con el mismo módulo, el ángulo exacto es el original.
        assertEquals(15.0, s.suggestions.first { it.module == 3.0 }.exactHelixAngleDeg!!, 1e-9)
    }

    @Test
    fun helicoidalAnguloParaModuloNormalizado() {
        // Dp = 64 con 20 dientes y β ≈ 20° da mn ≈ 3.007; con mn = 3 el ángulo exacto es acos(60/64).
        val s = ModuleFinder.helical(64.0, DiameterKind.PITCH, 20, 20.0)
        val three = s.suggestions.first { it.module == 3.0 }
        assertEquals(Math.toDegrees(Math.acos(60.0 / 64.0)), three.exactHelixAngleDeg!!, 1e-9)
    }

    @Test
    fun conicoIdaYVuelta() {
        val g = BevelGear.compute(2.5, 18, 36)
        val de = g.dims.first { it.symbol == "De" }.value
        val s = ModuleFinder.bevel(de, DiameterKind.OUTSIDE, 18, 36)
        assertEquals(2.5, s.exactModule, 1e-9)
    }
}
