package com.competitivephysique.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import com.competitivephysique.data.local.ExerciseDefinitionEntity
import com.competitivephysique.data.local.SetLogEntity
import com.competitivephysique.domain.assessment.PlanAssessmentRequest
import com.competitivephysique.domain.generation.PlanGenerationProfile
import com.competitivephysique.domain.progression.ProgressionRecommendation
import com.competitivephysique.ui.viewmodel.AppViewModel
import com.competitivephysique.ui.viewmodel.CoachUiState
import com.competitivephysique.ui.viewmodel.HistoryItem
import com.competitivephysique.ui.viewmodel.PlanAssessmentUiState
import com.competitivephysique.ui.viewmodel.PlanEditorUiState
import com.competitivephysique.ui.viewmodel.PlanGenerationUiState
import com.competitivephysique.ui.viewmodel.WorkoutDisplayStatus
import com.competitivephysique.ui.viewmodel.WorkoutOverviewItem
import com.competitivephysique.ui.viewmodel.WorkoutUiState

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
fun StatusChip(status: WorkoutDisplayStatus, locked: Boolean = false) {
    if (locked) return

    // NEXT is a scheduling state; visually it belongs to NOT STARTED.
    val displayStatus = when (status) {
        WorkoutDisplayStatus.NEXT -> WorkoutDisplayStatus.NOT_STARTED
        else -> status
    }

    val label = when (displayStatus) {
        WorkoutDisplayStatus.COMPLETED -> "COMPLETED"
        WorkoutDisplayStatus.IN_PROGRESS -> "IN PROGRESS"
        else -> "NOT STARTED"
    }

    val colors = when (displayStatus) {
        WorkoutDisplayStatus.COMPLETED -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFDFF6E5),
            labelColor = Color(0xFF176B36)
        )
        WorkoutDisplayStatus.IN_PROGRESS -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFE1ECFF),
            labelColor = Color(0xFF174EA6)
        )
        else -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFFFF0D5),
            labelColor = Color(0xFF8A5200)
        )
    }

    AssistChip(
        onClick = {},
        enabled = false,
        colors = colors,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutOverviewScreen(
    items: List<WorkoutOverviewItem>,
    currentWeek: Int,
    totalWeeks: Int,
    onSelect: (String) -> Unit,
    onExport: () -> Unit
) {
    var selectedWeek by remember { mutableStateOf(currentWeek.coerceAtLeast(1)) }
    var weekMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentWeek, totalWeeks) {
        if (selectedWeek !in 1..totalWeeks.coerceAtLeast(1)) selectedWeek = currentWeek.coerceAtLeast(1)
    }

    val weekItems = items.filter { it.weekNumber == selectedWeek }
    val selectedLocked = weekItems.isNotEmpty() && weekItems.all { it.locked }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (items.isNotEmpty()) item {
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export Active Plan") }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "WORKOUTS",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = weekMenuExpanded,
                            onExpandedChange = { weekMenuExpanded = !weekMenuExpanded },
                            modifier = Modifier.width(150.dp)
                        ) {
                            AssistChip(
                                onClick = { weekMenuExpanded = !weekMenuExpanded },
                                modifier = Modifier
                                    .menuAnchor()
                                    .width(150.dp),
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Week ${selectedWeek}")
                                        Text(if (weekMenuExpanded) "▲" else "▼")
                                    }
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = weekMenuExpanded,
                                onDismissRequest = { weekMenuExpanded = false }
                            ) {
                                (1..totalWeeks.coerceAtLeast(1)).forEach { week ->
                                    val weekEntries = items.filter { it.weekNumber == week }
                                    val locked = weekEntries.isNotEmpty() && weekEntries.all { it.locked }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when {
                                                    locked -> "Week ${week} • Locked"
                                                    week < currentWeek -> "Week ${week} • Complete"
                                                    week == currentWeek -> "Week ${week} • Current"
                                                    else -> "Week ${week}"
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectedWeek = week
                                            weekMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        if (selectedLocked) "Future week — complete earlier weeks to unlock"
                        else if (selectedWeek < currentWeek) "Completed week — view training history"
                        else "Active plan schedule",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (items.isEmpty()) {
            item { Text("No active plan. Import or activate a plan to see your workouts.") }
        } else if (weekItems.isEmpty()) {
            item { Text("This active plan has no workouts scheduled for Week ${selectedWeek}.") }
        } else {
            weekItems.forEach { workoutItem ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().alpha(if (workoutItem.locked) 0.45f else 1f),
                        onClick = { if (!workoutItem.locked) onSelect(workoutItem.definition.id) }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(workoutItem.definition.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Workout ${workoutItem.dayNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            StatusChip(workoutItem.status, workoutItem.locked)
                        }
                    }
                }
            }
        }
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
fun WorkoutScreen(
    state: WorkoutUiState,
    onLog: (String, Int, String, String, String) -> Unit,
    onEditSet: (String, String, String, String) -> Unit,
    onEditExercise: (String, String, String, String, String, String, String) -> Unit,
    onComplete: () -> Unit,
    onConfirmComplete: () -> Unit,
    onCancelComplete: () -> Unit,
    onDismiss: () -> Unit,
    onAssessResults: () -> Unit
) {
    if (state.session == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) { Text("Workout", style = MaterialTheme.typography.headlineMedium); Text("Select a workout card to start or resume training.") }
    } else {
        val fields = remember(state.session.id) { mutableStateMapOf<String, Triple<String,String,String>>() }
        var editingExercise by remember { mutableStateOf<ExerciseDefinitionEntity?>(null) }
        var editingSet by remember { mutableStateOf<SetLogEntity?>(null) }
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(state.workoutName, style = MaterialTheme.typography.headlineMedium); Text("Session data is saved immediately on this device.") }
            state.exercises.forEach { exercise -> item { ElevatedCard { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { editingExercise = exercise }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit exercise")
                    }
                }
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
                    if (saved != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = { editingSet = saved }, label = { Text("Set " + setNo + " ✓  " + saved.weightKg + " kg × " + saved.reps + " RIR " + (saved.rir ?: "-")) })
                        IconButton(onClick = { editingSet = saved }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit saved set")
                        }
                    }
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
        editingExercise?.let { exercise ->
            ExerciseEditDialog(
                exercise = exercise,
                onDismiss = { editingExercise = null },
                onSave = { name, sets, minReps, maxReps, rest, notes ->
                    onEditExercise(exercise.id, name, sets, minReps, maxReps, rest, notes)
                    editingExercise = null
                }
            )
        }
        editingSet?.let { setLog ->
            SetEditDialog(
                setLog = setLog,
                onDismiss = { editingSet = null },
                onSave = { weight, reps, rir ->
                    onEditSet(setLog.id, weight, reps, rir)
                    editingSet = null
                }
            )
        }
    }
    if (state.confirmCompletion) AlertDialog(onDismissRequest=onCancelComplete,title={Text("Complete workout?")},text={Text(state.completionMessage ?: "")},dismissButton={TextButton(onClick=onCancelComplete){Text("Continue Workout")}},confirmButton={TextButton(onClick=onConfirmComplete){Text("Complete Anyway")}})
    state.message?.let { message -> AlertDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick=onDismiss){Text("OK")}},title={Text("Workout")},text={Text(message)}) }
    if (state.programCompleted) AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Program Complete 🎉") },
        text = { Text("All planned workouts are complete. Great work! Review your results and assess the outcome of this training cycle.") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } },
        confirmButton = { Button(onClick = onAssessResults) { Text("Assess My Results") } }
    )
}

@Composable
private fun ExerciseEditDialog(
    exercise: ExerciseDefinitionEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember(exercise.id) { mutableStateOf(exercise.name) }
    var sets by remember(exercise.id) { mutableStateOf(exercise.targetSets.toString()) }
    var minReps by remember(exercise.id) { mutableStateOf(exercise.minReps.toString()) }
    var maxReps by remember(exercise.id) { mutableStateOf(exercise.maxReps.toString()) }
    var rest by remember(exercise.id) { mutableStateOf(exercise.restSeconds?.toString().orEmpty()) }
    var notes by remember(exercise.id) { mutableStateOf(exercise.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Exercise name") })
                OutlinedTextField(sets, { sets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(minReps, { minReps = it }, modifier = Modifier.weight(1f), label = { Text("Min reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(maxReps, { maxReps = it }, modifier = Modifier.weight(1f), label = { Text("Max reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                OutlinedTextField(rest, { rest = it }, label = { Text("Rest seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { onSave(name, sets, minReps, maxReps, rest, notes) }) { Text("Save") } }
    )
}

@Composable
private fun SetEditDialog(setLog: SetLogEntity, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var weight by remember(setLog.id) { mutableStateOf(setLog.weightKg.toString()) }
    var reps by remember(setLog.id) { mutableStateOf(setLog.reps.toString()) }
    var rir by remember(setLog.id) { mutableStateOf(setLog.rir?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Set ${setLog.setNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(weight, { weight = it }, label = { Text("kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(rir, { rir = it }, label = { Text("RIR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = { onSave(weight, reps, rir) }) { Text("Save") } }
    )
}

@Composable
fun ProgressScreen(history: List<HistoryItem>, analytics: com.competitivephysique.domain.analytics.ProgressAnalytics) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium); Text("${analytics.completedWorkouts} completed workout(s)"); Text("${analytics.uniqueExercises} tracked exercise(s) • ${String.format("%.0f", analytics.totalVolumeKg)} kg total volume", style = MaterialTheme.typography.bodyMedium) }
        item { Text("Performance Records", style = MaterialTheme.typography.titleLarge) }
        analytics.records.forEach { record -> item { ElevatedCard { Column(Modifier.padding(14.dp)) { Text(record.exerciseName, style = MaterialTheme.typography.titleMedium); Text("Sessions: ${record.sessions}"); Text("Best weight: ${record.bestWeightKg} kg • Best reps: ${record.bestReps}"); Text("Total volume: ${String.format("%.0f", record.totalVolumeKg)} kg") } } } }
        item { Text("Workout History", style = MaterialTheme.typography.titleLarge) }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanGenerationScreen(state: PlanGenerationUiState, onUpdate: (PlanGenerationProfile) -> Unit, onGenerate: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val p = state.profile
    var experienceExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.prompt) {
        if (state.prompt.isNotBlank()) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Build Your Plan", style = MaterialTheme.typography.headlineMedium)
            Text("Answer the profile questions, generate a prompt, then use ChatGPT and import the returned JSON.")
        }
        item {
            OutlinedTextField(
                value = p.goal,
                onValueChange = { onUpdate(p.copy(goal = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal") },
                isError = state.errors.containsKey("goal"),
                supportingText = { state.errors["goal"]?.let { Text(it) } }
            )
        }
        item {
            ExposedDropdownMenuBox(expanded = experienceExpanded, onExpandedChange = { experienceExpanded = !experienceExpanded }) {
                OutlinedTextField(
                    value = p.experience,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Experience") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = experienceExpanded) },
                    isError = state.errors.containsKey("experience"),
                    supportingText = { state.errors["experience"]?.let { Text(it) } }
                )
                ExposedDropdownMenu(expanded = experienceExpanded, onDismissRequest = { experienceExpanded = false }) {
                    listOf("Beginner", "Intermediate", "Advanced", "Professional").forEach { level ->
                        DropdownMenuItem(text = { Text(level) }, onClick = {
                            onUpdate(p.copy(experience = level))
                            experienceExpanded = false
                        })
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = p.trainingDays,
                onValueChange = { onUpdate(p.copy(trainingDays = it.filter(Char::isDigit))) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Training days per week") },
                placeholder = { Text("1–7") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.errors.containsKey("trainingDays"),
                supportingText = { state.errors["trainingDays"]?.let { Text(it) } }
            )
        }
        item { OutlinedTextField(p.gymEquipment, { onUpdate(p.copy(gymEquipment = it)) }, Modifier.fillMaxWidth(), label = { Text("Gym equipment") }) }
        item { OutlinedTextField(p.homeEquipment, { onUpdate(p.copy(homeEquipment = it)) }, Modifier.fillMaxWidth(), label = { Text("Home equipment") }) }
        item { OutlinedTextField(p.weakAreas, { onUpdate(p.copy(weakAreas = it)) }, Modifier.fillMaxWidth(), label = { Text("Weak areas") }) }
        item { OutlinedTextField(p.restrictions, { onUpdate(p.copy(restrictions = it)) }, Modifier.fillMaxWidth(), label = { Text("Restrictions / exercises to avoid") }) }
        item { OutlinedTextField(p.preferences, { onUpdate(p.copy(preferences = it)) }, Modifier.fillMaxWidth(), label = { Text("Training preferences") }) }
        item {
            OutlinedTextField(
                value = p.sessionDurationMinutes,
                onValueChange = { onUpdate(p.copy(sessionDurationMinutes = it.filter(Char::isDigit))) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Session duration (minutes)") },
                placeholder = { Text("20–240") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.errors.containsKey("duration"),
                supportingText = { state.errors["duration"]?.let { Text(it) } }
            )
        }
        item { OutlinedTextField(p.physiqueObjective, { onUpdate(p.copy(physiqueObjective = it)) }, Modifier.fillMaxWidth(), label = { Text("Physique objective") }) }
        item { Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) { Text("Generate Plan Prompt") } }
        if (state.prompt.isNotBlank()) item {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ready for ChatGPT", style = MaterialTheme.typography.titleMedium)
                    Text("Your prompt is ready. Copy it and paste it into ChatGPT.")
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(state.prompt)) }, modifier = Modifier.fillMaxWidth()) { Text("Copy Prompt") }
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(state.prompt))
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com")))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Open ChatGPT (Prompt Copied)") }
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


@Composable
fun PlanEditorScreen(state: PlanEditorUiState, onUpdate: (String) -> Unit, onValidate: () -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Plan Revision", style = MaterialTheme.typography.headlineMedium); Text("Edit a plan as JSON, validate it against the canonical schema, then save the revised version. Existing completed workout history is not rewritten.") }
        item { OutlinedTextField(value = state.rawJson, onValueChange = onUpdate, modifier = Modifier.fillMaxWidth().height(300.dp), label = { Text("Revised plan JSON") }, minLines = 12) }
        item { Button(onClick = onValidate, enabled = state.rawJson.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Validate Revision") } }
        if (state.errors.isNotEmpty()) item { ElevatedCard { Column(Modifier.padding(14.dp)) { Text("Validation issues", style = MaterialTheme.typography.titleMedium); state.errors.forEach { Text("• " + it) } } } }
        state.preview?.let { plan -> item { ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Revision Ready", style = MaterialTheme.typography.titleLarge); Text(plan.name); Text("Goal: " + plan.goal); Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save & Activate Revision") } } } } }
    }
    state.message?.let { message -> AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }, title = { Text("Plan Revision") }, text = { Text(message) }) }
}
