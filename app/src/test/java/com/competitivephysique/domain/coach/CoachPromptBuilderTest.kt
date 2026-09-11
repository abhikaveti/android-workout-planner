package com.competitivephysique.domain.coach

import org.junit.Assert.assertTrue
import org.junit.Test

class CoachPromptBuilderTest {
    @Test
    fun buildsStructuredPromptFromLocalContext() {
        val prompt = CoachPromptBuilder.build(
            CoachContext(
                planName = "Rebuild",
                goal = "Hypertrophy",
                currentWorkoutName = "Upper A",
                nextWorkoutName = "Lower A",
                completedWorkoutCount = 4,
                exercises = listOf(
                    CoachExercisePerformance(
                        name = "Bench Press",
                        targetSets = 3,
                        minReps = 8,
                        maxReps = 12,
                        recentSets = listOf(CoachSetPerformance(1, 80.0, 12, 2)),
                        progressionGuidance = "Consider increasing weight next session."
                    )
                )
            )
        )

        assertTrue(prompt.contains("TRAINING CONTEXT"))
        assertTrue(prompt.contains("Bench Press"))
        assertTrue(prompt.contains("Performance assessment"))
        assertTrue(prompt.contains("Do not invent missing data"))
    }
}
