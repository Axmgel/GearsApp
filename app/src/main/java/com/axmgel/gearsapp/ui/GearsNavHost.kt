package com.axmgel.gearsapp.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.axmgel.gearsapp.ui.screens.ComingSoonScreen
import com.axmgel.gearsapp.ui.screens.LoadingScreen
import com.axmgel.gearsapp.ui.screens.MachineEditScreen
import com.axmgel.gearsapp.ui.screens.MachinesScreen
import com.axmgel.gearsapp.ui.screens.MenuEntry
import com.axmgel.gearsapp.ui.screens.MenuScreen
import com.axmgel.gearsapp.ui.screens.MenuSection
import com.axmgel.gearsapp.ui.screens.newMachine

/** Rutas de las pantallas. Siguen el diagrama de menús del proyecto. */
object Routes {
    const val MAIN = "menu"
    const val GEARS = "engranes"
    const val MACHINING = "maquinaria"
    const val MACHINES = "maquinas"
    const val MACHINE_EDIT = "maquinas/editar/{id}"
    const val MACHINE_NEW = "maquinas/nueva/{tipo}"
    const val SOON = "pronto/{titulo}"

    fun machineEdit(id: String) = "maquinas/editar/$id"
    fun machineNew(type: String) = "maquinas/nueva/$type"
    fun soon(title: String) = "pronto/${Uri.encode(title)}"
}

@Composable
fun GearsNavHost() {
    val nav = rememberNavController()
    val machinesVm: MachinesViewModel = viewModel()
    val machines by machinesVm.machines.collectAsStateWithLifecycle()

    fun soon(title: String) = nav.navigate(Routes.soon(title))

    NavHost(navController = nav, startDestination = Routes.MAIN) {

        composable(Routes.MAIN) {
            MenuScreen(
                title = "GearsApp",
                onBack = null,
                sections = listOf(
                    MenuSection(
                        title = null,
                        entries = listOf(
                            MenuEntry("Cálculo de engranes", "Rectos, helicoidales, cónicos y cónicos helicoidales") { nav.navigate(Routes.GEARS) },
                            MenuEntry("Cálculo de maquinaria", "Generadora y cabezal divisor") { nav.navigate(Routes.MACHINING) },
                            MenuEntry("Configurar máquinas", "Captura los datos de las máquinas de tu taller") { nav.navigate(Routes.MACHINES) },
                        ),
                    ),
                ),
            )
        }

        composable(Routes.GEARS) {
            MenuScreen(
                title = "Cálculo de engranes",
                onBack = { nav.popBackStack() },
                sections = listOf(
                    MenuSection(
                        title = null,
                        entries = listOf(
                            MenuEntry("Rectos", "Dientes paralelos al eje") { soon("Engranes rectos") },
                            MenuEntry("Helicoidales", "Dientes inclinados con un ángulo de hélice") { soon("Engranes helicoidales") },
                            MenuEntry("Cónicos", "Ejes que se cruzan, dientes rectos") { soon("Engranes cónicos") },
                            MenuEntry("Cónicos helicoidales", "Ejes que se cruzan, dientes curvos o inclinados") { soon("Engranes cónicos helicoidales") },
                        ),
                    ),
                ),
            )
        }

        composable(Routes.MACHINING) {
            MenuScreen(
                title = "Cálculo de maquinaria",
                onBack = { nav.popBackStack() },
                sections = listOf(
                    MenuSection(
                        title = "Generadora",
                        entries = listOf(
                            MenuEntry("Sincronización", "Tren de engranes para el número de divisiones") { soon("Sincronización de la generadora") },
                            MenuEntry("Tren de hélice", "Engranes para el ángulo de hélice") { soon("Tren de hélice de la generadora") },
                        ),
                    ),
                    MenuSection(
                        title = "Cabezal divisor",
                        entries = listOf(
                            MenuEntry("Encontrador de puntos para divisiones", "Vueltas y agujeros del plato divisor") { soon("Encontrador de puntos") },
                            MenuEntry("Sincronización helicoidal", "Engranes entre la mesa y el cabezal para cortar una hélice") { soon("Sincronización helicoidal") },
                        ),
                    ),
                ),
            )
        }

        composable(Routes.MACHINES) {
            MachinesScreen(
                machines = machines,
                onBack = { nav.popBackStack() },
                onOpen = { nav.navigate(Routes.machineEdit(it)) },
                onAdd = { type -> nav.navigate(Routes.machineNew(type)) },
            )
        }

        composable(Routes.MACHINE_EDIT) { entry ->
            val id = entry.arguments?.getString("id")
            val list = machines
            val machine = list?.firstOrNull { it.id == id }
            when {
                // También cubre el instante después de borrar, mientras la pantalla se cierra.
                list == null || machine == null -> LoadingScreen()
                else -> MachineEditScreen(
                    initial = machine,
                    isNew = false,
                    onBack = { nav.popBackStack() },
                    onSave = { machinesVm.save(it); nav.popBackStack() },
                    onDelete = { nav.popBackStack(); machinesVm.delete(machine.id) },
                )
            }
        }

        composable(Routes.MACHINE_NEW) { entry ->
            val type = entry.arguments?.getString("tipo").orEmpty()
            MachineEditScreen(
                initial = newMachine(type),
                isNew = true,
                onBack = { nav.popBackStack() },
                onSave = { machinesVm.save(it); nav.popBackStack() },
                onDelete = null,
            )
        }

        composable(Routes.SOON) { entry ->
            ComingSoonScreen(
                title = entry.arguments?.getString("titulo").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
