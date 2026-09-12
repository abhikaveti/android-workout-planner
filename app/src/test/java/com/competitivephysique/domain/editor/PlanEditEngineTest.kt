package com.competitivephysique.domain.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEditEngineTest {
    @Test fun invalidJsonReturnsErrors() {
        val result = PlanEditEngine.validate("not json")
        assertTrue(result.plan == null)
        assertTrue(result.errors.isNotEmpty())
    }
}