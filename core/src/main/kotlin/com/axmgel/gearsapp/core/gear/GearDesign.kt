package com.axmgel.gearsapp.core.gear

import com.axmgel.gearsapp.core.calc.CalcStep
import com.axmgel.gearsapp.core.calc.fmt
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/** Una medida del engrane para mostrar en la tabla de resultados. */
data class Dim(val name: String, val symbol: String, val value: Double, val unit: String = "mm")

/**
 * Resultado de un cálculo de engrane.
 * @property dims medidas para maquinar.
 * @property steps explicación paso a paso con los números sustituidos.
 * @property teeth número de dientes (para el encontrador de puntos).
 * @property helixLeadMm paso de la hélice, si el engrane es helicoidal (para la sincronización helicoidal).
 * @property helixAngleDeg ángulo de hélice, si aplica.
 * @property normalModule módulo normal (para la generadora).
 */
data class GearResult(
    val dims: List<Dim>,
    val steps: List<CalcStep>,
    val teeth: Int,
    val helixLeadMm: Double? = null,
    val helixAngleDeg: Double? = null,
    val normalModule: Double? = null,
)

/**
 * Proporciones del diente en el sistema de módulo.
 * @property addendum altura de cabeza ÷ módulo (normalmente 1).
 * @property dedendum altura de pie ÷ módulo (1.25 en la norma ISO; algunos libros usan 1.167).
 */
data class ToothSystem(val pressureAngleDeg: Double = 20.0, val addendum: Double = 1.0, val dedendum: Double = 1.25) {
    init {
        require(pressureAngleDeg > 0.0 && pressureAngleDeg < 45.0) { "El ángulo de presión debe estar entre 0 y 45 grados." }
        require(addendum > 0.0 && dedendum > addendum) { "La altura de pie debe ser mayor que la de cabeza." }
    }
}

private fun rad(deg: Double) = Math.toRadians(deg)
private fun deg(rad: Double) = Math.toDegrees(rad)

/** Número de fresa de módulo (juego de 8 fresas) según el número de dientes (reales o virtuales). */
fun cutterNumber(teeth: Double): Int = when {
    teeth < 14 -> 1
    teeth < 17 -> 2
    teeth < 21 -> 3
    teeth < 26 -> 4
    teeth < 35 -> 5
    teeth < 55 -> 6
    teeth < 135 -> 7
    else -> 8
}

private fun cutterRange(n: Int) = when (n) {
    1 -> "12 a 13"; 2 -> "14 a 16"; 3 -> "17 a 20"; 4 -> "21 a 25"
    5 -> "26 a 34"; 6 -> "35 a 54"; 7 -> "55 a 134"; else -> "135 a cremallera"
}

private fun cutterStep(zForCutter: Double, virtual: Boolean): CalcStep {
    val n = cutterNumber(zForCutter)
    val why = if (virtual) "Como el diente es inclinado, la fresa se escoge con los dientes virtuales (${fmt(zForCutter, 2)})." else ""
    return CalcStep(
        "Fresa de módulo",
        "$why En el juego de 8 fresas, la número $n sirve para ${cutterRange(n)} dientes.".trim(),
    )
}

/** Medidas para comprobar el diente con calibre de dientes (vernier de engranes). */
private fun chordal(pitchDiameter: Double, teethForChord: Double, addendum: Double): Pair<Double, Double> {
    val half = rad(90.0 / teethForChord)
    val thickness = pitchDiameter * sin(half)
    val height = addendum + pitchDiameter / 2 * (1 - cos(half))
    return thickness to height
}

object SpurGear {
    /**
     * Engrane recto (dientes paralelos al eje).
     * @param mateTeeth dientes del engrane que acopla, para calcular la distancia entre centros (opcional).
     */
    fun compute(module: Double, teeth: Int, mateTeeth: Int? = null, system: ToothSystem = ToothSystem()): GearResult {
        require(module > 0.0) { "El módulo debe ser mayor que cero." }
        require(teeth >= 6) { "El engrane debe tener al menos 6 dientes." }
        val m = module
        val ha = system.addendum * m
        val hf = system.dedendum * m
        val dp = m * teeth
        val de = dp + 2 * ha
        val di = dp - 2 * hf
        val p = PI * m
        val db = dp * cos(rad(system.pressureAngleDeg))
        val (sc, hc) = chordal(dp, teeth.toDouble(), ha)

        val dims = mutableListOf(
            Dim("Diámetro primitivo", "Dp", dp),
            Dim("Diámetro exterior (para tornear la pieza)", "De", de),
            Dim("Diámetro interior (fondo del diente)", "Di", di),
            Dim("Altura total del diente (profundidad de corte)", "h", ha + hf),
            Dim("Altura de cabeza", "ha", ha),
            Dim("Altura de pie", "hf", hf),
            Dim("Paso circular", "p", p),
            Dim("Espesor del diente en el primitivo", "s", p / 2),
            Dim("Espesor cordal (calibre de dientes)", "sc", sc),
            Dim("Altura cordal (calibre de dientes)", "hc", hc),
            Dim("Diámetro base", "Db", db),
        )
        val steps = mutableListOf(
            CalcStep("Diámetro primitivo", "Es el diámetro donde se tocan los dos engranes. Dp = m × Z = ${fmt(m)} × $teeth = ${fmt(dp, 4)} mm."),
            CalcStep("Diámetro exterior", "Se suma la cabeza del diente arriba y abajo: De = Dp + 2 × ${fmt(system.addendum)} × m = ${fmt(dp, 4)} + ${fmt(2 * ha, 4)} = ${fmt(de, 4)} mm. A esta medida se tornea la pieza."),
            CalcStep("Profundidad de corte", "h = cabeza + pie = ${fmt(ha, 4)} + ${fmt(hf, 4)} = ${fmt(ha + hf, 4)} mm. Es lo que bajas la fresa desde que toca la pieza."),
            CalcStep("Diámetro interior", "Di = Dp − 2 × pie = ${fmt(dp, 4)} − ${fmt(2 * hf, 4)} = ${fmt(di, 4)} mm."),
            CalcStep("Paso y espesor", "Paso circular p = π × m = 3.1416 × ${fmt(m)} = ${fmt(p, 4)} mm. El diente ocupa la mitad: s = ${fmt(p / 2, 4)} mm."),
            CalcStep(
                "Calibre de dientes",
                "Para medir el diente con el calibre: pon la altura en hc = ${fmt(hc, 4)} mm y el espesor debe dar sc = ${fmt(sc, 4)} mm " +
                    "(sc = Dp × sen(90° ÷ Z), hc = cabeza + Dp/2 × (1 − cos(90° ÷ Z))).",
            ),
            cutterStep(teeth.toDouble(), virtual = false),
        )
        if (mateTeeth != null) {
            require(mateTeeth >= 6) { "El otro engrane debe tener al menos 6 dientes." }
            val a = m * (teeth + mateTeeth) / 2
            dims += Dim("Distancia entre centros", "a", a)
            dims += Dim("Relación de transmisión", "i", mateTeeth.toDouble() / teeth, "")
            steps += CalcStep("Distancia entre centros", "a = m × (Z1 + Z2) ÷ 2 = ${fmt(m)} × ($teeth + $mateTeeth) ÷ 2 = ${fmt(a, 4)} mm.")
        }
        return GearResult(dims, steps, teeth)
    }
}

object HelicalGear {
    /**
     * Engrane helicoidal de ejes paralelos.
     * @param normalModule módulo normal (el de la fresa).
     * @param helixAngleDeg ángulo de hélice β.
     */
    fun compute(
        normalModule: Double,
        teeth: Int,
        helixAngleDeg: Double,
        mateTeeth: Int? = null,
        system: ToothSystem = ToothSystem(),
    ): GearResult {
        require(normalModule > 0.0) { "El módulo debe ser mayor que cero." }
        require(teeth >= 6) { "El engrane debe tener al menos 6 dientes." }
        require(helixAngleDeg > 0.0 && helixAngleDeg < 60.0) { "El ángulo de hélice debe estar entre 0 y 60 grados." }
        val mn = normalModule
        val b = rad(helixAngleDeg)
        val mt = mn / cos(b)
        val ha = system.addendum * mn
        val hf = system.dedendum * mn
        val dp = mt * teeth
        val de = dp + 2 * ha
        val di = dp - 2 * hf
        val lead = PI * dp / tan(b)
        val zv = teeth / cos(b).pow(3)
        val alphaT = deg(atan(tan(rad(system.pressureAngleDeg)) / cos(b)))
        val (sc, hc) = chordal(mn * zv, zv, ha)

        val dims = mutableListOf(
            Dim("Módulo frontal (aparente)", "mt", mt, ""),
            Dim("Diámetro primitivo", "Dp", dp),
            Dim("Diámetro exterior (para tornear la pieza)", "De", de),
            Dim("Diámetro interior", "Di", di),
            Dim("Altura total del diente", "h", ha + hf),
            Dim("Paso de la hélice", "L", lead),
            Dim("Paso normal", "pn", PI * mn),
            Dim("Paso frontal", "pt", PI * mt),
            Dim("Dientes virtuales (para escoger la fresa)", "Zv", zv, ""),
            Dim("Ángulo de presión frontal", "αt", alphaT, "°"),
            Dim("Espesor cordal normal (calibre)", "sc", sc),
            Dim("Altura cordal (calibre)", "hc", hc),
        )
        val steps = mutableListOf(
            CalcStep(
                "Módulo frontal",
                "La fresa corta perpendicular al diente (módulo normal), pero de frente el diente se ve más ancho. " +
                    "mt = mn ÷ cos β = ${fmt(mn)} ÷ cos(${fmt(helixAngleDeg, 4)}°) = ${fmt(mn)} ÷ ${fmt(cos(b), 6)} = ${fmt(mt, 6)}.",
            ),
            CalcStep("Diámetro primitivo", "Dp = mt × Z = ${fmt(mt, 6)} × $teeth = ${fmt(dp, 4)} mm."),
            CalcStep("Diámetro exterior", "De = Dp + 2 × cabeza = ${fmt(dp, 4)} + ${fmt(2 * ha, 4)} = ${fmt(de, 4)} mm. La cabeza se mide con el módulo normal."),
            CalcStep("Profundidad de corte", "h = ${fmt(ha, 4)} + ${fmt(hf, 4)} = ${fmt(ha + hf, 4)} mm (con el módulo normal)."),
            CalcStep(
                "Paso de la hélice",
                "Lo que avanza la hélice en una vuelta: L = π × Dp ÷ tan β = 3.1416 × ${fmt(dp, 4)} ÷ ${fmt(tan(b), 6)} = ${fmt(lead, 4)} mm. " +
                    "Con este paso se calculan los engranes del cabezal divisor.",
            ),
            CalcStep(
                "Dientes virtuales",
                "Zv = Z ÷ cos³ β = $teeth ÷ ${fmt(cos(b).pow(3), 6)} = ${fmt(zv, 2)}. Es el número de dientes de un engrane recto con la misma forma de diente.",
            ),
            cutterStep(zv, virtual = true),
            CalcStep("En la fresadora", "Inclina la mesa ${fmt(helixAngleDeg, 4)}° y usa la sincronización helicoidal con el paso L."),
        )
        if (mateTeeth != null) {
            require(mateTeeth >= 6) { "El otro engrane debe tener al menos 6 dientes." }
            val a = mt * (teeth + mateTeeth) / 2
            dims += Dim("Distancia entre centros", "a", a)
            steps += CalcStep(
                "Distancia entre centros",
                "a = mt × (Z1 + Z2) ÷ 2 = ${fmt(mt, 6)} × ($teeth + $mateTeeth) ÷ 2 = ${fmt(a, 4)} mm. " +
                    "Los dos engranes llevan el mismo ángulo, uno con hélice derecha y el otro izquierda.",
            )
        }
        return GearResult(dims, steps, teeth, helixLeadMm = lead, helixAngleDeg = helixAngleDeg, normalModule = mn)
    }
}

object BevelGear {
    /**
     * Engrane cónico de dientes rectos (o helicoidales si [spiralAngleDeg] > 0).
     * Las medidas son en el extremo grande del diente.
     *
     * @param module módulo en el extremo grande.
     * @param teeth dientes de este engrane.
     * @param mateTeeth dientes del engrane que acopla (se necesitan para los ángulos del cono).
     * @param shaftAngleDeg ángulo entre los ejes (normalmente 90°).
     * @param faceWidth largo del diente; si es null se usa un tercio de la generatriz.
     * @param spiralAngleDeg ángulo de la espiral a la mitad del diente (0 para dientes rectos).
     */
    fun compute(
        module: Double,
        teeth: Int,
        mateTeeth: Int,
        shaftAngleDeg: Double = 90.0,
        faceWidth: Double? = null,
        spiralAngleDeg: Double = 0.0,
        system: ToothSystem = ToothSystem(),
    ): GearResult {
        require(module > 0.0) { "El módulo debe ser mayor que cero." }
        require(teeth >= 6 && mateTeeth >= 6) { "Cada engrane debe tener al menos 6 dientes." }
        require(shaftAngleDeg > 0.0 && shaftAngleDeg < 180.0) { "El ángulo entre ejes debe estar entre 0 y 180 grados." }
        require(spiralAngleDeg >= 0.0 && spiralAngleDeg < 60.0) { "El ángulo de espiral debe estar entre 0 y 60 grados." }
        val m = module
        val sigma = rad(shaftAngleDeg)
        val delta = atan2(sin(sigma), mateTeeth.toDouble() / teeth + cos(sigma))
        val deltaDeg = deg(delta)
        val dp = m * teeth
        val r = dp / (2 * sin(delta))
        val b = faceWidth ?: (r / 3)
        require(b > 0.0 && b < r / 2) { "El largo del diente debe ser menor que la mitad de la generatriz (${fmt(r / 2, 2)} mm)." }
        val ha = system.addendum * m
        val hf = system.dedendum * m
        val thetaA = deg(atan(ha / r))
        val thetaF = deg(atan(hf / r))
        val de = dp + 2 * ha * cos(delta)
        val beta = rad(spiralAngleDeg)
        val zv = teeth / (cos(delta) * cos(beta).pow(3))
        val rm = r - b / 2
        val mmid = m * rm / r
        val spiral = spiralAngleDeg > 0.0

        val dims = mutableListOf(
            Dim("Ángulo del cono primitivo", "δ", deltaDeg, "°"),
            Dim("Ángulo de cara (para tornear el cono)", "δa", deltaDeg + thetaA, "°"),
            Dim("Ángulo de fondo (inclinación del cabezal)", "δf", deltaDeg - thetaF, "°"),
            Dim("Ángulo de cabeza", "θa", thetaA, "°"),
            Dim("Ángulo de pie", "θf", thetaF, "°"),
            Dim("Diámetro primitivo (extremo grande)", "Dp", dp),
            Dim("Diámetro exterior (extremo grande)", "De", de),
            Dim("Generatriz (distancia del vértice)", "R", r),
            Dim("Largo del diente", "b", b),
            Dim("Altura total del diente (extremo grande)", "h", ha + hf),
            Dim("Módulo a la mitad del diente", "mm", mmid, ""),
            Dim("Dientes virtuales (para escoger la fresa)", "Zv", zv, ""),
        )
        if (spiral) dims += Dim("Módulo normal a la mitad del diente", "mn", mmid * cos(beta), "")

        val steps = mutableListOf(
            CalcStep(
                "Ángulo del cono",
                "Los dos conos se tocan en el vértice. Con ejes a ${fmt(shaftAngleDeg, 4)}°: " +
                    "tan δ = sen Σ ÷ (Z2/Z1 + cos Σ) = ${fmt(sin(sigma), 6)} ÷ (${mateTeeth}/${teeth} + ${fmt(cos(sigma), 6)}) → " +
                    "δ = ${fmt(deltaDeg, 4)}°. El otro engrane lleva ${fmt(shaftAngleDeg - deltaDeg, 4)}°.",
            ),
            CalcStep("Diámetro primitivo", "En el extremo grande: Dp = m × Z = ${fmt(m)} × $teeth = ${fmt(dp, 4)} mm."),
            CalcStep("Generatriz", "Distancia del vértice del cono al extremo grande del diente: R = Dp ÷ (2 × sen δ) = ${fmt(dp, 4)} ÷ ${fmt(2 * sin(delta), 6)} = ${fmt(r, 4)} mm."),
            CalcStep(
                "Ángulos de cabeza y pie",
                "El diente se hace más chico hacia el vértice. tan θa = cabeza ÷ R = ${fmt(ha, 4)} ÷ ${fmt(r, 4)} → θa = ${fmt(thetaA, 4)}°; " +
                    "tan θf = pie ÷ R = ${fmt(hf, 4)} ÷ ${fmt(r, 4)} → θf = ${fmt(thetaF, 4)}°.",
            ),
            CalcStep(
                "Tornear la pieza",
                "Ángulo de cara δa = δ + θa = ${fmt(deltaDeg + thetaA, 4)}°, medido desde el eje. " +
                    "Diámetro exterior De = Dp + 2 × cabeza × cos δ = ${fmt(dp, 4)} + ${fmt(2 * ha * cos(delta), 4)} = ${fmt(de, 4)} mm. " +
                    "Largo del diente b = ${fmt(b, 2)} mm (no más de un tercio de R).",
            ),
            CalcStep(
                "Fresar",
                "Inclina el cabezal divisor al ángulo de fondo δf = δ − θf = ${fmt(deltaDeg - thetaF, 4)}° " +
                    "y corta a la profundidad h = ${fmt(ha + hf, 4)} mm en el extremo grande.",
            ),
            CalcStep(
                "Dientes virtuales",
                if (spiral) {
                    "Zv = Z ÷ (cos δ × cos³ β) = $teeth ÷ (${fmt(cos(delta), 6)} × ${fmt(cos(beta).pow(3), 6)}) = ${fmt(zv, 2)}."
                } else {
                    "Zv = Z ÷ cos δ = $teeth ÷ ${fmt(cos(delta), 6)} = ${fmt(zv, 2)}."
                },
            ),
            cutterStep(zv, virtual = true),
        )
        if (spiral) {
            steps += CalcStep(
                "Diente helicoidal",
                "A la mitad del diente el módulo es mm = m × (R − b/2) ÷ R = ${fmt(m)} × ${fmt(rm, 4)} ÷ ${fmt(r, 4)} = ${fmt(mmid, 4)}, " +
                    "y el módulo normal es mn = mm × cos β = ${fmt(mmid * cos(beta), 4)}. Con este se escoge la fresa.",
            )
        }
        return GearResult(
            dims, steps, teeth,
            helixAngleDeg = if (spiral) spiralAngleDeg else null,
            normalModule = if (spiral) mmid * cos(beta) else null,
        )
    }
}
