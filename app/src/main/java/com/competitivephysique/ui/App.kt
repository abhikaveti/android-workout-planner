package com.competitivephysique.ui

import androidx.activity.compose.BackHandler

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
    val backStack = remember { mutableStateListOf<AppTab>() }

    fun navigateTo(destination: AppTab) {
        if (destination != tab) {
            backStack.add(tab)
            tab = destination
        }
        executing = false
    }

    BackHandler(enabled = executing || backStack.isNotEmpty()) {
        if (executing) {
            executing = false
        } else if (backStack.isNotEmpty()) {
            tab = backStack.removeAt(backStack.lastIndex)
        }
    }
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
                    NavigationBarItem(selected = tab == AppTab.HOME, onClick = { navigateTo(AppTab.HOME) }, icon = { Icon(Icons.Default.Home, "Home") }, alwaysShowLabel = false)
                    NavigationBarItem(selected = tab == AppTab.IMPORT, onClick = { navigateTo(AppTab.IMPORT) }, icon = { Icon(Icons.Default.UploadFile, "Plan") }, alwaysShowLabel = false)
                    NavigationBarItem(selected = tab == AppTab.GENERATE, onClick = { navigateTo(AppTab.GENERATE) }, icon = { Icon(Icons.Default.AutoAwesome, "Generate") }, alwaysShowLabel = false)
                    NavigationBarItem(selected = tab == AppTab.ASSESS, onClick = { navigateTo(AppTab.ASSESS) }, icon = { Icon(Icons.Default.Search, "Assess") }, alwaysShowLabel = false)
                    NavigationBarItem(selected = tab == AppTab.EDIT, onClick = { navigateTo(AppTab.EDIT) }, icon = { Icon(Icons.Default.Edit, "Edit") }, alwaysShowLabel = false)
                    NavigationBarItem(selected = tab == AppTab.WORKOUT, onClick = { navigateTo(AppTab.WORKOUT); vm.refreshProgramState() }, icon = { Icon(Icons.Default.FitnessCenter, "Workout") }, alwaysShowLabel = false)
                    NavigationBarItem(selected = tab == AppTab.PROGRESS, onClick = { navigateTo(AppTab.PROGRESS); vm.refreshProgramState() }, icon = { Icon(Icons.Default.BarChart, "Progress") }, alwaysShowLabel = false)
                    NavigationBarItem(selected = tab == AppTab.COACH, onClick = { navigateTo(AppTab.COACH) }, icon = { Icon(Icons.Default.Chat, "Coach") }, alwaysShowLabel = false)
                }
            }
        ) { padding ->
            Surface(Modifier.padding(padding)) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(active?.name, next?.name, history.size, programState, vm::seedAndActivateSample, { navigateTo(AppTab.IMPORT) }, { navigateTo(AppTab.WORKOUT) })
                    AppTab.IMPORT -> PlanImportScreen(vm)
                    AppTab.GENERATE -> PlanGenerationScreen(generation, vm::updateGenerationProfile, vm::generatePlanPrompt)
                    AppTab.ASSESS -> PlanAssessmentScreen(assessment, vm::updateAssessmentRequest, vm::generateAssessmentPrompt)
                    AppTab.EDIT -> PlanEditorScreen(editor, vm::updateEditorJson, vm::validateEditedPlan, vm::saveEditedPlan, vm::dismissEditorMessage)
                    AppTab.WORKOUT -> if (executing) {
                        WorkoutScreen(workout, vm::logSet, vm::requestCompleteWorkout, vm::confirmCompleteWorkout, vm::cancelCompletion, vm::dismissWorkoutMessage) { vm.dismissProgramCompletion(); navigateTo(AppTab.ASSESS) }
                    } else {
                        WorkoutOverviewScreen(
                            items = overview,
                            currentWeek = programState.currentWeek,
                            totalWeeks = overview.maxOfOrNull { it.weekNumber } ?: 1
                        ) { workoutId ->
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