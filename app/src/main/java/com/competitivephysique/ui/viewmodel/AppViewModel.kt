package com.competitivephysique.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.competitivephysique.data.local.*
import com.competitivephysique.data.repository.PlanRepository
import com.competitivephysique.data.repository.WorkoutRepository
import com.competitivephysique.domain.plan.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ImportUiState(
    val rawJson: String = "",
    val errors: List<String> = emptyList(),
    val preview: TrainingPlan? = null
)

data class WorkoutUiState(
    val loading: Boolean = false,
    val session: WorkoutSessionEntity? = null,
    val workoutName: String = "",
    val exercises: List<ExerciseDefinitionEntity> = emptyList(),
    val logs: List<SetLogEntity> = emptyList(),
    val message: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DatabaseProvider.get(application).dao()
    private val plans = PlanRepository(dao)
    private val workouts = WorkoutRepository(dao)

    val activePlan = plans.observeActivePlan().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _nextWorkout = MutableStateFlow<WorkoutDefinitionEntity?>(null)
    val nextWorkout = _nextWorkout.asStateFlow()

    private val _import = MutableStateFlow(ImportUiState())
    val import = _import.asStateFlow()

    private val _workout = MutableStateFlow(WorkoutUiState())
    val workout = _workout.asStateFlow()

    fun seedAndActivateSample() = viewModelScope.launch {
        val plan = SamplePlan.competitiveRebuild()
        plans.save(plan)
        plans.activate(plan.id)
        refreshNextWorkout()
    }

    fun refreshNextWorkout() = viewModelScope.launch {
        _nextWorkout.value = plans.nextWorkout()
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
        refreshNextWorkout()
        _import.value = ImportUiState()
    }

    fun startNextWorkout() = viewModelScope.launch {
        val active = activePlan.value ?: return@launch
        val next = _nextWorkout.value ?: plans.nextWorkout() ?: return@launch
        val existing = workouts.current()
        val session = if (existing != null && existing.planId == active.id) existing
        else workouts.start(active.id, next.id)
        val definition = dao.getWorkout(session.workoutDefinitionId)
        val exercises = workouts.exercises(session.workoutDefinitionId)
        _workout.value = WorkoutUiState(
            session = session,
            workoutName = definition?.name ?: next.name,
            exercises = exercises,
            logs = dao.getSetLogs(session.id)
        )
    }

    fun logSet(exerciseId: String, setNumber: Int, weight: String, reps: String, rir: String) = viewModelScope.launch {
        val session = _workout.value.session ?: return@launch
        val w = weight.toDoubleOrNull()
        val r = reps.toIntOrNull()
        val ri = rir.toIntOrNull()
        if (w == null || r == null || r <= 0) {
            _workout.value = _workout.value.copy(message = "Enter a valid weight and positive reps.")
            return@launch
        }
        if (ri != null && ri !in 0..10) {
            _workout.value = _workout.value.copy(message = "RIR must be between 0 and 10.")
            return@launch
        }
        workouts.logSet(session.id, exerciseId, setNumber, w, r, ri)
        _workout.value = _workout.value.copy(logs = dao.getSetLogs(session.id), message = "Set saved locally.")
    }

    fun completeWorkout() = viewModelScope.launch {
        val session = _workout.value.session ?: return@launch
        workouts.complete(session.id)
        _workout.value = WorkoutUiState()
        refreshNextWorkout()
    }

    fun dismissWorkoutMessage() {
        _workout.value = _workout.value.copy(message = null)
    }
}
