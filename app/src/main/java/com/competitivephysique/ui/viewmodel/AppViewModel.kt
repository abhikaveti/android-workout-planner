package com.competitivephysique.ui.viewmodel

import android.app.Application
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
import com.competitivephysique.domain.progression.ExerciseProgressionInsight
import com.competitivephysique.domain.progression.ProgressionEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WorkoutDisplayStatus { NOT_STARTED, IN_PROGRESS, NEXT, COMPLETED }

data class WorkoutOverviewItem(
    val definition: WorkoutDefinitionEntity,
    val dayNumber: Int,
    val status: WorkoutDisplayStatus
)

data class HistoryItem(
    val session: WorkoutSessionEntity,
    val workoutName: String,
    val logs: List<SetLogEntity>
)

data class ImportUiState(
    val rawJson: String = "",
    val errors: List<String> = emptyList(),
    val preview: TrainingPlan? = null
)

data class PlanAssessmentUiState(
    val request: PlanAssessmentRequest = PlanAssessmentRequest(),
    val prompt: String = ""
)

data class PlanGenerationUiState(
    val profile: PlanGenerationProfile = PlanGenerationProfile(),
    val prompt: String = ""
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
    val completionMessage: String? = null
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
        val completedIds = completed.map { it.workoutDefinitionId }.toSet()
        val current = dao.getCurrentSession()?.takeIf { it.planId == active.id }
        val nextId = _nextWorkout.value?.id

        val phases = dao.getPhases(active.id)
        val definitions = phases.flatMap { dao.getWorkoutsForPhase(it.id) }
        _programState.value = ProgramStateEngine.resolve(phases, definitions, completed.size, _nextWorkout.value)
        _overview.value = definitions.mapIndexed { index, definition ->
            val status = when {
                definition.id in completedIds -> WorkoutDisplayStatus.COMPLETED
                current?.workoutDefinitionId == definition.id -> WorkoutDisplayStatus.IN_PROGRESS
                definition.id == nextId -> WorkoutDisplayStatus.NEXT
                else -> WorkoutDisplayStatus.NOT_STARTED
            }
            WorkoutOverviewItem(definition, index + 1, status)
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

    fun validateImport() {
        when (val result = PlanImporter.parse(_import.value.rawJson)) {
            is PlanImportResult.Success -> _import.value = _import.value.copy(preview = result.plan, errors = emptyList())
            is PlanImportResult.Failure -> _import.value = _import.value.copy(preview = null, errors = result.errors)
        }
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
        val session = if (current != null && current.planId == active.id) {
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
        _workout.value = WorkoutUiState(message = "$completedName completed. Refreshing your next workout.")
        refreshProgramState()
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
        _generation.value = _generation.value.copy(prompt = PlanGenerationPromptBuilder.build(profile))
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
}