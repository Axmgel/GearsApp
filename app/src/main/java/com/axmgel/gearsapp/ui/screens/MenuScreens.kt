package com.axmgel.gearsapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class MenuEntry(val title: String, val subtitle: String, val onClick: () -> Unit)

data class MenuSection(val title: String?, val entries: List<MenuEntry>)

/** Barra superior común con botón para regresar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)?,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                },
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
fun MenuScreen(title: String, onBack: (() -> Unit)?, sections: List<MenuSection>) {
    AppScaffold(title = title, onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sections.forEach { section ->
                if (section.title != null) {
                    item {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                section.entries.forEach { entry ->
                    item { MenuCard(entry) }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(entry: MenuEntry) {
    Card(onClick = entry.onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleLarge)
            Text(entry.subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Pantalla provisional para los cálculos que se agregan en los siguientes pasos del plan. */
@Composable
fun ComingSoonScreen(title: String, onBack: () -> Unit) {
    AppScaffold(title = title, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Este cálculo todavía no está listo. Se agrega en los siguientes pasos del plan.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
