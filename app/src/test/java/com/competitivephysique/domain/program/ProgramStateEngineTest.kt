package com.competitivephysique.domain.program

import com.competitivephysique.data.local.TrainingPhaseEntity
import com.competitivephysique.data.local.WorkoutDefinitionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgramStateEngineTest {
    @Test fun resolvesWeekAndPhase() {
        val phases = listOf(TrainingPhaseEntity("p1","plan","Accumulation",1,4,1))
        val workouts = listOf(WorkoutDefinitionEntity("w1","p1","Upper",1), WorkoutDefinitionEntity("w2","p1","Lower",2))
        val state = ProgramStateEngine.resolve(phases, workouts, 2, workouts[0])
        assertEquals(2, state.currentWeek)
        assertEquals("Accumulation", state.phaseName)
    }
}