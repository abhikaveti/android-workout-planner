package com.competitivephysique.domain.progression

import com.competitivephysique.data.local.ExerciseDefinitionEntity

data class ExerciseProgressionInsight(
    val exercise: ExerciseDefinitionEntity,
    val recommendation: ProgressionRecommendation
)
