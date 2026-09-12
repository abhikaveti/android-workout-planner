package com.competitivephysique.domain.plan

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object PlanFileParser {
    private val supportedExtensions = setOf("json", "txt", "md", "pdf", "docx")

    fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    fun parse(context: Context, uri: Uri): PlanImportResult {
        val name = displayName(context, uri) ?: "selected file"
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension !in supportedExtensions) {
            return PlanImportResult.Failure(listOf("Unsupported file type. Choose a .json, .txt, .md, .pdf, or .docx plan file."))
        }
        return try {
            when (extension) {
                "json", "txt", "md" -> {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: return PlanImportResult.Failure(listOf("Unable to read $name."))
                    PlanImporter.parse(content)
                }
                "pdf", "docx" -> PlanImportResult.Failure(
                    listOf("$name is supported by the picker, but this build still needs a PDF/DOCX text-extraction dependency before those binary formats can be parsed.")
                )
                else -> PlanImportResult.Failure(listOf("Unsupported file type."))
            }
        } catch (e: Exception) {
            PlanImportResult.Failure(listOf("Unable to read $name.", e.message ?: "Unknown file error."))
        }
    }
}
