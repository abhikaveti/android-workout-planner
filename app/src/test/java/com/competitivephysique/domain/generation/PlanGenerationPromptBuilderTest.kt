package com.competitivephysique.domain.generation

import org.junit.Assert.assertTrue
import org.junit.Test

class PlanGenerationPromptBuilderTest {
    @Test fun includesProfileAndImportInstruction() {
        val prompt = PlanGenerationPromptBuilder.build(PlanGenerationProfile(goal = "Competitive physique", trainingDays = 5))
        assertTrue(prompt.contains("Competitive physique"))
        assertTrue(prompt.contains("5"))
        assertTrue(prompt.contains("Return ONLY valid JSON"))
    }
}