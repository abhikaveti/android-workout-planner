package com.competitivephysique.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.competitivephysique.ui.screens.CoachPlaceholder
import com.competitivephysique.ui.screens.HomeScreen
import com.competitivephysique.ui.screens.PlanImportScreen
import com.competitivephysique.ui.screens.ProgressPlaceholder
import com.competitivephysique.ui.screens.WorkoutScreen
import com.competitivephysique.ui.viewmodel.AppViewModel

private enum class AppTab {
    HOME,
    IMPORT,
    WORKOUT,
    PROGRESS,
    COACH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitivePhysiqueApp(
    vm: AppViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(AppTab.HOME) }

    val active by vm.activePlan.collectAsState()
    val next by vm.nextWorkout.collectAsState()
    val workout by vm.workout.collectAsState()

    LaunchedEffect(active?.id) {
        vm.refreshNextWorkout()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("Competitive Physique")
                    }
                )
            },

            bottomBar = {
                NavigationBar {

                    NavigationBarItem(
                        selected = tab == AppTab.HOME,
                        onClick = {
                            tab = AppTab.HOME
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = {
                            Text("Home")
                        }
                    )

                    NavigationBarItem(
                        selected = tab == AppTab.IMPORT,
                        onClick = {
                            tab = AppTab.IMPORT
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Plan"
                            )
                        },
                        label = {
                            Text("Plan")
                        }
                    )

                    NavigationBarItem(
                        selected = tab == AppTab.WORKOUT,
                        onClick = {
                            tab = AppTab.WORKOUT
                            vm.startNextWorkout()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "Workout"
                            )
                        },
                        label = {
                            Text("Workout")
                        }
                    )

                    NavigationBarItem(
                        selected = tab == AppTab.PROGRESS,
                        onClick = {
                            tab = AppTab.PROGRESS
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Progress"
                            )
                        },
                        label = {
                            Text("Progress")
                        }
                    )

                    NavigationBarItem(
                        selected = tab == AppTab.COACH,
                        onClick = {
                            tab = AppTab.COACH
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Coach"
                            )
                        },
                        label = {
                            Text("Coach")
                        }
                    )
                }
            }
        ) { paddingValues ->

            Surface(
                modifier = Modifier.padding(paddingValues)
            ) {
                when (tab) {

                    AppTab.HOME -> {
                        HomeScreen(
                            active?.name,
                            next?.name,
                            vm::seedAndActivateSample,
                            { tab = AppTab.IMPORT },
                            {
                                tab = AppTab.WORKOUT
                                vm.startNextWorkout()
                            }
                        )
                    }

                    AppTab.IMPORT -> {
                        PlanImportScreen(vm)
                    }

                    AppTab.WORKOUT -> {
                        WorkoutScreen(
                            workout,
                            vm::logSet,
                            vm::completeWorkout,
                            vm::dismissWorkoutMessage
                        )
                    }

                    AppTab.PROGRESS -> {
                        ProgressPlaceholder()
                    }

                    AppTab.COACH -> {
                        CoachPlaceholder()
                    }
                }
            }
        }
    }
}