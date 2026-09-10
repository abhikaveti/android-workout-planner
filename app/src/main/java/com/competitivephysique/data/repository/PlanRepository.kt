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
        val sequence = dao.getPhases(active.id).flatMap { dao.getWorkoutsForPhase(it.id) }
        if (sequence.isEmpty()) return null
        val last = dao.getLastCompletedSession(active.id)?.workoutDefinitionId
        val index = sequence.indexOfFirst { it.id == last }
        return if (index < 0) sequence.first() else sequence[(index + 1) % sequence.size]
    }
}
