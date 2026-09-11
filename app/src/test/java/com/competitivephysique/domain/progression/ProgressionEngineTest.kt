package com.competitivephysique.domain.progression

import com.competitivephysique.data.local.ExerciseDefinitionEntity
import com.competitivephysique.data.local.SetLogEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {
    private val exercise = ExerciseDefinitionEntity(
        id = "bench",
        workoutId = "push",
        name = "Bench Press",
        sequenceOrder = 1,
        targetSets = 3,
        minReps = 8,
        maxReps = 12
    )

    private fun session(id: String, reps: List<Int>): List<SetLogEntity> = reps.mapIndexed { index, rep ->
        SetLogEntity(
            id = "$id-$index",
            workoutSessionId = id,
            exerciseDefinitionId = exercise.id,
            setNumber = index + 1,
            weightKg = 80.0,
            reps = rep,
            rir = 1,
            completedAt = index.toLong()
        )
    }

    @Test
    fun recommendsIncreaseWhenAllPlannedSetsHitUpperTarget() {
        val result = ProgressionEngine.recommend(exercise, listOf(session("s1", listOf(12, 12, 12))))
        assertTrue(result is ProgressionRecommendation.IncreaseWeight)
    }

    @Test
    fun recommendsMaintainWhenSetsStayInRange() {
        val result = ProgressionEngine.recommend(exercise, listOf(session("s1", listOf(10, 9, 10))))
        assertTrue(result is ProgressionRecommendation.MaintainWeight)
    }

    @Test
    fun recommendsImproveRepsWhenMinimumTargetIsMissed() {
        val result = ProgressionEngine.recommend(exercise, listOf(session("s1", listOf(7, 8, 8))))
        assertTrue(result is ProgressionRecommendation.ImproveReps)
    }

    @Test
    fun flagsRecoveryWhenRecentPerformanceRepeatedlyDeclines() {
        val result = ProgressionEngine.recommend(
            exercise,
            listOf(
                session("s3", listOf(7, 7, 7)),
                session("s2", listOf(8, 8, 8)),
                session("s1", listOf(9, 9, 9))
            )
        )
        assertTrue(result is ProgressionRecommendation.ReviewRecovery)
    }
}
