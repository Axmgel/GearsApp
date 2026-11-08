package com.axmgel.gearsapp.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.axmgel.gearsapp.core.machine.DividingHeadMill
import com.axmgel.gearsapp.core.machine.GearHobber
import com.axmgel.gearsapp.ui.screens.ComingSoonScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.axmgel.gearsapp.core.calc.fmt
import com.axmgel.gearsapp.ui.screens.calc.GearKind
import com.axmgel.gearsapp.ui.screens.calc.GearLinks
import com.axmgel.gearsapp.ui.screens.calc.GearScreen
import com.axmgel.gearsapp.ui.screens.calc.HelicalMillingScreen
import com.axmgel.gearsapp.ui.screens.calc.HobberHelixScreen
import com.axmgel.gearsapp.ui.screens.calc.HobberSyncScreen
import com.axmgel.gearsapp.ui.screens.calc.IndexingScreen
import com.axmgel.gearsapp.ui.screens.LoadingScreen
import com.axmgel.gearsapp.ui.screens.MachineEditScreen
import com.axmgel.gearsapp.ui.screens.MachinesScreen
import com.axmgel.gearsapp.ui.screens.EntryStyle
import com.axmgel.gearsapp.ui.screens.MenuEntry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
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
    const val HELICAL_MILLING = "cabezal/helicoidal?paso={paso}&ang={ang}"
    const val INDEXING = "cabezal/puntos?z={z}"
    const val HOB_HELIX = "generadora/helice?m={m}&b={b}"
    const val GEAR = "engrane/{tipo}"

    fun gear(kind: GearKind) = "engrane/${kind.name}"
    fun indexing(z: Int? = null) = if (z == null) "cabezal/puntos" else "cabezal/puntos?z=$z"
    fun helicalMilling(lead: Double? = null, angle: Double? = null) =
        if (lead == null) "cabezal/helicoidal" else "cabezal/helicoidal?paso=${fmt(lead, 4)}&ang=${fmt(angle ?: 0.0, 4)}"
    fun hobHelix(module: Double? = null, angle: Double? = null) =
        if (module == null) "generadora/helice" else "generadora/helice?m=${fmt(module, 6)}&b=${fmt(angle ?: 0.0, 4)}"
    const val HOB_SYNC = "generadora/sincronizacion"

    fun machineEdit(id: String) = "maquinas/editar/$id"
    fun machineNew(type: String) = "maquinas/nueva/$type"
    fun soon(title: String) = "pronto/${Uri.encode(title)}"
}

/** Argumentos opcionales de texto (vacíos si no se mandan). */
private fun optionalArgs(vararg names: String) = names.map { name ->
    navArgument(name) {
        type = NavType.StringType
        defaultValue = ""
    }
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
                            MenuEntry("Cálculo de engranes", "Rectos, helicoidales, cónicos y cónicos helicoidales", Icons.Filled.Settings) { nav.navigate(Routes.GEARS) },
                            MenuEntry("Cálculo de maquinaria", "Generadora y cabezal divisor", Icons.Filled.PrecisionManufacturing) { nav.navigate(Routes.MACHINING) },
                        ),
                    ),
                    MenuSection(
                        title = "Tu taller",
                        entries = listOf(
                            MenuEntry("Configurar máquinas", "Captura los datos de las máquinas de tu taller", Icons.Filled.Tune, EntryStyle.SETTINGS) { nav.navigate(Routes.MACHINES) },
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
                            MenuEntry("Rectos", "Dientes paralelos al eje", Icons.Filled.Settings) { nav.navigate(Routes.gear(GearKind.SPUR)) },
                            MenuEntry("Helicoidales", "Dientes inclinados con un ángulo de hélice", Icons.Filled.RotateRight) { nav.navigate(Routes.gear(GearKind.HELICAL)) },
                            MenuEntry("Cónicos", "Ejes que se cruzan, dientes rectos", Icons.Filled.ChangeHistory) { nav.navigate(Routes.gear(GearKind.BEVEL)) },
                            MenuEntry("Cónicos helicoidales", "Ejes que se cruzan, dientes curvos o inclinados", Icons.Filled.Loop) { nav.navigate(Routes.gear(GearKind.SPIRAL_BEVEL)) },
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
                            MenuEntry("Sincronización", "Tren de engranes para el número de divisiones", Icons.Filled.Sync) { nav.navigate(Routes.HOB_SYNC) },
                            MenuEntry("Tren de hélice", "Engranes para el ángulo de hélice", Icons.Filled.RotateRight) { nav.navigate(Routes.hobHelix()) },
                        ),
                    ),
                    MenuSection(
                        title = "Cabezal divisor",
                        entries = listOf(
                            MenuEntry("Encontrador de puntos para divisiones", "Vueltas y agujeros del plato divisor", Icons.Filled.TrackChanges) { nav.navigate(Routes.indexing()) },
                            MenuEntry("Sincronización helicoidal", "Engranes entre la mesa y el cabezal para cortar una hélice", Icons.Filled.Loop) { nav.navigate(Routes.helicalMilling()) },
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

        composable(Routes.HELICAL_MILLING, arguments = optionalArgs("paso", "ang")) { entry ->
            HelicalMillingScreen(
                machines?.filterIsInstance<DividingHeadMill>(),
                onBack = { nav.popBackStack() },
                initialLead = entry.arguments?.getString("paso").orEmpty(),
                initialAngle = entry.arguments?.getString("ang").orEmpty(),
            )
        }
        composable(Routes.INDEXING, arguments = optionalArgs("z")) { entry ->
            IndexingScreen(
                machines?.filterIsInstance<DividingHeadMill>(),
                onBack = { nav.popBackStack() },
                initialDivisions = entry.arguments?.getString("z").orEmpty(),
            )
        }
        composable(Routes.HOB_HELIX, arguments = optionalArgs("m", "b")) { entry ->
            HobberHelixScreen(
                machines?.filterIsInstance<GearHobber>(),
                onBack = { nav.popBackStack() },
                initialModule = entry.arguments?.getString("m").orEmpty(),
                initialAngle = entry.arguments?.getString("b").orEmpty(),
            )
        }
        composable(Routes.GEAR) { entry ->
            val kind = runCatching { GearKind.valueOf(entry.arguments?.getString("tipo").orEmpty()) }.getOrDefault(GearKind.SPUR)
            GearScreen(
                kind = kind,
                links = GearLinks(
                    toIndexing = { nav.navigate(Routes.indexing(it)) },
                    toHelicalMilling = { lead, angle -> nav.navigate(Routes.helicalMilling(lead, angle)) },
                    toHobberHelix = { m, angle -> nav.navigate(Routes.hobHelix(m, angle)) },
                ),
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.HOB_SYNC) {
            HobberSyncScreen(machines?.filterIsInstance<GearHobber>(), onBack = { nav.popBackStack() })
        }

        composable(Routes.SOON) { entry ->
            ComingSoonScreen(
                title = entry.arguments?.getString("titulo").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
