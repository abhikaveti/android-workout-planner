package com.competitivephysique.data.repository

import com.competitivephysique.data.local.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed interface SetValidationResult {
    data object Valid : SetValidationResult
    data class Invalid(val message: String) : SetValidationResult
}

object SetValidator {
    fun validate(weight: Double?, reps: Int?, rir: Int?): SetValidationResult {
        if (weight == null || !weight.isFinite() || weight < 0.0) return SetValidationResult.Invalid("Weight must be a valid non-negative number.")
        if (reps == null || reps <= 0) return SetValidationResult.Invalid("Reps must be a positive whole number.")
        if (rir != null && rir !in 0..10) return SetValidationResult.Invalid("RIR must be between 0 and 10.")
        return SetValidationResult.Valid
    }
}

class WorkoutRepository(private val dao: CompetitivePhysiqueDao) {
    suspend fun start(planId: String, workoutId: String): WorkoutSessionEntity {
        val current = dao.getCurrentSession()
        if (
            current != null &&
            current.planId == planId &&
            current.workoutDefinitionId == workoutId
        ) return current
        val session = WorkoutSessionEntity(UUID.randomUUID().toString(), planId, workoutId, System.currentTimeMillis())
        dao.upsertWorkoutSession(session)
        return session
    }

    suspend fun current(): WorkoutSessionEntity? = dao.getCurrentSession()
    suspend fun exercises(workoutId: String) = dao.getExercisesForWorkout(workoutId)

    suspend fun logSet(sessionId: String, exerciseId: String, setNumber: Int, weight: Double, reps: Int, rir: Int?) {
        val validation = SetValidator.validate(weight, reps, rir)
        require(validation is SetValidationResult.Valid) {
            (validation as SetValidationResult.Invalid).message
        }
        dao.upsertSetLog(
            SetLogEntity(
                id = "$sessionId-$exerciseId-$setNumber",
                workoutSessionId = sessionId,
                exerciseDefinitionId = exerciseId,
                setNumber = setNumber,
                weightKg = weight,
                reps = reps,
                rir = rir
            )
        )
    }

    suspend fun updateLoggedSet(setLogId: String, weight: Double, reps: Int, rir: Int?) {
        val validation = SetValidator.validate(weight, reps, rir)
        require(validation is SetValidationResult.Valid) {
            (validation as SetValidationResult.Invalid).message
        }
        val existing = requireNotNull(dao.getSetLog(setLogId)) { "Saved set not found." }
        dao.updateSetLog(existing.copy(weightKg = weight, reps = reps, rir = rir))
    }

    fun observeSets(sessionId: String): Flow<List<SetLogEntity>> = dao.observeSetLogs(sessionId)
    suspend fun logs(sessionId: String): List<SetLogEntity> = dao.getSetLogs(sessionId)
    suspend fun completed(planId: String): List<WorkoutSessionEntity> = dao.getCompletedSessions(planId)
    suspend fun recentExerciseSets(planId: String, exerciseId: String): List<SetLogEntity> = dao.getCompletedExerciseSets(planId, exerciseId)
    suspend fun exerciseSets(sessionId: String, exerciseId: String): List<SetLogEntity> = dao.getExerciseSets(sessionId, exerciseId)
    suspend fun complete(sessionId: String) = dao.completeSession(sessionId, System.currentTimeMillis())
}
