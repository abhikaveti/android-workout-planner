# Competitive Physique — UAT Hardening Pass

## Step 1 — Build readiness audit
### What was checked
- Android Gradle plugin configuration
- Kotlin/KAPT configuration
- Compose dependencies
- Room runtime/compiler pairing
- Serialization plugin/dependency
- Android SDK levels

### Result
Static configuration review completed.
This workspace does not contain a Gradle wrapper or Android SDK, so a real APK compilation cannot be honestly executed here.

### Required device-side verification
Open the project in Android Studio and run:
`./gradlew assembleDebug`
or use Android Studio's Run button.

## Step 2 — Data integrity hardening
### Change made
Added indexes for the most frequently queried relationship columns:
- planId
- phaseId
- workoutId
- workoutSessionId
- exerciseDefinitionId

### Why
Workout history grows over time. Indexes reduce lookup cost and make the local database more reliable as logs accumulate.

## Step 3 — Plan replacement safety
### Problem found
Replacing an existing plan could leave obsolete phases/workouts/exercises in local storage.

### Fix
The repository transaction now removes old child definitions for the plan before inserting the new hierarchy.

## Step 4 — Interrupted workout/resume review
### What was tested logically
- Session remains IN_PROGRESS in SQLite.
- Opening Workout checks for an unfinished session.
- Existing session is resumed instead of creating another session for the same active plan.
- Saved sets remain visible because every set is persisted immediately.

## Step 5 — Set logging validation
### Current protections
- Weight cannot be negative.
- Reps must be positive.
- RIR must be 0–10 when supplied.
- Duplicate set saves replace the same logical set instead of creating duplicate rows.

## Step 6 — Next workout progression
### Expected UAT
1. Activate plan.
2. Start first workout.
3. Save sets.
4. Complete workout.
5. Return Home.
6. Next workout should advance.
7. Miss a calendar day.
8. Next workout should remain unchanged.

## Step 7 — Plan import UAT
Test:
- Valid JSON → preview → save → activate.
- Missing plan name → rejected.
- Invalid rep range → rejected.
- Unsupported schema version → rejected.
- Unknown future JSON fields → tolerated.

## Step 8 — App restart persistence
Test:
1. Start workout.
2. Save at least one set.
3. Force-close app.
4. Reopen.
5. Open Workout.
Expected: unfinished workout and saved sets remain available.

## Step 9 — Manual device test matrix
| Test | Expected |
|---|---|
| Fresh install | App launches |
| Sample plan | Activates locally |
| Valid import | Saves and activates |
| Invalid import | Does not alter active plan |
| Save set | Appears immediately |
| Restart app | Saved set remains |
| Resume workout | Same session resumes |
| Complete workout | Session becomes completed |
| Next workout | Advances in sequence |

## Current sign-off status
CODE HARDENING: Complete for this pass.
DEVICE BUILD/INSTALL SIGN-OFF: Pending actual Android Studio/physical-device execution.

## Cost
Recurring infrastructure: ₹0.
