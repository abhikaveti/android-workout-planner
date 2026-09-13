package com.competitivephysique.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.competitivephysique.data.local.*
import com.competitivephysique.data.repository.PlanRepository
import com.competitivephysique.data.repository.SetValidationResult
import com.competitivephysique.data.repository.SetValidator
import com.competitivephysique.data.repository.WorkoutRepository
import com.competitivephysique.domain.plan.*
import com.competitivephysique.domain.coach.*
import com.competitivephysique.domain.generation.*
import com.competitivephysique.domain.assessment.*
import com.competitivephysique.domain.analytics.*
import com.competitivephysique.domain.program.*
import com.competitivephysique.domain.editor.*
import com.competitivephysique.domain.progression.ExerciseProgressionInsight
import com.competitivephysique.domain.progression.ProgressionEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WorkoutDisplayStatus { NOT_STARTED, IN_PROGRESS, NEXT, COMPLETED }

data class WorkoutOverviewItem(
    val definition: WorkoutDefinitionEntity,
    val weekNumber: Int,
    val dayNumber: Int,
    val status: WorkoutDisplayStatus,
    val locked: Boolean = false
)

data class HistoryItem(
    val session: WorkoutSessionEntity,
    val workoutName: String,
    val logs: List<SetLogEntity>
)

data class ImportUiState(
    val rawJson: String = "",
    val errors: List<String> = emptyList(),
    val preview: TrainingPlan? = null,
    val selectedFileName: String? = null,
    val message: String? = null
)

data class PlanEditorUiState(
    val rawJson: String = "",
    val errors: List<String> = emptyList(),
    val preview: TrainingPlan? = null,
    val message: String? = null
)

data class PlanAssessmentUiState(
    val request: PlanAssessmentRequest = PlanAssessmentRequest(),
    val prompt: String = ""
)

data class PlanGenerationUiState(
    val profile: PlanGenerationProfile = PlanGenerationProfile(),
    val prompt: String = "",
    val errors: Map<String, String> = emptyMap()
)

data class CoachUiState(
    val prompt: String = "",
    val summary: String = "",
    val message: String? = null
)

data class WorkoutUiState(
    val session: WorkoutSessionEntity? = null,
    val workoutName: String = "",
    val exercises: List<ExerciseDefinitionEntity> = emptyList(),
    val logs: List<SetLogEntity> = emptyList(),
    val progression: List<ExerciseProgressionInsight> = emptyList(),
    val message: String? = null,
    val confirmCompletion: Boolean = false,
    val completionMessage: String? = null,
    val programCompleted: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DatabaseProvider.get(application).dao()
    private val plans = PlanRepository(dao)
    private val workouts = WorkoutRepository(dao)

    val activePlan = plans.observeActivePlan().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _nextWorkout = MutableStateFlow<WorkoutDefinitionEntity?>(null)
    val nextWorkout = _nextWorkout.asStateFlow()

    private val _programState = MutableStateFlow(ProgramState(1, null, null, null, null, 0, false))
    val programState = _programState.asStateFlow()

    private val _overview = MutableStateFlow<List<WorkoutOverviewItem>>(emptyList())
    val overview = _overview.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    private val _analytics = MutableStateFlow(ProgressAnalytics(0, 0, 0.0, emptyList()))
    val analytics = _analytics.asStateFlow()

    private val _import = MutableStateFlow(ImportUiState())
    val import = _import.asStateFlow()

    private val _workout = MutableStateFlow(WorkoutUiState())
    val workout = _workout.asStateFlow()

    private val _coach = MutableStateFlow(CoachUiState())
    val coach = _coach.asStateFlow()

    private val _generation = MutableStateFlow(PlanGenerationUiState())
    val generation = _generation.asStateFlow()

    private val _assessment = MutableStateFlow(PlanAssessmentUiState())
    val assessment = _assessment.asStateFlow()

    private val _editor = MutableStateFlow(PlanEditorUiState())
    val editor = _editor.asStateFlow()

    fun seedAndActivateSample() = viewModelScope.launch {
        val plan = SamplePlan.competitiveRebuild()
        plans.save(plan)
        plans.activate(plan.id)
        refreshProgramState()
    }

    fun refreshNextWorkout() = refreshProgramState()

    fun refreshProgramState() = viewModelScope.launch {
        val active = dao.getActivePlan()
        _nextWorkout.value = plans.nextWorkout()
        if (active == null) {
            _overview.value = emptyList()
            _history.value = emptyList()
            _programState.value = ProgramState(1, null, null, null, null, 0, false)
            return@launch
        }

        val completed = dao.getCompletedSessions(active.id)
        val current = dao.getCurrentSession()?.takeIf { it.planId == active.id }

        val phases = dao.getPhases(active.id).sortedBy { it.sequenceOrder }
        val definitions = phases.flatMap { dao.getWorkoutsForPhase(it.id) }
        val resolvedState = ProgramStateEngine.resolve(phases, definitions, completed.size, _nextWorkout.value)
        _programState.value = resolvedState

        val weeklySchedule = buildList<Pair<Int, WorkoutDefinitionEntity>> {
            val totalWeeks = phases.maxOfOrNull { it.endWeek } ?: 0
            for (week in 1..totalWeeks) {
                val phase = phases.firstOrNull { week in it.startWeek..it.endWeek } ?: continue
                dao.getWorkoutsForPhase(phase.id).sortedBy { it.sequenceOrder }.forEach { workout ->
                    add(week to workout)
                }
            }
        }

        _overview.value = weeklySchedule.mapIndexed { occurrenceIndex, (weekNumber, definition) ->
            val locked = !resolvedState.programComplete && weekNumber > resolvedState.currentWeek
            val status = when {
                locked -> WorkoutDisplayStatus.NOT_STARTED
                occurrenceIndex < completed.size -> WorkoutDisplayStatus.COMPLETED
                current != null && occurrenceIndex == completed.size -> WorkoutDisplayStatus.IN_PROGRESS
                occurrenceIndex == completed.size -> WorkoutDisplayStatus.NEXT
                else -> WorkoutDisplayStatus.NOT_STARTED
            }
            val dayNumber = weeklySchedule.take(occurrenceIndex + 1).count { it.first == weekNumber }
            WorkoutOverviewItem(definition, weekNumber, dayNumber, status, locked)
        }

        _history.value = completed.map { session ->
            HistoryItem(
                session = session,
                workoutName = dao.getWorkout(session.workoutDefinitionId)?.name ?: "Workout",
                logs = dao.getSetLogs(session.id)
            )
        }
        _analytics.value = ProgressAnalyticsEngine.build(_history.value)
    }

    fun updateImportJson(value: String) {
        _import.value = _import.value.copy(rawJson = value, errors = emptyList(), preview = null)
    }

    fun importFile(uri: Uri) {
        val context = getApplication<Application>()
        val name = PlanFileParser.displayName(context, uri)
        when (val result = PlanFileParser.parse(context, uri)) {
            is PlanImportResult.Success -> _import.value = ImportUiState(
                rawJson = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty(),
                preview = result.plan,
                selectedFileName = name
            )
            is PlanImportResult.Failure -> _import.value = _import.value.copy(
                selectedFileName = name,
                errors = result.errors,
                preview = null,
                message = result.errors.joinToString("\n")
            )
        }
    }

    fun validateImport() {
        when (val result = PlanImporter.parse(_import.value.rawJson)) {
            is PlanImportResult.Success -> _import.value = _import.value.copy(preview = result.plan, errors = emptyList())
            is PlanImportResult.Failure -> _import.value = _import.value.copy(preview = null, errors = result.errors)
        }
    }

    fun dismissImportMessage() {
        _import.value = _import.value.copy(message = null)
    }

    fun saveImportedPlan() = viewModelScope.launch {
        val plan = _import.value.preview ?: return@launch
        plans.save(plan)
        plans.activate(plan.id)
        refreshProgramState()
        _import.value = ImportUiState()
    }

    fun selectWorkout(workoutId: String) = viewModelScope.launch {
        val active = activePlan.value ?: return@launch
        val current = workouts.current()
        val session = if (
            current != null &&
            current.planId == active.id &&
            current.workoutDefinitionId == workoutId
        ) {
            current
        } else {
            workouts.start(active.id, workoutId)
        }
        loadSession(session)
        refreshProgramState()
    }

    fun startNextWorkout() = viewModelScope.launch {
        val active = activePlan.value ?: return@launch
        val next = _nextWorkout.value ?: plans.nextWorkout() ?: return@launch
        val current = workouts.current()
        val session = if (current != null && current.planId == active.id) current else workouts.start(active.id, next.id)
        loadSession(session)
        refreshProgramState()
    }

    private suspend fun loadSession(session: WorkoutSessionEntity) {
        val definition = dao.getWorkout(session.workoutDefinitionId)
        val exercises = workouts.exercises(session.workoutDefinitionId)
        val progression = exercises.mapNotNull { exercise ->
            val history = workouts.recentExerciseSets(session.planId, exercise.id)
                .groupBy { it.workoutSessionId }
                .toList()
                .sortedByDescending { (_, logs) -> logs.maxOfOrNull { it.completedAt } ?: 0L }
                .map { (_, logs) -> logs.sortedBy { it.setNumber } }
            val recommendation = ProgressionEngine.recommend(exercise, history)
            ExerciseProgressionInsight(exercise, recommendation).takeUnless {
                it.recommendation is com.competitivephysique.domain.progression.ProgressionRecommendation.NoRecommendation
            }
        }
        _workout.value = WorkoutUiState(
            session = session,
            workoutName = definition?.name ?: "Workout",
            exercises = exercises,
            logs = dao.getSetLogs(session.id),
            progression = progression.orEmpty()
        )
    }

    fun logSet(exerciseId: String, setNumber: Int, weight: String, reps: String, rir: String) = viewModelScope.launch {
        val session = _workout.value.session ?: return@launch
        val parsedWeight = weight.trim().toDoubleOrNull()
        val parsedReps = reps.trim().toIntOrNull()
        val parsedRir = rir.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

        when (val validation = SetValidator.validate(parsedWeight, parsedReps, parsedRir)) {
            is SetValidationResult.Invalid -> {
                _workout.value = _workout.value.copy(message = validation.message)
                return@launch
            }
            SetValidationResult.Valid -> Unit
        }

        workouts.logSet(session.id, exerciseId, setNumber, parsedWeight!!, parsedReps!!, parsedRir)
        loadSession(session)
        _workout.value = _workout.value.copy(message = "Set saved locally.")
    }

    fun updateLoggedSet(setLogId: String, weight: String, reps: String, rir: String) = viewModelScope.launch {
        val session = _workout.value.session ?: return@launch
        val parsedWeight = weight.trim().toDoubleOrNull()
        val parsedReps = reps.trim().toIntOrNull()
        val parsedRir = rir.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        when (val validation = SetValidator.validate(parsedWeight, parsedReps, parsedRir)) {
            is SetValidationResult.Invalid -> {
                _workout.value = _workout.value.copy(message = validation.message)
                return@launch
            }
            SetValidationResult.Valid -> Unit
        }
        workouts.updateLoggedSet(setLogId, parsedWeight!!, parsedReps!!, parsedRir)
        loadSession(session)
        _workout.value = _workout.value.copy(message = "Saved set updated.")
    }

    fun updateActiveExercise(
        exerciseId: String,
        name: String,
        targetSets: String,
        minReps: String,
        maxReps: String,
        restSeconds: String,
        notes: String
    ) = viewModelScope.launch {
        val parsedSets = targetSets.trim().toIntOrNull()
        val parsedMinReps = minReps.trim().toIntOrNull()
        val parsedMaxReps = maxReps.trim().toIntOrNull()
        val parsedRest = restSeconds.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val error = when {
            name.isBlank() -> "Exercise name is required."
            parsedSets == null || parsedSets <= 0 -> "Sets must be a positive whole number."
            parsedMinReps == null || parsedMaxReps == null || parsedMinReps <= 0 || parsedMaxReps < parsedMinReps -> "Rep range is invalid."
            parsedRest != null && parsedRest <= 0 -> "Rest must be positive when provided."
            else -> null
        }
        if (error != null) {
            _workout.value = _workout.value.copy(message = error)
            return@launch
        }
        try {
            plans.updateActiveExercise(
                exerciseId, name, parsedSets!!, parsedMinReps!!, parsedMaxReps!!, parsedRest, notes
            )
            val session = _workout.value.session
            if (session != null) loadSession(session)
            refreshProgramState()
            _workout.value = _workout.value.copy(message = "Exercise updated in the active plan.")
        } catch (e: IllegalArgumentException) {
            _workout.value = _workout.value.copy(message = e.message ?: "Unable to update exercise.")
        }
    }

    fun exportActivePlan(uri: Uri) = viewModelScope.launch {
        try {
            val json = plans.exportActivePlanJson()
            val context = getApplication<Application>()
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                ?: error("Unable to open the selected location.")
            _workout.value = _workout.value.copy(message = "Active plan exported.")
        } catch (e: Exception) {
            _workout.value = _workout.value.copy(message = e.message ?: "Unable to export the active plan.")
        }
    }

    fun requestCompleteWorkout() = viewModelScope.launch {
        val state = _workout.value
        val session = state.session ?: return@launch
        val expected = state.exercises.sumOf { it.targetSets }
        val logged = dao.getSetLogs(session.id).size
        val message = when {
            logged == 0 -> "No valid sets have been logged for this workout. Are you sure you want to complete it?"
            logged < expected -> "This workout has incomplete planned sets ($logged of $expected logged). You can continue training or complete it anyway."
            else -> null
        }
        if (message == null) completeWorkoutInternal() else {
            _workout.value = state.copy(confirmCompletion = true, completionMessage = message)
        }
    }

    fun cancelCompletion() {
        _workout.value = _workout.value.copy(confirmCompletion = false, completionMessage = null)
    }

    fun confirmCompleteWorkout() = viewModelScope.launch { completeWorkoutInternal() }

    private suspend fun completeWorkoutInternal() {
        val session = _workout.value.session ?: return
        val completedName = _workout.value.workoutName
        workouts.complete(session.id)
        refreshProgramState()
        val active = dao.getActivePlan()
        val completedCount = active?.let { dao.completedSessionCount(it.id) } ?: 0
        val phases = active?.let { dao.getPhases(it.id) }.orEmpty()
        val definitions = phases.flatMap { dao.getWorkoutsForPhase(it.id) }
        val state = ProgramStateEngine.resolve(phases, definitions, completedCount, plans.nextWorkout())
        _workout.value = WorkoutUiState(
            message = if (state.programComplete) null else "$completedName completed. Week ${state.currentWeek} is now active.",
            programCompleted = state.programComplete
        )
    }

    fun updateEditorJson(value: String) {
        _editor.value = _editor.value.copy(rawJson = value, errors = emptyList(), preview = null, message = null)
    }

    fun validateEditedPlan() {
        val result = PlanEditEngine.validate(_editor.value.rawJson)
        _editor.value = _editor.value.copy(preview = result.plan, errors = result.errors, message = null)
    }

    fun saveEditedPlan() = viewModelScope.launch {
        val plan = _editor.value.preview ?: return@launch
        plans.save(plan)
        plans.activate(plan.id)
        refreshProgramState()
        _editor.value = PlanEditorUiState(message = "Revised plan saved and activated. Historical workout sessions remain unchanged.")
    }

    fun dismissEditorMessage() {
        _editor.value = _editor.value.copy(message = null)
    }

    fun updateAssessmentRequest(request: PlanAssessmentRequest) {
        _assessment.value = PlanAssessmentUiState(request = request, prompt = "")
    }

    fun generateAssessmentPrompt() {
        val request = _assessment.value.request
        _assessment.value = _assessment.value.copy(prompt = PlanAssessmentPromptBuilder.build(request))
    }

    fun updateGenerationProfile(profile: PlanGenerationProfile) {
        _generation.value = PlanGenerationUiState(profile = profile, prompt = "")
    }

    fun generatePlanPrompt() {
        val profile = _generation.value.profile
        val errors = buildMap {
            if (profile.goal.isBlank()) put("goal", "Goal is required")
            if (profile.experience !in listOf("Beginner", "Intermediate", "Advanced", "Professional")) put("experience", "Select your experience level")
            val days = profile.trainingDays.toIntOrNull()
            if (days == null || days !in 1..7) put("trainingDays", "Enter a value from 1 to 7")
            val duration = profile.sessionDurationMinutes.toIntOrNull()
            if (duration == null || duration !in 20..240) put("duration", "Enter a value from 20 to 240 minutes")
        }
        if (errors.isNotEmpty()) {
            _generation.value = _generation.value.copy(prompt = "", errors = errors)
            return
        }
        _generation.value = _generation.value.copy(prompt = PlanGenerationPromptBuilder.build(profile), errors = emptyMap())
    }

    fun generateCoachPrompt() = viewModelScope.launch {
        val active = dao.getActivePlan()
        if (active == null) {
            _coach.value = CoachUiState(message = "Activate a training plan before generating coaching context.")
            return@launch
        }

        val currentSession = workouts.current()?.takeIf { it.planId == active.id }
        val currentWorkout = currentSession?.let { dao.getWorkout(it.workoutDefinitionId) }
        val next = plans.nextWorkout()
        val completedCount = dao.completedSessionCount(active.id)
        val exerciseSource = when {
            currentWorkout != null -> workouts.exercises(currentWorkout.id)
            next != null -> workouts.exercises(next.id)
            else -> emptyList()
        }

        val exercises = exerciseSource.map { exercise ->
            val recentSets = workouts.recentExerciseSets(active.id, exercise.id)
                .groupBy { it.workoutSessionId }
                .toList()
                .sortedByDescending { (_, logs) -> logs.maxOfOrNull { it.completedAt } ?: 0L }
                .take(3)
                .flatMap { (_, logs) -> logs.sortedBy { it.setNumber } }
                .map { CoachSetPerformance(it.setNumber, it.weightKg, it.reps, it.rir) }

            val history = workouts.recentExerciseSets(active.id, exercise.id)
                .groupBy { it.workoutSessionId }
                .toList()
                .sortedByDescending { (_, logs) -> logs.maxOfOrNull { it.completedAt } ?: 0L }
                .map { (_, logs) -> logs.sortedBy { it.setNumber } }
            val guidance = ProgressionEngine.recommend(exercise, history).coachMessage()

            CoachExercisePerformance(
                name = exercise.name,
                targetSets = exercise.targetSets,
                minReps = exercise.minReps,
                maxReps = exercise.maxReps,
                recentSets = recentSets,
                progressionGuidance = guidance
            )
        }

        val context = CoachContext(
            planName = active.name,
            goal = active.goal,
            currentWorkoutName = currentWorkout?.name,
            nextWorkoutName = next?.name,
            completedWorkoutCount = completedCount,
            exercises = exercises
        )
        val prompt = CoachPromptBuilder.build(context)
        _coach.value = CoachUiState(
            prompt = prompt,
            summary = "${active.name} • ${completedCount} completed workout(s) • ${exerciseSource.size} exercise(s) included"
        )
    }

    fun dismissCoachMessage() {
        _coach.value = _coach.value.copy(message = null)
    }

    private fun com.competitivephysique.domain.progression.ProgressionRecommendation.coachMessage(): String? = when (this) {
        com.competitivephysique.domain.progression.ProgressionRecommendation.NoRecommendation -> null
        is com.competitivephysique.domain.progression.ProgressionRecommendation.IncreaseWeight -> message
        is com.competitivephysique.domain.progression.ProgressionRecommendation.MaintainWeight -> message
        is com.competitivephysique.domain.progression.ProgressionRecommendation.ImproveReps -> message
        is com.competitivephysique.domain.progression.ProgressionRecommendation.ReviewRecovery -> message
    }

    fun dismissWorkoutMessage() {
        _workout.value = _workout.value.copy(message = null)
    }

    fun dismissProgramCompletion() {
        _workout.value = _workout.value.copy(programCompleted = false)
    }
}
