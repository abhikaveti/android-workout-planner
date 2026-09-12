package com.competitivephysique.domain.generation

import org.junit.Assert.assertTrue
import org.junit.Test

class PlanGenerationPromptBuilderTest {
    @Test
    fun promptIncludesSchemaRequirements() {
        val profile = PlanGenerationProfile(
            goal = "Muscle building",
            experience = "Intermediate",
            trainingDays = "4",
            sessionDurationMinutes = "75"
        )
        val prompt = PlanGenerationPromptBuilder.build(profile)
        assertTrue(prompt.contains("SCHEMA REQUIREMENTS"))
        assertTrue(prompt.contains("Every phase"))
        assertTrue(prompt.contains("Every workout"))
        assertTrue(prompt.contains("Every exercise"))
    }
}
