package com.competitivephysique.domain.plan

import android.content.Context
import android.net.Uri

/**
 * Converts supported plan documents to text before passing them to the canonical PlanImporter.
 * PDF/DOCX extraction intentionally stays conservative: when no platform-safe text extractor is
 * available, we surface an actionable error rather than pretending the document was parsed.
 */
object PlanFileParser {
    private val supportedExtensions = setOf("json", "txt", "md", "pdf", "docx")

    fun parse(context: Context, uri: Uri): PlanImportResult {
        val name = queryDisplayName(context, uri) ?: "selected file"
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension !in supportedExtensions) {
            return PlanImportResult.Failure(
                listOf("Unsupported file type. Choose a .json, .txt, .md, .pdf, or .docx plan file.")
            )
        }

        return try {
            when (extension) {
                "json", "txt", "md" -> {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: return PlanImportResult.Failure(listOf("Unable to read $name."))
                    PlanImporter.parse(content)
                }
                "pdf", "docx" -> PlanImportResult.Failure(
                    listOf(
                        "$name was selected, but this build does not include a document parser for $extension files yet. " +
                            "Install a PDF/DOCX text-extraction dependency before enabling binary document parsing."
                    )
                )
                else -> PlanImportResult.Failure(listOf("Unsupported file type."))
            }
        } catch (e: Exception) {
            PlanImportResult.Failure(listOf("Unable to read $name.", e.message ?: "Unknown file error."))
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
