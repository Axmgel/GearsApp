package com.axmgel.gearsapp.core.machine

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Revisa que los datos de una máquina tengan sentido y devuelve los problemas en lenguaje de taller. */
object MachineValidation {

    fun problems(machine: Machine): List<String> {
        val out = mutableListOf<String>()
        if (machine.name.isBlank()) out += "Ponle un nombre a la máquina para reconocerla."
        out += gearProblems(machine.changeGears)
        when (machine) {
            is DividingHeadMill -> {
                if (machine.headRatio <= 0) out += "La relación del cabezal debe ser mayor que cero (casi siempre es 40)."
                if (machine.tableLeadScrewPitchMm <= 0.0) out += "El paso del husillo de la mesa debe ser mayor que cero."
                if (machine.indexPlates.isEmpty()) out += "Agrega al menos un plato divisor."
                machine.indexPlates.forEachIndexed { i, plate ->
                    if (plate.isEmpty()) out += "El plato ${i + 1} no tiene circunferencias de agujeros."
                    if (plate.any { it <= 0 }) out += "El plato ${i + 1} tiene un número de agujeros inválido."
                }
            }
            is GearHobber -> {
                if (machine.helixConstant <= 0.0) out += "La constante de hélice debe ser mayor que cero."
                val dc = machine.divisionConstant
                if (dc != null && dc <= 0.0) out += "La constante de división debe ser mayor que cero (o déjala vacía)."
            }
        }
        return out
    }

    private fun gearProblems(gears: List<ChangeGear>): List<String> {
        val out = mutableListOf<String>()
        if (gears.isEmpty()) out += "Agrega los engranes de cambio que tienes en el taller."
        if (gears.any { it.teeth <= 0 }) out += "Hay un engrane con cero dientes o menos."
        if (gears.any { it.quantity <= 0 }) out += "Hay un engrane con cantidad cero o menos."
        val repeated = gears.groupBy { it.teeth }.filterValues { it.size > 1 }.keys
        if (repeated.isNotEmpty()) {
            out += "El engrane de ${repeated.sorted().joinToString()} dientes aparece en más de un renglón; súmalos en la cantidad."
        }
        return out
    }
}

/** Convierte la lista de máquinas a texto (JSON) para guardarla en el teléfono, y de regreso. */
object MachineStorageFormat {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(machines: List<Machine>): String = json.encodeToString<List<Machine>>(machines)

    fun decode(text: String): List<Machine> = json.decodeFromString<List<Machine>>(text)
}
