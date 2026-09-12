package com.competitivephysique.domain.assessment

import org.junit.Assert.assertTrue
import org.junit.Test

class PlanAssessmentPromptBuilderTest {
    @Test fun requestsAssessmentAndRevisedImportableJson() {
        val prompt = PlanAssessmentPromptBuilder.build(PlanAssessmentRequest(planContent = "Upper A", userGoal = "Hypertrophy"))
        assertTrue(prompt.contains("Upper A"))
        assertTrue(prompt.contains("Overall assessment"))
        assertTrue(prompt.contains("valid JSON"))
    }
}