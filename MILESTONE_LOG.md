# Competitive Physique — Feature-to-Feature Development Log

This file is the running implementation log for the milestone roadmap.

## M0 — Physical Build and UAT Stabilization

**Status:** CODE HARDENING COMPLETE / DEVICE SIGN-OFF PENDING

### Baseline
- Branch started from `master`.
- Baseline commit: `443b6383b54e0476ba270012164e69b7e1c03e6d`.
- Existing MVP functionality retained; no completed feature was rewritten.

### Verified from repository state
- Local Room/SQLite persistence is present.
- Plan import and validation are present.
- Workout session/set persistence is present.
- Workout completion and next-workout progression are present.
- Workout history/progress wiring is present.
- UAT hardening documentation records relationship indexes, plan replacement cleanup, resume behavior, set validation, and import safeguards.

### Verification limitation
A physical Android device/emulator and Android SDK are not available to this development session. Therefore the following cannot be honestly marked as executed here:

- Real APK compilation
- Physical installation
- Launch on a physical device/emulator
- Restart persistence test on-device
- Device-side end-to-end UAT

These remain explicit device-side verification steps for Android Studio or CI.

### Exit decision
The source baseline is suitable for feature development. M0 will be considered fully signed off only after device-side verification is completed externally.

---

## M1 — Smart Local Progression Engine

**Status:** IMPLEMENTED — BUILD/DEVICE VERIFICATION PENDING

### Implementation completed
- Added `ProgressionRecommendation` sealed model.
- Added `ProgressionEngine` with deterministic local rules.
- Added `ExerciseProgressionInsight` result model.
- Added DAO queries for completed exercise sets and current-session exercise sets.
- Exposed recent exercise performance through `WorkoutRepository`.
- Wired progression analysis into `WorkoutUiState`.
- Workout loading derives recommendations from recent completed sessions.
- Workout UI displays progression guidance per exercise when a recommendation exists.
- No database schema migration was introduced.
- No AI/API/backend dependency was introduced.

### Rules implemented
- Upper rep target reached → consider increasing weight.
- Performance inside programmed range → maintain weight/build reps.
- Minimum rep target missed → improve reps before progressing.
- Repeated declining session totals → review recovery/fatigue/training load.

### Verification limitation
The development environment does not provide a Gradle/Android SDK/device execution path, so APK compilation and on-device UAT are not marked complete here.

### Exit criteria status
- ✅ Domain logic implemented
- ✅ Repository/data access implemented
- ✅ UI integration implemented
- ✅ Local-only architecture preserved
- ⏳ Automated/build/device verification pending

---

## M2 — AI Coach Handoff

**Status:** IMPLEMENTED — BUILD/DEVICE VERIFICATION PENDING

### Implementation completed
- Added CoachContext and exercise/set performance models.
- Added CoachPromptBuilder as a dedicated domain component.
- Built coaching context entirely from local Room-backed training data.
- Includes active plan, goal, current workout, next workout, completed workout count, recent exercise sets and local progression guidance.
- Added CoachUiState and ViewModel orchestration.
- Added an AI Coach screen.
- Added prompt generation and refresh.
- Added clipboard copy support.
- Wired the Coach navigation tab to the implemented feature.
- Added focused unit coverage for structured prompt generation.
- No API key, backend or cloud service was introduced.

### User flow
1. Open the Coach tab.
2. Generate coaching context from local training data.
3. Review the generated structured prompt.
4. Copy the prompt.
5. Paste it into ChatGPT.

### Architecture

```text
Local Room Data
   +
Active Plan
   +
Workout History
   +
Local Progression Guidance
        ↓
Coach Context
        ↓
CoachPromptBuilder
        ↓
Copy
        ↓
User → ChatGPT
```

### Exit criteria status
- ✅ Local coaching context implemented
- ✅ Structured prompt implemented
- ✅ Coach UI implemented
- ✅ Copy action implemented
- ✅ No mandatory API/backend
- ⏳ Build/device verification delegated to external environment

---

## M3 — Plan Generation Wizard

**Status:** Planned

Target capability:
- Multi-step questionnaire.
- Persist draft locally.
- Generate structured plan-generation prompt.
- Reuse the existing plan JSON import/validation pipeline.

---

## M4 — Existing Plan Assessment

**Status:** Planned

Target capability:
- Accept an existing plan as text/JSON first.
- Generate an assessment prompt.
- Support improved-plan return through the existing import pipeline.
- Add document formats only after reliable extraction is established.

---

## M5 — Advanced Progress Analytics

**Status:** Planned

Target capability:
- Exercise progression.
- Weight/rep/volume trends.
- Weekly consistency.
- Training streaks.
- Personal records.
- Muscle-group/weekly volume where supported.
- Charts and progress summaries.

---

## M6 — Advanced Program Management

**Status:** Planned

Target capability:
- Program state.
- Week progression.
- Phase progression.
- Deload/rest handling.
- Repeating cycles.
- Phase transitions.
- Program completion.

---

## M7 — Plan Editing and Lifecycle Refinement

**Status:** Planned

Target capability:
- Edit exercises.
- Edit sets/rep ranges.
- Reorder and replace exercises.
- Safely persist plan changes.
- Protect historical workout records.
- Prepare for plan versioning/lifecycle features.

---

## Deferred — Not in Current Roadmap

- Backup
- Export/restore
- Cloud sync
- Traditional backend
- Mandatory paid AI API


---

## M3 — Plan Generation Wizard

**Status:** IMPLEMENTED

- Added PlanGenerationProfile.
- Added PlanGenerationPromptBuilder.
- Added questionnaire-style generation UI for goals, experience, training frequency, equipment, weak areas, restrictions, preferences, session duration and physique objective.
- Added Generate tab and copy-to-ChatGPT flow.
- Prompt explicitly requests import-compatible JSON.
- Reuses the existing JSON import/validation pipeline after ChatGPT generation.
- Added focused prompt builder test.


---

## M4 — Existing Plan Assessment

**Status:** IMPLEMENTED

- Added plain-text/JSON existing-plan input.
- Added structured user context for goals, weak areas, equipment and performance.
- Added PlanAssessmentPromptBuilder.
- Added assessment UI and copy-to-ChatGPT workflow.
- Prompt requests assessment plus revised JSON compatible with the existing importer.
- Added focused prompt test.
