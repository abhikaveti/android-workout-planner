package com.competitivephysique.domain.generation

data class PlanGenerationProfile(
    val goal: String = "",
    val experience: String = "",
    val trainingDays: String = "",
    val gymEquipment: String = "",
    val homeEquipment: String = "",
    val weakAreas: String = "",
    val restrictions: String = "",
    val preferences: String = "",
    val sessionDurationMinutes: String = "",
    val physiqueObjective: String = ""
)
