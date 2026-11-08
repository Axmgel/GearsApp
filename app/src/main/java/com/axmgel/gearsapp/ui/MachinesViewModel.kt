package com.axmgel.gearsapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axmgel.gearsapp.core.machine.Machine
import com.axmgel.gearsapp.data.MachineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MachinesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MachineRepository(app)

    /** null mientras se leen las máquinas guardadas. */
    val machines: StateFlow<List<Machine>?> = repository.machines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(machine: Machine) {
        viewModelScope.launch { repository.save(machine) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
