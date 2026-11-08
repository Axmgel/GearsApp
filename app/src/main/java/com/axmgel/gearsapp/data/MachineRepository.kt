package com.axmgel.gearsapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import com.axmgel.gearsapp.core.machine.GearHobber
import com.axmgel.gearsapp.core.machine.Machine
import com.axmgel.gearsapp.core.machine.MachineStorageFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.machineStore by preferencesDataStore(name = "maquinas")

/** Guarda las máquinas del taller dentro del teléfono (no necesita internet). */
class MachineRepository(private val context: Context) {

    private val key = stringPreferencesKey("maquinas_json")

    val machines: Flow<List<Machine>> = context.machineStore.data.map { readFrom(it) }

    suspend fun save(machine: Machine) {
        context.machineStore.edit { prefs ->
            val current = readFrom(prefs)
            val updated = if (current.any { it.id == machine.id }) {
                current.map { if (it.id == machine.id) machine else it }
            } else {
                current + machine
            }
            prefs[key] = MachineStorageFormat.encode(updated)
        }
    }

    suspend fun delete(id: String) {
        context.machineStore.edit { prefs ->
            prefs[key] = MachineStorageFormat.encode(readFrom(prefs).filterNot { it.id == id })
        }
    }

    private fun readFrom(prefs: Preferences): List<Machine> {
        val text = prefs[key] ?: return defaultMachines()
        return runCatching { MachineStorageFormat.decode(text) }.getOrElse { defaultMachines() }
    }

    companion object {
        /** Máquinas de ejemplo para la primera vez que se abre la app. */
        fun defaultMachines(): List<Machine> = listOf(
            DividingHeadMill(id = "ejemplo-fresadora", name = "Fresadora universal (ejemplo)"),
            GearHobber(id = "ejemplo-generadora", name = "Generadora (ejemplo)"),
        )
    }
}
