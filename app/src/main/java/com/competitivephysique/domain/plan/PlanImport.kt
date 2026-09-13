package com.competitivephysique.domain.plan

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PlanImportDto(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val goal: String,
    val phases: List<PhaseImportDto>
)

@Serializable
data class PhaseImportDto(
    val id: String,
    val name: String,
    val startWeek: Int,
    val endWeek: Int,
    val workouts: List<WorkoutImportDto>
)

@Serializable
data class WorkoutImportDto(
    val id: String,
    val name: String,
    val exercises: List<ExerciseImportDto>
)

@Serializable
data class ExerciseImportDto(
    val id: String,
    val name: String,
    val targetSets: Int,
    val minReps: Int,
    val maxReps: Int,
    val restSeconds: Int? = null,
    val notes: String? = null
)

sealed interface PlanImportResult {
    data class Success(val plan: TrainingPlan) : PlanImportResult
    data class Failure(val errors: List<String>) : PlanImportResult
}

object PlanJsonCodec {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
}

object PlanImporter {
    fun parse(raw: String): PlanImportResult {
        return try {
            val dto = PlanJsonCodec.json.decodeFromString<PlanImportDto>(raw)
            if (dto.schemaVersion != 1) {
                return PlanImportResult.Failure(listOf("Unsupported schemaVersion: ${dto.schemaVersion}."))
            }
            val plan = TrainingPlan(
                schemaVersion = dto.schemaVersion,
                id = dto.id,
                name = dto.name,
                goal = dto.goal,
                phases = dto.phases.map { phase ->
                    TrainingPhase(
                        id = phase.id,
                        name = phase.name,
                        startWeek = phase.startWeek,
                        endWeek = phase.endWeek,
                        workouts = phase.workouts.map { workout ->
                            Workout(
                                id = workout.id,
                                name = workout.name,
                                exercises = workout.exercises.map { exercise ->
                                    PlannedExercise(
                                        id = exercise.id,
                                        name = exercise.name,
                                        targetSets = exercise.targetSets,
                                        minReps = exercise.minReps,
                                        maxReps = exercise.maxReps,
                                        restSeconds = exercise.restSeconds,
                                        notes = exercise.notes
                                    )
                                }
                            )
                        }
                    )
                }
            )
            when (val validation = PlanValidator.validate(plan)) {
                PlanValidation.Valid -> PlanImportResult.Success(plan)
                is PlanValidation.Invalid -> PlanImportResult.Failure(validation.errors)
            }
        } catch (e: Exception) {
            PlanImportResult.Failure(listOf("Unable to parse plan JSON.", e.message ?: "Unknown parsing error."))
        }
    }
}
