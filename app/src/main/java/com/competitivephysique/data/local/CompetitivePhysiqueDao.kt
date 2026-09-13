package com.competitivephysique.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompetitivePhysiqueDao {
    @Query("SELECT * FROM training_plans ORDER BY createdAt DESC")
    fun observePlans(): Flow<List<TrainingPlanEntity>>

    @Query("SELECT * FROM training_plans WHERE isActive = 1 LIMIT 1")
    fun observeActivePlan(): Flow<TrainingPlanEntity?>

    @Query("SELECT * FROM training_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlan(): TrainingPlanEntity?

    @Query("SELECT * FROM training_plans WHERE id = :id LIMIT 1")
    suspend fun getPlan(id: String): TrainingPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: TrainingPlanEntity)

    @Query("UPDATE training_plans SET isActive = 0")
    suspend fun deactivateAllPlans()

    @Query("UPDATE training_plans SET isActive = 1 WHERE id = :planId")
    suspend fun activatePlan(planId: String)

    @Query("SELECT * FROM training_phases WHERE planId = :planId ORDER BY sequenceOrder")
    suspend fun getPhases(planId: String): List<TrainingPhaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhases(phases: List<TrainingPhaseEntity>)

    @Query("SELECT * FROM workout_definitions WHERE phaseId = :phaseId ORDER BY sequenceOrder")
    suspend fun getWorkoutsForPhase(phaseId: String): List<WorkoutDefinitionEntity>

    @Query("SELECT * FROM workout_definitions WHERE id = :id LIMIT 1")
    suspend fun getWorkout(id: String): WorkoutDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutDefinitionEntity>)

    @Query("SELECT * FROM exercise_definitions WHERE workoutId = :workoutId ORDER BY sequenceOrder")
    suspend fun getExercisesForWorkout(workoutId: String): List<ExerciseDefinitionEntity>

    @Query("SELECT * FROM exercise_definitions WHERE id = :id LIMIT 1")
    suspend fun getExercise(id: String): ExerciseDefinitionEntity?

    @Update
    suspend fun updateExercise(exercise: ExerciseDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseDefinitionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getCurrentSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE planId = :planId AND status = 'COMPLETED' ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLastCompletedSession(planId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE planId = :planId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    suspend fun getCompletedSessions(planId: String): List<WorkoutSessionEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE planId = :planId AND status = 'COMPLETED'")
    suspend fun completedSessionCount(planId: String): Int

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): WorkoutSessionEntity?

    @Query("UPDATE workout_sessions SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :id")
    suspend fun completeSession(id: String, completedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetLog(setLog: SetLogEntity)

    @Query("SELECT * FROM set_logs WHERE workoutSessionId = :sessionId ORDER BY exerciseDefinitionId, setNumber")
    fun observeSetLogs(sessionId: String): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE workoutSessionId = :sessionId ORDER BY exerciseDefinitionId, setNumber")
    suspend fun getSetLogs(sessionId: String): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE id = :id LIMIT 1")
    suspend fun getSetLog(id: String): SetLogEntity?

    @Update
    suspend fun updateSetLog(setLog: SetLogEntity)

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSetLog(id: String)

    @Query("SELECT s.* FROM set_logs s INNER JOIN workout_sessions ws ON ws.id = s.workoutSessionId WHERE ws.planId = :planId AND ws.status = 'COMPLETED' AND s.exerciseDefinitionId = :exerciseId ORDER BY ws.completedAt DESC, s.setNumber ASC")
    suspend fun getCompletedExerciseSets(planId: String, exerciseId: String): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE workoutSessionId = :sessionId AND exerciseDefinitionId = :exerciseId ORDER BY setNumber")
    suspend fun getExerciseSets(sessionId: String, exerciseId: String): List<SetLogEntity>

    @Query("DELETE FROM exercise_definitions WHERE workoutId IN (SELECT id FROM workout_definitions WHERE phaseId IN (SELECT id FROM training_phases WHERE planId = :planId))")
    suspend fun deleteExercisesForPlan(planId: String)

    @Query("DELETE FROM workout_definitions WHERE phaseId IN (SELECT id FROM training_phases WHERE planId = :planId)")
    suspend fun deleteWorkoutsForPlan(planId: String)

    @Query("DELETE FROM training_phases WHERE planId = :planId")
    suspend fun deletePhasesForPlan(planId: String)

    @Transaction
    suspend fun replacePlan(
        plan: TrainingPlanEntity,
        phases: List<TrainingPhaseEntity>,
        workouts: List<WorkoutDefinitionEntity>,
        exercises: List<ExerciseDefinitionEntity>
    ) {
        deleteExercisesForPlan(plan.id)
        deleteWorkoutsForPlan(plan.id)
        deletePhasesForPlan(plan.id)
        upsertPlan(plan)
        if (phases.isNotEmpty()) insertPhases(phases)
        if (workouts.isNotEmpty()) insertWorkouts(workouts)
        if (exercises.isNotEmpty()) insertExercises(exercises)
    }
}