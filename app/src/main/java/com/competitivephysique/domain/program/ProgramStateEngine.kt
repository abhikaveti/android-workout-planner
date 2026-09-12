package com.competitivephysique.domain.program

import com.competitivephysique.data.local.TrainingPhaseEntity
import com.competitivephysique.data.local.WorkoutDefinitionEntity

data class ProgramState(
    val currentWeek: Int,
    val phaseName: String?,
    val phaseStartWeek: Int?,
    val phaseEndWeek: Int?,
    val nextWorkoutName: String?,
    val completedWorkouts: Int,
    val programComplete: Boolean
)

object ProgramStateEngine {
    fun resolve(
        phases: List<TrainingPhaseEntity>,
        workouts: List<WorkoutDefinitionEntity>,
        completedWorkouts: Int,
        nextWorkout: WorkoutDefinitionEntity?
    ): ProgramState {
        val ordered = phases.sortedBy { it.sequenceOrder }
        if (ordered.isEmpty()) return ProgramState(1, null, null, null, null, completedWorkouts, true)

        val totalWeeks = ordered.maxOf { it.endWeek }
        val weeklySchedule = buildList {
            for (week in 1..totalWeeks) {
                val phase = ordered.firstOrNull { week in it.startWeek..it.endWeek } ?: continue
                addAll(workouts.filter { it.phaseId == phase.id }.sortedBy { it.sequenceOrder })
            }
        }

        val complete = completedWorkouts >= weeklySchedule.size
        val currentWeek = if (complete) totalWeeks else {
            var consumed = 0
            var resolved = 1
            for (week in 1..totalWeeks) {
                val phase = ordered.firstOrNull { week in it.startWeek..it.endWeek } ?: continue
                val count = workouts.count { it.phaseId == phase.id }
                if (completedWorkouts < consumed + count) {
                    resolved = week
                    break
                }
                consumed += count
                resolved = week.coerceAtMost(totalWeeks)
            }
            resolved
        }

        val phase = ordered.firstOrNull { currentWeek in it.startWeek..it.endWeek }
            ?: ordered.lastOrNull()

        return ProgramState(
            currentWeek = currentWeek,
            phaseName = phase?.name,
            phaseStartWeek = phase?.startWeek,
            phaseEndWeek = phase?.endWeek,
            nextWorkoutName = if (complete) null else nextWorkout?.name,
            completedWorkouts = completedWorkouts,
            programComplete = complete
        )
    }
}
