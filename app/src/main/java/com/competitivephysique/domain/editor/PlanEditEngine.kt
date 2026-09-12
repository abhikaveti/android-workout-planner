package com.competitivephysique.domain.editor

import com.competitivephysique.domain.plan.PlanImportResult
import com.competitivephysique.domain.plan.PlanImporter
import com.competitivephysique.domain.plan.TrainingPlan

data class PlanEditValidation(
    val plan: TrainingPlan? = null,
    val errors: List<String> = emptyList()
)

object PlanEditEngine {
    fun validate(json: String): PlanEditValidation = when (val result = PlanImporter.parse(json)) {
        is PlanImportResult.Success -> PlanEditValidation(plan = result.plan)
        is PlanImportResult.Failure -> PlanEditValidation(errors = result.errors)
    }
}