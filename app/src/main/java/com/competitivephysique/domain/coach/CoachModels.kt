package com.competitivephysique.domain.coach

data class CoachSetPerformance(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int?
)

data class CoachExercisePerformance(
    val name: String,
    val targetSets: Int,
    val minReps: Int,
    val maxReps: Int,
    val recentSets: List<CoachSetPerformance>,
    val progressionGuidance: String?
)

data class CoachContext(
    val planName: String,
    val goal: String,
    val currentWorkoutName: String?,
    val nextWorkoutName: String?,
    val completedWorkoutCount: Int,
    val exercises: List<CoachExercisePerformance>
)
