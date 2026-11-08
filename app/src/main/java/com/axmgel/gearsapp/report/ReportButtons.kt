package com.axmgel.gearsapp.report

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.axmgel.gearsapp.core.report.Report

/** Botones para imprimir o compartir el cálculo como PDF. */
@Composable
fun ReportButtons(report: Report) {
    val context = LocalContext.current
    fun withPdf(action: (java.io.File) -> Unit) {
        runCatching { action(PdfExporter.write(context, report)) }
            .onFailure { Toast.makeText(context, "No se pudo crear el PDF: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { withPdf { PdfExporter.print(context, it, report.title) } }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Print, contentDescription = null)
            Text("Imprimir", modifier = Modifier.padding(start = 6.dp))
        }
        OutlinedButton(onClick = { withPdf { PdfExporter.share(context, it) } }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Text("Compartir", modifier = Modifier.padding(start = 6.dp))
        }
    }
}
