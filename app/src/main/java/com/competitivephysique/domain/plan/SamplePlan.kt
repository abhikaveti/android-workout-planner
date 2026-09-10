package com.competitivephysique.domain.plan

object SamplePlan {
    fun competitiveRebuild() = TrainingPlan(
        id = "competitive-rebuild-v1",
        name = "Competitive Physique — Rebuild",
        goal = "Muscle rebuilding and hypertrophy",
        phases = listOf(
            TrainingPhase(
                id = "phase-1", name = "Foundation", startWeek = 1, endWeek = 8,
                workouts = listOf(
                    Workout("push-a", "Push A", listOf(
                        PlannedExercise("incline-press", "Incline Chest Press", 3, 8, 12, 120),
                        PlannedExercise("chest-press", "Machine Chest Press", 3, 8, 12, 120),
                        PlannedExercise("lateral-raise", "Lateral Raise Machine", 4, 10, 15, 75),
                        PlannedExercise("triceps-pressdown", "Cable Triceps Pressdown", 3, 10, 15, 75)
                    )),
                    Workout("pull-a", "Pull A", listOf(
                        PlannedExercise("lat-pulldown", "Lat Pulldown", 3, 8, 12, 120),
                        PlannedExercise("chest-row", "Chest Supported Row", 3, 8, 12, 120),
                        PlannedExercise("rear-delt", "Reverse Pec Deck", 3, 12, 20, 75),
                        PlannedExercise("curl", "EZ Bar Curl", 3, 8, 12, 75)
                    )),
                    Workout("legs-a", "Legs A", listOf(
                        PlannedExercise("leg-press", "Leg Press", 4, 8, 12, 150),
                        PlannedExercise("leg-curl", "Seated Leg Curl", 3, 10, 15, 90),
                        PlannedExercise("leg-extension", "Leg Extension", 3, 10, 15, 75),
                        PlannedExercise("calf-raise", "Standing Calf Raise", 4, 8, 15, 75)
                    ))
                )
            )
        )
    )
}
