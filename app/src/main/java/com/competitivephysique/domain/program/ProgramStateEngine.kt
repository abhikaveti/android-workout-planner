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
        val totalWeeks = ordered.maxOfOrNull { it.endWeek } ?: 1
        val workoutsPerCycle = workouts.size.coerceAtLeast(1)
        val currentWeek = (completedWorkouts / workoutsPerCycle + 1).coerceAtMost(totalWeeks)
        val phase = ordered.firstOrNull { currentWeek in it.startWeek..it.endWeek }
            ?: ordered.lastOrNull()
        val complete = completedWorkouts >= workoutsPerCycle * totalWeeks
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