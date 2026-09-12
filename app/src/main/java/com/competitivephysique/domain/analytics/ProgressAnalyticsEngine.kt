package com.competitivephysique.domain.analytics

data class ExerciseAnalytics(
    val exerciseName: String,
    val sessions: Int,
    val bestWeightKg: Double,
    val bestReps: Int,
    val totalVolumeKg: Double,
    val latestVolumeKg: Double
)

data class ProgressAnalytics(
    val completedWorkouts: Int,
    val uniqueExercises: Int,
    val totalVolumeKg: Double,
    val records: List<ExerciseAnalytics>
)

object ProgressAnalyticsEngine {
    fun build(history: List<com.competitivephysique.ui.viewmodel.HistoryItem>): ProgressAnalytics {
        val grouped = history.flatMap { item ->
            item.logs.map { log -> Triple(item.workoutName, log.exerciseDefinitionId, log) }
        }.groupBy { it.second }

        val records = grouped.map { (_, values) ->
            val logs = values.map { it.third }
            val name = values.first().first
            val volume = logs.sumOf { it.weightKg * it.reps }
            ExerciseAnalytics(
                exerciseName = name,
                sessions = logs.map { it.workoutSessionId }.distinct().size,
                bestWeightKg = logs.maxOfOrNull { it.weightKg } ?: 0.0,
                bestReps = logs.maxOfOrNull { it.reps } ?: 0,
                totalVolumeKg = volume,
                latestVolumeKg = logs.filter { it.workoutSessionId == logs.maxByOrNull { x -> x.completedAt }?.workoutSessionId }.sumOf { it.weightKg * it.reps }
            )
        }.sortedByDescending { it.totalVolumeKg }

        return ProgressAnalytics(
            completedWorkouts = history.size,
            uniqueExercises = grouped.size,
            totalVolumeKg = records.sumOf { it.totalVolumeKg },
            records = records
        )
    }
}