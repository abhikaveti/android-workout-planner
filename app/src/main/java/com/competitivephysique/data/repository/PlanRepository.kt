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
}
