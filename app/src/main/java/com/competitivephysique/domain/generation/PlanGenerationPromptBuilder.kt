package com.competitivephysique.domain.generation

object PlanGenerationPromptBuilder {
    fun build(profile: PlanGenerationProfile): String = buildString {
        appendLine("Create a personalized competitive physique training plan for me.")
        appendLine("Return ONLY valid JSON matching this app's canonical training-plan schema. Do not wrap JSON in markdown.")
        appendLine()
        appendLine("PROFILE")
        appendLine("Goal: ${profile.goal}")
        appendLine("Experience: ${profile.experience}")
        appendLine("Training days per week: ${profile.trainingDays}")
        appendLine("Gym equipment: ${profile.gymEquipment.ifBlank { "Not specified" }}")
        appendLine("Home equipment: ${profile.homeEquipment.ifBlank { "Not specified" }}")
        appendLine("Weak areas: ${profile.weakAreas.ifBlank { "None specified" }}")
        appendLine("Restrictions: ${profile.restrictions.ifBlank { "None specified" }}")
        appendLine("Preferences: ${profile.preferences.ifBlank { "None specified" }}")
        appendLine("Session duration: ${profile.sessionDurationMinutes} minutes")
        appendLine("Physique objective: ${profile.physiqueObjective.ifBlank { "Not specified" }}")
        appendLine()
        appendLine("Use evidence-informed exercise selection, realistic volume, progressive overload, sensible fatigue management and clear week/phase organization.")
        appendLine("SCHEMA REQUIREMENTS")
        appendLine("Return one complete root plan object with every required field populated.")
        appendLine("Every phase must include its identifier, name, start/end week and sequence order.")
        appendLine("Every workout must include its identifier, name and sequence/day order.")
        appendLine("Every exercise must include its identifier, name, target sets and min/max repetition range.")
        appendLine("Use valid JSON strings for identifiers and names, integers for counts/weeks/reps, and do not omit required arrays.")
        appendLine("Ensure all references and ordering are internally consistent so the JSON can be validated and imported directly.")
    }
}
