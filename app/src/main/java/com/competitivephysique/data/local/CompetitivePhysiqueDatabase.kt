package com.competitivephysique.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrainingPlanEntity::class,
        TrainingPhaseEntity::class,
        WorkoutDefinitionEntity::class,
        ExerciseDefinitionEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        AppStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CompetitivePhysiqueDatabase : RoomDatabase() {
    abstract fun dao(): CompetitivePhysiqueDao
}
