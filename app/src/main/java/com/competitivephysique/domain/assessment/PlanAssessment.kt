package com.competitivephysique.domain.assessment

data class PlanAssessmentRequest(
    val planContent: String = "",
    val userGoal: String = "",
    val weakAreas: String = "",
    val availableEquipment: String = "",
    val recentPerformance: String = ""
)

object PlanAssessmentPromptBuilder {
    fun build(request: PlanAssessmentRequest): String = buildString {
        appendLine("Act as a competitive physique training-program reviewer.")
        appendLine("Assess the existing plan below without inventing missing details.")
        appendLine()
        appendLine("USER CONTEXT")
        appendLine("Goal: ${request.userGoal.ifBlank { "Not specified" }}")
        appendLine("Weak areas: ${request.weakAreas.ifBlank { "Not specified" }}")
        appendLine("Equipment: ${request.availableEquipment.ifBlank { "Not specified" }}")
        appendLine("Recent performance: ${request.recentPerformance.ifBlank { "Not specified" }}")
        appendLine()
        appendLine("EXISTING PLAN")
        appendLine(request.planContent)
        appendLine()
        appendLine("Respond with:")
        appendLine("1. Overall assessment")
        appendLine("2. Exercise selection review")
        appendLine("3. Volume and frequency review")
        appendLine("4. Progression and fatigue concerns")
        appendLine("5. Specific improvements")
        appendLine("6. A revised plan as valid JSON compatible with the application's existing plan importer")
    }
}