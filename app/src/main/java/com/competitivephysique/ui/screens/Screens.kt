package com.competitivephysique.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.competitivephysique.ui.viewmodel.*

@Composable
fun HomeScreen(plan: String?, next: String?, onSample: () -> Unit, onImport: () -> Unit, onWorkout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Training Dashboard", style = MaterialTheme.typography.headlineMedium)
        if (plan == null) {
            Text("No active plan. Import your final ChatGPT-generated plan, or load the sample plan for testing.")
            Button(onClick = onImport) { Text("Import Plan") }
            OutlinedButton(onClick = onSample) { Text("Load Sample Plan") }
        } else {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(plan, style = MaterialTheme.typography.titleLarge)
                    Text("Next workout: ${next ?: "Calculating..."}")
                    Button(onClick = onWorkout, enabled = next != null) { Text("Start Workout") }
                    OutlinedButton(onClick = onImport) { Text("Import / Replace Plan") }
                }
            }
        }
    }
}

@Composable
fun PlanImportScreen(vm: AppViewModel) {
    val state by vm.import.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Import Training Plan", style = MaterialTheme.typography.headlineMedium)
            Text("Paste the structured JSON exported from ChatGPT. The app validates it before saving anything.")
        }
        item {
            OutlinedTextField(
                value = state.rawJson,
                onValueChange = vm::updateImportJson,
                modifier = Modifier.fillMaxWidth().height(280.dp),
                label = { Text("Plan JSON") },
                minLines = 12
            )
        }
        item {
            Button(onClick = vm::validateImport, enabled = state.rawJson.isNotBlank()) { Text("Validate Plan") }
        }
        if (state.errors.isNotEmpty()) item {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Validation issues", style = MaterialTheme.typography.titleMedium)
                    state.errors.forEach { Text("• $it") }
                }
            }
        }
        state.preview?.let { plan ->
            item {
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Plan Ready", style = MaterialTheme.typography.titleLarge)
                        Text(plan.name)
                        Text("Goal: ${plan.goal}")
                        Text("${plan.phases.size} phase(s)")
                        plan.phases.forEach { phase ->
                            Text("${phase.name}: ${phase.workouts.joinToString { it.name }}")
                        }
                        Button(onClick = vm::saveImportedPlan) { Text("Save & Activate Plan") }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onLog: (String, Int, String, String, String) -> Unit,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state.session == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Workout", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Select Workout from the navigation bar after activating a plan.")
        }
        return
    }

    val fields = remember(state.session.id) { mutableStateMapOf<String, Triple<String,String,String>>() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(state.workoutName, style = MaterialTheme.typography.headlineMedium)
            Text("Session data is saved immediately on this device.")
        }

        state.exercises.forEach { exercise ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                        Text("${exercise.targetSets} sets × ${exercise.minReps}–${exercise.maxReps} reps")

                        repeat(exercise.targetSets) { index ->
                            val setNo = index + 1
                            val key = "${exercise.id}-$setNo"
                            val saved = state.logs.firstOrNull { it.exerciseDefinitionId == exercise.id && it.setNumber == setNo }
                            if (saved != null) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Set $setNo ✓  ${saved.weightKg} kg × ${saved.reps}  RIR ${saved.rir ?: "-"}") }
                                )
                            } else {
                                val current = fields[key] ?: Triple("", "", "")
                                Text("Set $setNo", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = current.first,
                                        onValueChange = { fields[key] = Triple(it, current.second, current.third) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("kg") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = current.second,
                                        onValueChange = { fields[key] = Triple(current.first, it, current.third) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Reps") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = current.third,
                                        onValueChange = { fields[key] = Triple(current.first, current.second, it) },
                                        modifier = Modifier.weight(0.8f),
                                        label = { Text("RIR") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                                Button(onClick = {
                                    val v = fields[key] ?: Triple("", "", "")
                                    onLog(exercise.id, setNo, v.first, v.second, v.third)
                                }) { Text("Save Set") }
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Complete Workout") }
        }
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
            title = { Text("Workout") },
            text = { Text(message) }
        )
    }
}

@Composable
fun ProgressPlaceholder() {
    Box(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Progress analytics will use the set history you are now logging locally.")
    }
}

@Composable
fun CoachPlaceholder() {
    Box(Modifier.fillMaxSize().padding(20.dp)) {
        Text("ChatGPT handoff will generate prompts from your local workout context without an API.")
    }
}
