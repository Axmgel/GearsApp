package com.axmgel.gearsapp.core.gear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GearDesignTest {

    private fun GearResult.dim(symbol: String) = dims.first { it.symbol == symbol }.value

    @Test
    fun rectoModulo2De30Dientes() {
        val g = SpurGear.compute(2.0, 30, mateTeeth = 45)
        assertEquals(60.0, g.dim("Dp"), 1e-9)
        assertEquals(64.0, g.dim("De"), 1e-9)
        assertEquals(55.0, g.dim("Di"), 1e-9)
        assertEquals(4.5, g.dim("h"), 1e-9)
        assertEquals(Math.PI * 2, g.dim("p"), 1e-9)
        assertEquals(75.0, g.dim("a"), 1e-9)
        // Calibre de dientes para Z = 30, m = 2: sc = 60·sen(3°), hc = 2 + 30·(1 − cos 3°).
        assertEquals(3.14016, g.dim("sc"), 1e-5)
        assertEquals(2.041114, g.dim("hc"), 1e-6)
        assertEquals(5, cutterNumber(30.0))
    }

    @Test
    fun helicoidal() {
        val g = HelicalGear.compute(3.0, 20, 15.0)
        val mt = 3.0 / Math.cos(Math.toRadians(15.0))
        assertEquals(mt * 20, g.dim("Dp"), 1e-9)
        assertEquals(mt * 20 + 6, g.dim("De"), 1e-9)
        assertEquals(Math.PI * mt * 20 / Math.tan(Math.toRadians(15.0)), g.helixLeadMm!!, 1e-9)
        assertEquals(20 / Math.pow(Math.cos(Math.toRadians(15.0)), 3.0), g.dim("Zv"), 1e-9)
    }

    @Test
    fun conicoIgualesA90Grados() {
        // Dos engranes iguales a 90°: cono de 45°.
        val g = BevelGear.compute(3.0, 20, 20)
        assertEquals(45.0, g.dim("δ"), 1e-9)
        assertEquals(60.0 / (2 * Math.sin(Math.toRadians(45.0))), g.dim("R"), 1e-9)
        assertEquals(60.0 + 6 * Math.cos(Math.toRadians(45.0)), g.dim("De"), 1e-9)
        assertNull(g.helixAngleDeg)
    }

    @Test
    fun conicoRelacion2a1() {
        val pinion = BevelGear.compute(2.0, 20, 40)
        val wheel = BevelGear.compute(2.0, 40, 20)
        assertEquals(Math.toDegrees(Math.atan(0.5)), pinion.dim("δ"), 1e-9)
        assertEquals(90.0, pinion.dim("δ") + wheel.dim("δ"), 1e-9)
        // Los dos comparten la misma generatriz.
        assertEquals(pinion.dim("R"), wheel.dim("R"), 1e-9)
    }

    @Test
    fun conicoHelicoidal() {
        val g = BevelGear.compute(3.0, 20, 40, spiralAngleDeg = 35.0)
        assertEquals(35.0, g.helixAngleDeg!!, 0.0)
        val r = g.dim("R")
        assertEquals(3.0 * (r - r / 6) / r * Math.cos(Math.toRadians(35.0)), g.dim("mn"), 1e-9)
    }
}
