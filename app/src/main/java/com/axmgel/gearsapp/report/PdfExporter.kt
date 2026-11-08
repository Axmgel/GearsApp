package com.axmgel.gearsapp.report

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.axmgel.gearsapp.core.report.Report
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Convierte un reporte en PDF (hoja carta/A4) para imprimirlo o compartirlo. */
object PdfExporter {

    private const val PAGE_W = 595 // A4 en puntos
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    fun write(context: Context, report: Report): File {
        val doc = PdfDocument()
        val width = (PAGE_W - 2 * MARGIN).toInt()
        val titlePaint = TextPaint().apply { isAntiAlias = true; textSize = 16f; typeface = Typeface.DEFAULT_BOLD }
        val headPaint = TextPaint().apply { isAntiAlias = true; textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
        val bodyPaint = TextPaint().apply { isAntiAlias = true; textSize = 10f }
        val smallPaint = TextPaint().apply { isAntiAlias = true; textSize = 8f; color = 0xFF666666.toInt() }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNumber++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            p.canvas.drawText("GearsApp · página $pageNumber", MARGIN, PAGE_H - MARGIN / 2, smallPaint)
            page = p
            y = MARGIN
        }

        fun draw(text: String, paint: TextPaint, spaceBefore: Float = 0f) {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1f, 1f)
                .build()
            if (page == null || y + spaceBefore + layout.height > PAGE_H - MARGIN) newPage() else y += spaceBefore
            val canvas = page!!.canvas
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height
        }

        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        if (report.name.isNotEmpty()) {
            draw(report.name, titlePaint)
            draw(report.title, headPaint, 2f)
        } else {
            draw(report.title, titlePaint)
        }
        draw(date, smallPaint, 2f)
        if (report.inputs.isNotEmpty()) {
            draw("Datos", headPaint, 12f)
            report.inputs.forEach { (k, v) -> draw("$k: $v", bodyPaint, 2f) }
        }
        report.sections.forEach { section ->
            draw(section.heading, headPaint, 14f)
            section.lines.forEach { draw(it, bodyPaint, 4f) }
        }
        if (report.comments.isNotEmpty()) {
            draw("Comentarios", headPaint, 14f)
            draw(report.comments, bodyPaint, 4f)
        }
        page?.let { doc.finishPage(it) }

        val dir = File(context.cacheDir, "reportes").apply { mkdirs() }
        val safe = report.name.ifEmpty { report.title }.replace(Regex("[^A-Za-z0-9áéíóúñÁÉÍÓÚÑ ]"), "").trim().replace(' ', '_').ifEmpty { "calculo" }
        val file = File(dir, "$safe.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Abre el menú de Android para mandar el PDF (WhatsApp, correo, Drive, etc.). */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Compartir cálculo"))
    }

    /** Manda el PDF a la impresora (o a "Guardar como PDF"). [context] debe ser la pantalla (Activity). */
    fun print(context: Context, file: File, jobName: String) {
        val manager = context.getSystemService(PrintManager::class.java) ?: return
        manager.print(jobName, FilePrintAdapter(file, jobName), PrintAttributes.Builder().build())
    }

    private class FilePrintAdapter(private val file: File, private val name: String) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("$name.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }
}
