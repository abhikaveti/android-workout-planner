package com.competitivephysique.data.repository

import com.competitivephysique.data.local.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WorkoutRepository(private val dao: CompetitivePhysiqueDao) {
    suspend fun start(planId: String, workoutId: String): WorkoutSessionEntity {
        val current = dao.getCurrentSession()
        if (current != null && current.planId == planId) return current
        val session = WorkoutSessionEntity(UUID.randomUUID().toString(), planId, workoutId, System.currentTimeMillis())
        dao.upsertWorkoutSession(session)
        return session
    }
    suspend fun current(): WorkoutSessionEntity? = dao.getCurrentSession()
    suspend fun exercises(workoutId: String) = dao.getExercisesForWorkout(workoutId)
    suspend fun logSet(sessionId: String, exerciseId: String, setNumber: Int, weight: Double, reps: Int, rir: Int?) {
        require(weight >= 0) { "Weight cannot be negative." }
        require(reps > 0) { "Reps must be positive." }
        dao.upsertSetLog(SetLogEntity("$sessionId-$exerciseId-$setNumber", sessionId, exerciseId, setNumber, weight, reps, rir))
    }
    fun observeSets(sessionId: String): Flow<List<SetLogEntity>> = dao.observeSetLogs(sessionId)
    suspend fun complete(sessionId: String) = dao.completeSession(sessionId, System.currentTimeMillis())
}
