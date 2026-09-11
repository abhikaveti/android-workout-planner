package com.competitivephysique.domain.coach

object CoachPromptBuilder {
    fun build(context: CoachContext): String = buildString {
        appendLine("You are my competitive physique training coach.")
        appendLine("Use the training data below to provide practical, conservative coaching guidance.")
        appendLine()
        appendLine("TRAINING CONTEXT")
        appendLine("Plan: ${context.planName}")
        appendLine("Primary goal: ${context.goal}")
        appendLine("Current workout: ${context.currentWorkoutName ?: "No workout currently open"}")
        appendLine("Next workout: ${context.nextWorkoutName ?: "Not available"}")
        appendLine("Completed workouts in this plan: ${context.completedWorkoutCount}")
        appendLine()
        appendLine("RECENT EXERCISE PERFORMANCE")
        if (context.exercises.isEmpty()) {
            appendLine("No recent exercise performance is available yet.")
        } else {
            context.exercises.forEach { exercise ->
                appendLine("- ${exercise.name} (target: ${exercise.targetSets} sets × ${exercise.minReps}-${exercise.maxReps} reps)")
                if (exercise.recentSets.isEmpty()) {
                    appendLine("  Recent data: none")
                } else {
                    appendLine(
                        "  Recent sets: " + exercise.recentSets.joinToString("; ") {
                            "${it.weightKg} kg × ${it.reps}" + (it.rir?.let { rir -> " @ RIR $rir" } ?: "")
                        }
                    )
                }
                exercise.progressionGuidance?.let { appendLine("  Local progression guidance: $it") }
            }
        }
        appendLine()
        appendLine("Please respond using exactly these sections:")
        appendLine("1. Performance assessment")
        appendLine("2. Exercise progression suggestions")
        appendLine("3. Recovery or fatigue concerns")
        appendLine("4. Training modifications, if needed")
        appendLine("5. Specific recommendations for my next session")
        appendLine()
        appendLine("Do not invent missing data. Clearly distinguish observations from assumptions.")
    }
}
