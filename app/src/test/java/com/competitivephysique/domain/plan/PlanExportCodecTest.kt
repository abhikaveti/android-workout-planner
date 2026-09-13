package com.competitivephysique.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanExportCodecTest {
    @Test
    fun exportRoundTripsThroughCanonicalImporter() {
        val plan = SamplePlan.competitiveRebuild()

        val json = PlanExportCodec.encode(plan)
        val result = PlanImporter.parse(json)

        assertTrue(result is PlanImportResult.Success)
        val imported = (result as PlanImportResult.Success).plan
        assertEquals(plan.id, imported.id)
        assertEquals(plan.phases.first().workouts.first().exercises.first(), imported.phases.first().workouts.first().exercises.first())
    }
}
