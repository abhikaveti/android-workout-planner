package com.competitivephysique.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressAnalyticsEngineTest {
    @Test fun emptyHistoryProducesEmptyAnalytics() {
        val result = ProgressAnalyticsEngine.build(emptyList())
        assertEquals(0, result.completedWorkouts)
        assertEquals(0, result.uniqueExercises)
        assertEquals(0.0, result.totalVolumeKg, 0.0)
    }
}