package com.competitivephysique.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.competitivephysique.ui.screens.*
import com.competitivephysique.ui.viewmodel.AppViewModel

private enum class AppTab { HOME, IMPORT, GENERATE, ASSESS, EDIT, WORKOUT, PROGRESS, COACH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitivePhysiqueApp(vm: AppViewModel = viewModel()) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var executing by remember { mutableStateOf(false) }
    val active by vm.activePlan.collectAsState()
    val next by vm.nextWorkout.collectAsState()
    val workout by vm.workout.collectAsState()
    val overview by vm.overview.collectAsState()
    val history by vm.history.collectAsState()
    val analytics by vm.analytics.collectAsState()
    val programState by vm.programState.collectAsState()
    val coach by vm.coach.collectAsState()
    val generation by vm.generation.collectAsState()
    val assessment by vm.assessment.collectAsState()
    val editor by vm.editor.collectAsState()

    LaunchedEffect(active?.id) { vm.refreshProgramState() }

    MaterialTheme {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Competitive Physique") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == AppTab.HOME, { tab = AppTab.HOME; executing = false }, { Icon(Icons.Default.Home, null) }, { Text("Home") })
                    NavigationBarItem(tab == AppTab.IMPORT, { tab = AppTab.IMPORT; executing = false }, { Icon(Icons.Default.UploadFile, null) }, { Text("Plan") })
                    NavigationBarItem(tab == AppTab.GENERATE, { tab = AppTab.GENERATE; executing = false }, { Icon(Icons.Default.AutoAwesome, null) }, { Text("Generate") })
                    NavigationBarItem(tab == AppTab.ASSESS, { tab = AppTab.ASSESS; executing = false }, { Icon(Icons.Default.Search, null) }, { Text("Assess") })
                    NavigationBarItem(tab == AppTab.EDIT, { tab = AppTab.EDIT; executing = false }, { Icon(Icons.Default.Edit, null) }, { Text("Edit") })
                    NavigationBarItem(tab == AppTab.WORKOUT, { tab = AppTab.WORKOUT; executing = false; vm.refreshProgramState() }, { Icon(Icons.Default.FitnessCenter, null) }, { Text("Workout") })
                    NavigationBarItem(tab == AppTab.PROGRESS, { tab = AppTab.PROGRESS; executing = false; vm.refreshProgramState() }, { Icon(Icons.Default.BarChart, null) }, { Text("Progress") })
                    NavigationBarItem(tab == AppTab.COACH, { tab = AppTab.COACH; executing = false }, { Icon(Icons.Default.Chat, null) }, { Text("Coach") })
                }
            }
        ) { padding ->
            Surface(Modifier.padding(padding)) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(active?.name, next?.name, history.size, programState, vm::seedAndActivateSample, { tab = AppTab.IMPORT }, { tab = AppTab.WORKOUT; executing = false })
                    AppTab.IMPORT -> PlanImportScreen(vm)
                    AppTab.GENERATE -> PlanGenerationScreen(generation, vm::updateGenerationProfile, vm::generatePlanPrompt)
                    AppTab.ASSESS -> PlanAssessmentScreen(assessment, vm::updateAssessmentRequest, vm::generateAssessmentPrompt)
                    AppTab.EDIT -> PlanEditorScreen(editor, vm::updateEditorJson, vm::validateEditedPlan, vm::saveEditedPlan, vm::dismissEditorMessage)
                    AppTab.WORKOUT -> if (executing) {
                        WorkoutScreen(workout, vm::logSet, vm::requestCompleteWorkout, vm::confirmCompleteWorkout, vm::cancelCompletion, vm::dismissWorkoutMessage)
                    } else {
                        WorkoutOverviewScreen(overview) { workoutId ->
                            executing = true
                            vm.selectWorkout(workoutId)
                        }
                    }
                    AppTab.PROGRESS -> ProgressScreen(history, analytics)
                    AppTab.COACH -> CoachScreen(coach, vm::generateCoachPrompt, vm::dismissCoachMessage)
                }
            }
        }
    }
}