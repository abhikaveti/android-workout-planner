package com.competitivephysique.domain.plan

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Canonical import-compatible plan encoder with a mandatory parse-and-validate round trip. */
object PlanExportCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(plan: TrainingPlan): String {
        val dto = PlanImportDto(
            schemaVersion = plan.schemaVersion,
            id = plan.id,
            name = plan.name,
            goal = plan.goal,
            phases = plan.phases.map { phase ->
                PhaseImportDto(
                    id = phase.id,
                    name = phase.name,
                    startWeek = phase.startWeek,
                    endWeek = phase.endWeek,
                    workouts = phase.workouts.map { workout ->
                        WorkoutImportDto(
                            id = workout.id,
                            name = workout.name,
                            exercises = workout.exercises.map { exercise ->
                                ExerciseImportDto(
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
        val encoded = json.encodeToString(dto)
        json.decodeFromString<PlanImportDto>(encoded)
        require(PlanImporter.parse(encoded) is PlanImportResult.Success) { "Exported plan failed validation." }
        return encoded
    }
}
