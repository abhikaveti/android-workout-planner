package com.competitivephysique.domain.plan

data class TrainingPlan(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val goal: String,
    val phases: List<TrainingPhase>
)
data class TrainingPhase(
    val id: String,
    val name: String,
    val startWeek: Int,
    val endWeek: Int,
    val workouts: List<Workout>
)
data class Workout(
    val id: String,
    val name: String,
    val exercises: List<PlannedExercise>
)
data class PlannedExercise(
    val id: String,
    val name: String,
    val targetSets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int? = null,
    val notes: String? = null
)

sealed interface PlanValidation {
    data object Valid : PlanValidation
    data class Invalid(val errors: List<String>) : PlanValidation
}

object PlanValidator {
    fun validate(plan: TrainingPlan): PlanValidation {
        val errors = mutableListOf<String>()
        if (plan.id.isBlank()) errors += "Plan id is required."
        if (plan.name.isBlank()) errors += "Plan name is required."
        if (plan.phases.isEmpty()) errors += "At least one phase is required."
        plan.phases.forEach { phase ->
            if (phase.workouts.isEmpty()) errors += "${phase.name}: at least one workout is required."
            if (phase.startWeek > phase.endWeek) errors += "${phase.name}: week range is invalid."
            phase.workouts.forEach { workout ->
                if (workout.exercises.isEmpty()) errors += "${workout.name}: at least one exercise is required."
                workout.exercises.forEach { exercise ->
                    if (exercise.targetSets <= 0) errors += "${exercise.name}: target sets must be positive."
                    if (exercise.minReps <= 0 || exercise.maxReps < exercise.minReps)
                        errors += "${exercise.name}: rep range is invalid."
                }
            }
        }
        return if (errors.isEmpty()) PlanValidation.Valid else PlanValidation.Invalid(errors)
    }
}
