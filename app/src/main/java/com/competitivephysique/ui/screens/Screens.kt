package com.competitivephysique.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.competitivephysique.ui.viewmodel.*

@Composable
fun HomeScreen(plan: String?, next: String?, completedCount: Int, onSample: () -> Unit, onImport: () -> Unit, onWorkout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Training Dashboard", style = MaterialTheme.typography.headlineMedium)
        if (plan == null) {
            Text("No active plan. Import your final ChatGPT-generated plan, or load the sample plan for testing.")
            Button(onClick = onImport) { Text("Import Plan") }
            OutlinedButton(onClick = onSample) { Text("Load Sample Plan") }
        } else ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ACTIVE PLAN", style = MaterialTheme.typography.labelLarge)
                Text(plan, style = MaterialTheme.typography.titleLarge)
                Text("Completed workouts: " + completedCount)
                Text("Next workout: " + (next ?: "Calculating..."))
                Button(onClick = onWorkout, enabled = next != null) { Text("Open Workout") }
                OutlinedButton(onClick = onImport) { Text("Import / Replace Plan") }
            }
        }
    }
}

@Composable
fun PlanImportScreen(vm: AppViewModel) {
    val state by vm.import.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Import Training Plan", style = MaterialTheme.typography.headlineMedium); Text("Paste the structured JSON exported from ChatGPT. The app validates it before saving anything.") }
        item { OutlinedTextField(value = state.rawJson, onValueChange = vm::updateImportJson, modifier = Modifier.fillMaxWidth().height(280.dp), label = { Text("Plan JSON") }, minLines = 12) }
        item { Button(onClick = vm::validateImport, enabled = state.rawJson.isNotBlank()) { Text("Validate Plan") } }
        if (state.errors.isNotEmpty()) item { Card { Column(Modifier.padding(12.dp)) { Text("Validation issues", style = MaterialTheme.typography.titleMedium); state.errors.forEach { Text("• " + it) } } } }
        state.preview?.let { plan -> item { ElevatedCard { Column(Modifier.padding(16.dp)) { Text("Plan Ready", style = MaterialTheme.typography.titleLarge); Text(plan.name); Text("Goal: " + plan.goal); Button(onClick = vm::saveImportedPlan) { Text("Save & Activate Plan") } } } } }
    }
}

@Composable
fun StatusChip(status: WorkoutDisplayStatus) {
    val label = when(status) { WorkoutDisplayStatus.COMPLETED -> "COMPLETED"; WorkoutDisplayStatus.IN_PROGRESS -> "IN PROGRESS"; WorkoutDisplayStatus.NEXT -> "NEXT"; WorkoutDisplayStatus.NOT_STARTED -> "NOT STARTED" }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
fun WorkoutOverviewScreen(items: List<WorkoutOverviewItem>, onSelect: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("WORKOUTS", style = MaterialTheme.typography.headlineMedium); Text("Week 1", style = MaterialTheme.typography.titleMedium) } }
        if (items.isEmpty()) item { Text("No active plan. Import or activate a plan to see your workouts.") }
        items.forEach { item -> item { ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = { onSelect(item.definition.id) }) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(item.definition.name, style = MaterialTheme.typography.titleLarge); Text("Day " + item.dayNumber) }; StatusChip(item.status) } } } }
    }
}

@Composable
fun WorkoutScreen(state: WorkoutUiState, onLog: (String, Int, String, String, String) -> Unit, onComplete: () -> Unit, onConfirmComplete: () -> Unit, onCancelComplete: () -> Unit, onDismiss: () -> Unit) {
    if (state.session == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) { Text("Workout", style = MaterialTheme.typography.headlineMedium); Text("Select a workout card to start or resume training.") }
    } else {
        val fields = remember(state.session.id) { mutableStateMapOf<String, Triple<String,String,String>>() }
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(state.workoutName, style = MaterialTheme.typography.headlineMedium); Text("Session data is saved immediately on this device.") }
            state.exercises.forEach { exercise -> item { ElevatedCard { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                repeat(exercise.targetSets) { index ->
                    val setNo = index + 1; val key = exercise.id + "-" + setNo
                    val saved = state.logs.firstOrNull { it.exerciseDefinitionId == exercise.id && it.setNumber == setNo }
                    if (saved != null) AssistChip(onClick = {}, label = { Text("Set " + setNo + " ✓  " + saved.weightKg + " kg × " + saved.reps + " RIR " + (saved.rir ?: "-")) })
                    else {
                        val current = fields[key] ?: Triple("", "", "")
                        Text("Set " + setNo)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value=current.first,onValueChange={fields[key]=Triple(it,current.second,current.third)},modifier=Modifier.weight(1f),label={Text("kg")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
                            OutlinedTextField(value=current.second,onValueChange={fields[key]=Triple(current.first,it,current.third)},modifier=Modifier.weight(1f),label={Text("Reps")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
                            OutlinedTextField(value=current.third,onValueChange={fields[key]=Triple(current.first,current.second,it)},modifier=Modifier.weight(.8f),label={Text("RIR")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
                        }
                        Button(onClick={val v=fields[key] ?: Triple("","",""); onLog(exercise.id,setNo,v.first,v.second,v.third)}) { Text("Save Set") }
                    }
                }
            } } } }
            item { Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Complete Workout") } }
        }
    }
    if (state.confirmCompletion) AlertDialog(onDismissRequest=onCancelComplete,title={Text("Complete workout?")},text={Text(state.completionMessage ?: "")},dismissButton={TextButton(onClick=onCancelComplete){Text("Continue Workout")}},confirmButton={TextButton(onClick=onConfirmComplete){Text("Complete Anyway")}})
    state.message?.let { message -> AlertDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick=onDismiss){Text("OK")}},title={Text("Workout")},text={Text(message)}) }
}

@Composable
fun ProgressScreen(history: List<HistoryItem>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium); Text(history.size.toString() + " completed workout(s)"); Text("Workout History", style = MaterialTheme.typography.titleLarge) }
        if (history.isEmpty()) item { Text("No completed workouts yet. Complete your first workout to see your history here.") }
        history.forEach { item -> item { ElevatedCard { Column(Modifier.padding(14.dp)) { Text(item.workoutName, style=MaterialTheme.typography.titleMedium); Text("Completed"); item.logs.forEach { log -> Text("Set " + log.setNumber + ": " + log.weightKg + " kg × " + log.reps + " · RIR " + (log.rir ?: "-")) } } } } }
    }
}

@Composable
fun CoachPlaceholder() { Box(Modifier.fillMaxSize().padding(20.dp)) { Text("ChatGPT handoff will generate prompts from your local workout context without an API.") } }
