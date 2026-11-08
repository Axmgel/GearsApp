package com.axmgel.gearsapp.report

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.axmgel.gearsapp.core.report.Report
import java.io.File

private enum class PdfAction { PRINT, SHARE }

/**
 * Botones para imprimir o compartir el cálculo como PDF.
 * Antes de crear el PDF se pide el nombre de la pieza y comentarios opcionales.
 * La explicación "Cómo se calcula" no se imprime.
 */
@Composable
fun ReportButtons(report: Report) {
    val context = LocalContext.current
    var pending by rememberSaveable { mutableStateOf<PdfAction?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var comments by rememberSaveable { mutableStateOf("") }

    fun withPdf(action: (File) -> Unit) {
        runCatching { action(PdfExporter.write(context, report.forPrint(name, comments))) }
            .onFailure { Toast.makeText(context, "No se pudo crear el PDF: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { pending = PdfAction.PRINT }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Print, contentDescription = null)
            Text("Imprimir", modifier = Modifier.padding(start = 6.dp))
        }
        OutlinedButton(onClick = { pending = PdfAction.SHARE }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Text("Compartir", modifier = Modifier.padding(start = 6.dp))
        }
    }

    val action = pending ?: return
    AlertDialog(
        onDismissRequest = { pending = null },
        title = { Text(if (action == PdfAction.PRINT) "Imprimir cálculo" else "Compartir cálculo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del engrane (opcional)") },
                    placeholder = { Text("Ej. Piñón de la caja de avances") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Comentarios (opcional)") },
                    placeholder = { Text("Material, tratamiento, cliente…") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "La hoja lleva los datos y los resultados. La explicación paso a paso no se imprime.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                pending = null
                withPdf {
                    if (action == PdfAction.PRINT) PdfExporter.print(context, it, name.ifBlank { report.title })
                    else PdfExporter.share(context, it)
                }
            }) { Text(if (action == PdfAction.PRINT) "Imprimir" else "Compartir") }
        },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } },
    )
}
