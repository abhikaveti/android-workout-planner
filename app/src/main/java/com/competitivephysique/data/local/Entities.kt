package com.competitivephysique.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goal: String,
    val version: Int = 1,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "training_phases", indices = [Index("planId")])
data class TrainingPhaseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val name: String,
    val startWeek: Int,
    val endWeek: Int,
    val sequenceOrder: Int
)

@Entity(tableName = "workout_definitions", indices = [Index("phaseId")])
data class WorkoutDefinitionEntity(
    @PrimaryKey val id: String,
    val phaseId: String,
    val name: String,
    val sequenceOrder: Int
)

@Entity(tableName = "exercise_definitions", indices = [Index("workoutId")])
data class ExerciseDefinitionEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val name: String,
    val sequenceOrder: Int,
    val targetSets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int? = null,
    val notes: String? = null
)

@Entity(tableName = "workout_sessions", indices = [Index("planId"), Index("workoutDefinitionId")])
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val workoutDefinitionId: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: String = "IN_PROGRESS"
)

@Entity(tableName = "set_logs", indices = [Index("workoutSessionId"), Index("exerciseDefinitionId")])
data class SetLogEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String,
    val exerciseDefinitionId: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int? = null,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val key: String,
    val value: String
)
