package com.competitivephysique.domain.progression

import com.competitivephysique.data.local.ExerciseDefinitionEntity
import com.competitivephysique.data.local.SetLogEntity

sealed interface ProgressionRecommendation {
    data object NoRecommendation : ProgressionRecommendation
    data class IncreaseWeight(val message: String) : ProgressionRecommendation
    data class MaintainWeight(val message: String) : ProgressionRecommendation
    data class ImproveReps(val message: String) : ProgressionRecommendation
    data class ReviewRecovery(val message: String) : ProgressionRecommendation
}

object ProgressionEngine {
    fun recommend(
        exercise: ExerciseDefinitionEntity,
        recentLogs: List<List<SetLogEntity>>
    ): ProgressionRecommendation {
        if (recentLogs.isEmpty()) return ProgressionRecommendation.NoRecommendation

        val latest = recentLogs.firstOrNull().orEmpty()
        if (latest.isEmpty()) return ProgressionRecommendation.NoRecommendation

        val latestReachedUpperTarget = latest.size >= exercise.targetSets &&
            latest.take(exercise.targetSets).all { it.reps >= exercise.maxReps }

        if (latestReachedUpperTarget) {
            return ProgressionRecommendation.IncreaseWeight(
                "All planned working sets reached the upper rep target of ${exercise.maxReps}. Consider increasing weight next workout."
            )
        }

        val latestWithinRange = latest.take(exercise.targetSets).all {
            it.reps in exercise.minReps..exercise.maxReps
        }

        if (latestWithinRange && latest.isNotEmpty()) {
            return ProgressionRecommendation.MaintainWeight(
                "Performance is within the target range of ${exercise.minReps}–${exercise.maxReps} reps. Maintain weight and build reps."
            )
        }

        if (latest.any { it.reps < exercise.minReps }) {
            val decliningSessions = recentLogs
                .take(3)
                .map { session ->
                    session.take(exercise.targetSets).sumOf { it.reps }
                }
                .let { sums -> sums.size >= 2 && sums.zipWithNext().all { (current, previous) -> current < previous } }

            if (decliningSessions) {
                return ProgressionRecommendation.ReviewRecovery(
                    "Recent performance is declining across multiple workouts. Review recovery, fatigue and training load."
                )
            }

            return ProgressionRecommendation.ImproveReps(
                "One or more working sets missed the minimum target of ${exercise.minReps} reps. Keep the load and improve reps before progressing."
            )
        }

        return ProgressionRecommendation.NoRecommendation
    }
}
