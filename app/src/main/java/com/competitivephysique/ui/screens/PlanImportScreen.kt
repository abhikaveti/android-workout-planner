package com.competitivephysique.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.competitivephysique.ui.viewmodel.AppViewModel

@Composable
fun PlanImportScreen(vm: AppViewModel) {
    val state by vm.import.collectAsState()
    val clipboard = LocalClipboardManager.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importFile)
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Import / Update Training Plan", style = MaterialTheme.typography.headlineMedium)
            Text("Paste JSON, plain text, or Markdown, or upload a plan file. Existing completed workout history is kept unchanged.")
        }
        item {
            OutlinedTextField(
                value = state.rawJson,
                onValueChange = vm::updateImportJson,
                modifier = Modifier.fillMaxWidth().height(280.dp),
                label = { Text("Plan content") },
                placeholder = { Text("Paste JSON, text, or Markdown") },
                minLines = 12
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    launcher.launch(arrayOf(
                        "application/json",
                        "text/plain",
                        "text/markdown",
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "*/*"
                    ))
                }) { Text("Upload Plan File") }
                state.selectedFileName?.let { Text(it, modifier = Modifier.weight(1f), maxLines = 2) }
            }
        }
        item { Text("Supported: JSON • TXT • MD • PDF • DOCX", style = MaterialTheme.typography.bodySmall) }
        item { Button(onClick = vm::validateImport, enabled = state.rawJson.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Validate Plan") } }
        if (state.errors.isNotEmpty()) item {
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Validation issues", style = MaterialTheme.typography.titleMedium)
                    state.errors.forEach { Text("• $it") }
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString("The training plan failed validation. Please correct these issues and return the complete corrected JSON only:\n\n" + state.errors.mapIndexed { index, error -> "${index + 1}. $error" }.joinToString("\n")))
                    }) { Text("Copy Issues for ChatGPT") }
                }
            }
        }
        state.preview?.let { plan ->
            item {
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Plan Ready", style = MaterialTheme.typography.titleLarge)
                        Text(plan.name)
                        Text("Goal: ${plan.goal}")
                        Text("Source: ${state.selectedFileName ?: "pasted content"}")
                        Button(onClick = vm::saveImportedPlan, modifier = Modifier.fillMaxWidth()) { Text("Save & Activate Plan") }
                    }
                }
            }
        }
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismissImportMessage,
            title = { Text("Plan Import") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = vm::dismissImportMessage) { Text("OK") } }
        )
    }
}
