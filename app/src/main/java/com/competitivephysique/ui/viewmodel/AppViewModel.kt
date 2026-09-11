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

    private val _overview = MutableStateFlow<List<WorkoutOverviewItem>>(emptyList())
    val overview = _overview.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    private val _import = MutableStateFlow(ImportUiState())
    val import = _import.asStateFlow()

    private val _workout = MutableStateFlow(WorkoutUiState())
    val workout = _workout.asStateFlow()

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
            return@launch
        }

        val completed = dao.getCompletedSessions(active.id)
        val completedIds = completed.map { it.workoutDefinitionId }.toSet()
        val current = dao.getCurrentSession()?.takeIf { it.planId == active.id }
        val nextId = _nextWorkout.value?.id

        val definitions = dao.getPhases(active.id).flatMap { dao.getWorkoutsForPhase(it.id) }
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

    fun dismissWorkoutMessage() {
        _workout.value = _workout.value.copy(message = null)
    }
}