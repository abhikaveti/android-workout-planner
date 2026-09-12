package com.competitivephysique.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.competitivephysique.domain.progression.ProgressionRecommendation
import com.competitivephysique.ui.viewmodel.*

@Composable
fun HomeScreen(plan: String?, next: String?, completedCount: Int, programState: com.competitivephysique.domain.program.ProgramState, onSample: () -> Unit, onImport: () -> Unit, onWorkout: () -> Unit) {
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
                Text("Week " + programState.currentWeek + (programState.phaseName?.let { " • " + it } ?: ""))
                if (programState.phaseStartWeek != null) Text("Phase weeks " + programState.phaseStartWeek + "–" + programState.phaseEndWeek)
                if (programState.programComplete) Text("Program cycle complete. Review results before starting another cycle.")
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

private fun ProgressionRecommendation.message(): String? = when (this) {
    ProgressionRecommendation.NoRecommendation -> null
    is ProgressionRecommendation.IncreaseWeight -> message
    is ProgressionRecommendation.MaintainWeight -> message
    is ProgressionRecommendation.ImproveReps -> message
    is ProgressionRecommendation.ReviewRecovery -> message
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
                state.progression.firstOrNull { it.exercise.id == exercise.id }?.recommendation?.message()?.let { recommendation ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Progression guidance", style = MaterialTheme.typography.labelLarge)
                            Text(recommendation)
                        }
                    }
                }
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
fun ProgressScreen(history: List<HistoryItem>, analytics: com.competitivephysique.domain.analytics.ProgressAnalytics) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium); Text("${analytics.completedWorkouts} completed workout(s)"); Text("${analytics.uniqueExercises} tracked exercise(s) • ${String.format("%.0f", analytics.totalVolumeKg)} kg total volume", style = MaterialTheme.typography.bodyMedium) }\n        item { Text("Performance Records", style = MaterialTheme.typography.titleLarge) }\n        analytics.records.forEach { record -> item { ElevatedCard { Column(Modifier.padding(14.dp)) { Text(record.exerciseName, style = MaterialTheme.typography.titleMedium); Text("Sessions: ${record.sessions}"); Text("Best weight: ${record.bestWeightKg} kg • Best reps: ${record.bestReps}"); Text("Total volume: ${String.format("%.0f", record.totalVolumeKg)} kg") } } } }\n        item { Text("Workout History", style = MaterialTheme.typography.titleLarge) }
        if (history.isEmpty()) item { Text("No completed workouts yet. Complete your first workout to see your history here.") }
        history.forEach { item -> item { ElevatedCard { Column(Modifier.padding(14.dp)) { Text(item.workoutName, style=MaterialTheme.typography.titleMedium); Text("Completed"); item.logs.forEach { log -> Text("Set " + log.setNumber + ": " + log.weightKg + " kg × " + log.reps + " · RIR " + (log.rir ?: "-")) } } } } }
    }
}

@Composable
fun CoachScreen(state: CoachUiState, onGenerate: () -> Unit, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("AI Coach", style = MaterialTheme.typography.headlineMedium)
            Text("Generate a structured coaching prompt from your local training data. No API key or backend is required.")
        }
        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your training context", style = MaterialTheme.typography.titleMedium)
                    Text(if (state.summary.isBlank()) "Generate a prompt to collect your active plan, workout performance and local progression guidance." else state.summary)
                    Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.prompt.isBlank()) "Generate Coaching Prompt" else "Refresh Coaching Prompt")
                    }
                }
            }
        }
        if (state.prompt.isNotBlank()) {
            item {
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ready for ChatGPT", style = MaterialTheme.typography.titleMedium)
                        Text("Copy the prompt below and paste it into ChatGPT for coaching feedback.")
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(state.prompt)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Copy Prompt") }
                        Text(state.prompt, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
            title = { Text("AI Coach") },
            text = { Text(message) }
        )
    }
}


@Composable
fun PlanGenerationScreen(state: PlanGenerationUiState, onUpdate: (PlanGenerationProfile) -> Unit, onGenerate: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val p = state.profile
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Build Your Plan", style = MaterialTheme.typography.headlineMedium); Text("Answer the profile questions, generate a prompt, then use ChatGPT and import the returned JSON.") }
        item { OutlinedTextField(p.goal, { onUpdate(p.copy(goal = it)) }, Modifier.fillMaxWidth(), label = { Text("Goal") }) }
        item { OutlinedTextField(p.experience, { onUpdate(p.copy(experience = it)) }, Modifier.fillMaxWidth(), label = { Text("Experience") }) }
        item { OutlinedTextField(p.trainingDays.toString(), { onUpdate(p.copy(trainingDays = it.toIntOrNull()?.coerceIn(1, 7) ?: p.trainingDays)) }, Modifier.fillMaxWidth(), label = { Text("Training days per week") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        item { OutlinedTextField(p.gymEquipment, { onUpdate(p.copy(gymEquipment = it)) }, Modifier.fillMaxWidth(), label = { Text("Gym equipment") }) }
        item { OutlinedTextField(p.homeEquipment, { onUpdate(p.copy(homeEquipment = it)) }, Modifier.fillMaxWidth(), label = { Text("Home equipment") }) }
        item { OutlinedTextField(p.weakAreas, { onUpdate(p.copy(weakAreas = it)) }, Modifier.fillMaxWidth(), label = { Text("Weak areas") }) }
        item { OutlinedTextField(p.restrictions, { onUpdate(p.copy(restrictions = it)) }, Modifier.fillMaxWidth(), label = { Text("Restrictions / exercises to avoid") }) }
        item { OutlinedTextField(p.preferences, { onUpdate(p.copy(preferences = it)) }, Modifier.fillMaxWidth(), label = { Text("Training preferences") }) }
        item { OutlinedTextField(p.sessionDurationMinutes.toString(), { onUpdate(p.copy(sessionDurationMinutes = it.toIntOrNull()?.coerceIn(20, 240) ?: p.sessionDurationMinutes)) }, Modifier.fillMaxWidth(), label = { Text("Session duration (minutes)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        item { OutlinedTextField(p.physiqueObjective, { onUpdate(p.copy(physiqueObjective = it)) }, Modifier.fillMaxWidth(), label = { Text("Physique objective") }) }
        item { Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) { Text("Generate Plan Prompt") } }
        if (state.prompt.isNotBlank()) item {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ready for ChatGPT", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(state.prompt)) }, modifier = Modifier.fillMaxWidth()) { Text("Copy Prompt") }
                    Text(state.prompt, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}


@Composable
fun PlanAssessmentScreen(state: PlanAssessmentUiState, onUpdate: (PlanAssessmentRequest) -> Unit, onGenerate: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val r = state.request
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Assess Existing Plan", style = MaterialTheme.typography.headlineMedium); Text("Paste an existing plan and generate a structured assessment request for ChatGPT.") }
        item { OutlinedTextField(r.planContent, { onUpdate(r.copy(planContent = it)) }, Modifier.fillMaxWidth().height(220.dp), label = { Text("Existing plan (text or JSON)") }, minLines = 8) }
        item { OutlinedTextField(r.userGoal, { onUpdate(r.copy(userGoal = it)) }, Modifier.fillMaxWidth(), label = { Text("Your goal") }) }
        item { OutlinedTextField(r.weakAreas, { onUpdate(r.copy(weakAreas = it)) }, Modifier.fillMaxWidth(), label = { Text("Weak areas") }) }
        item { OutlinedTextField(r.availableEquipment, { onUpdate(r.copy(availableEquipment = it)) }, Modifier.fillMaxWidth(), label = { Text("Available equipment") }) }
        item { OutlinedTextField(r.recentPerformance, { onUpdate(r.copy(recentPerformance = it)) }, Modifier.fillMaxWidth(), label = { Text("Recent performance notes") }) }
        item { Button(onClick = onGenerate, enabled = r.planContent.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Generate Assessment Prompt") } }
        if (state.prompt.isNotBlank()) item { ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Assessment Prompt", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(state.prompt)) }, modifier = Modifier.fillMaxWidth()) { Text("Copy Prompt") }
            Text(state.prompt, style = MaterialTheme.typography.bodySmall)
        } } }
    }
}
