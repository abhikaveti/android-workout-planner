package com.competitivephysique.domain.generation

data class PlanGenerationProfile(
    val goal: String = "Muscle building",
    val experience: String = "Intermediate",
    val trainingDays: Int = 4,
    val gymEquipment: String = "",
    val homeEquipment: String = "",
    val weakAreas: String = "",
    val restrictions: String = "",
    val preferences: String = "",
    val sessionDurationMinutes: Int = 75,
    val physiqueObjective: String = ""
)
