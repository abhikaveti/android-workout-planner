package com.competitivephysique.data.repository

import com.competitivephysique.data.local.*
import com.competitivephysique.domain.plan.*
import kotlinx.coroutines.flow.Flow

class PlanRepository(private val dao: CompetitivePhysiqueDao) {
    fun observePlans(): Flow<List<TrainingPlanEntity>> = dao.observePlans()
    fun observeActivePlan(): Flow<TrainingPlanEntity?> = dao.observeActivePlan()

    suspend fun save(plan: TrainingPlan) {
        val validation = PlanValidator.validate(plan)
        require(validation is PlanValidation.Valid) {
            (validation as? PlanValidation.Invalid)?.errors?.joinToString("\n") ?: "Invalid plan"
        }
        val planEntity = TrainingPlanEntity(plan.id, plan.name, plan.goal)
        val phases = plan.phases.mapIndexed { i, p ->
            TrainingPhaseEntity(p.id, plan.id, p.name, p.startWeek, p.endWeek, i)
        }
        val workouts = plan.phases.flatMap { phase ->
            phase.workouts.mapIndexed { i, w -> WorkoutDefinitionEntity(w.id, phase.id, w.name, i) }
        }
        val exercises = plan.phases.flatMap { phase ->
            phase.workouts.flatMap { workout ->
                workout.exercises.mapIndexed { i, e ->
                    ExerciseDefinitionEntity(e.id, workout.id, e.name, i, e.targetSets, e.minReps, e.maxReps, e.restSeconds, e.notes)
                }
            }
        }
        dao.replacePlan(planEntity, phases, workouts, exercises)
    }

    suspend fun updateActiveExercise(
        exerciseId: String,
        name: String,
        targetSets: Int,
        minReps: Int,
        maxReps: Int,
        restSeconds: Int?,
        notes: String?
    ) {
        require(name.isNotBlank()) { "Exercise name is required." }
        require(targetSets > 0) { "Target sets must be positive." }
        require(minReps > 0 && maxReps >= minReps) { "Rep range is invalid." }
        val current = requireNotNull(dao.getExercise(exerciseId)) { "Exercise not found." }
        val workout = requireNotNull(dao.getWorkout(current.workoutId)) { "Workout not found." }
        val phase = requireNotNull(dao.getPhasesForWorkout(workout.phaseId).firstOrNull()) { "Phase not found." }
        val plan = requireNotNull(dao.getPlan(phase.planId)) { "Plan not found." }
        require(plan.isActive) { "Only exercises in the active plan can be edited." }
        dao.updateExercise(
            current.copy(
                name = name.trim(),
                targetSets = targetSets,
                minReps = minReps,
                maxReps = maxReps,
                restSeconds = restSeconds,
                notes = notes?.trim()?.takeIf { it.isNotBlank() }
            )
        )
    }

    suspend fun activate(planId: String) {
        requireNotNull(dao.getPlan(planId)) { "Plan not found." }
        dao.deactivateAllPlans()
        dao.activatePlan(planId)
    }

    suspend fun nextWorkout(): WorkoutDefinitionEntity? {
        val active = dao.getActivePlan() ?: return null
        val phases = dao.getPhases(active.id)
        if (phases.isEmpty()) return null

        val weeklySchedule = buildList {
            for (week in 1..(phases.maxOf { it.endWeek })) {
                val phase = phases.firstOrNull { week in it.startWeek..it.endWeek } ?: continue
                addAll(dao.getWorkoutsForPhase(phase.id))
            }
        }
        if (weeklySchedule.isEmpty()) return null

        val completed = dao.completedSessionCount(active.id)
        return weeklySchedule.getOrNull(completed)
    }

    suspend fun currentActivePlanExport(): TrainingPlan {
        val active = requireNotNull(dao.getActivePlan()) { "No active plan to export." }
        val phases = dao.getPhases(active.id).sortedBy { it.sequenceOrder }
        return TrainingPlan(
            id = active.id,
            name = active.name,
            goal = active.goal,
            phases = phases.map { phase ->
                TrainingPhase(
                    id = phase.id,
                    name = phase.name,
                    startWeek = phase.startWeek,
                    endWeek = phase.endWeek,
                    workouts = dao.getWorkoutsForPhase(phase.id).map { workout ->
                        Workout(
                            id = workout.id,
                            name = workout.name,
                            exercises = dao.getExercisesForWorkout(workout.id).map { exercise ->
                                PlannedExercise(
                                    id = exercise.id,
                                    name = exercise.name,
                                    targetSets = exercise.targetSets,
                                    minReps = exercise.minReps,
                                    maxReps = exercise.maxReps,
                                    restSeconds = exercise.restSeconds,
                                    notes = exercise.notes
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    suspend fun currentActivePlanExportDto(): PlanImportDto {
        val plan = currentActivePlanExport()
        return PlanImportDto(
            schemaVersion = plan.schemaVersion,
            id = plan.id,
            name = plan.name,
            goal = plan.goal,
            phases = plan.phases.map { phase ->
                PhaseImportDto(
                    id = phase.id,
                    name = phase.name,
                    startWeek = phase.startWeek,
                    endWeek = phase.endWeek,
                    workouts = phase.workouts.map { workout ->
                        WorkoutImportDto(
                            id = workout.id,
                            name = workout.name,
                            exercises = workout.exercises.map { exercise ->
                                ExerciseImportDto(
                                    id = exercise.id,
                                    name = exercise.name,
                                    targetSets = exercise.targetSets,
                                    minReps = exercise.minReps,
                                    maxReps = exercise.maxReps,
                                    restSeconds = exercise.restSeconds,
                                    notes = exercise.notes
                                )
                            }
                        )
                    }
                )
            }
        )
    }
}
