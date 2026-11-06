package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.FitnessRepository
import com.example.sensor.StepTrackerManager
import com.example.ui.screens.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

enum class BottomNavItem(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    ACTIVITY("Activity", Icons.Default.DirectionsWalk, "nav_activity"),
    WORKOUTS("Workouts", Icons.Default.FitnessCenter, "nav_workouts"),
    PROGRESS("Progress", Icons.Default.ShowChart, "nav_progress"),
    PROFILE("Profile", Icons.Default.Person, "nav_profile")
}

enum class SubScreen {
    NONE,
    WORKOUT_PLANS,
    GOALS,
    REMINDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    currentUser: FirebaseUser,
    authRepository: AuthRepository,
    fitnessRepository: FitnessRepository,
    stepTrackerManager: StepTrackerManager,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomNavItem.HOME) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val uid = currentUser.uid

    // Collect Firestore Data Flows
    val userProfile by authRepository.observeUserProfile(uid).collectAsState(initial = null)
    val todayActivity by fitnessRepository.observeTodayActivity(uid).collectAsState(initial = null)
    val activityHistory by fitnessRepository.observeActivityHistory(uid).collectAsState(initial = emptyList())
    val workouts by fitnessRepository.observeWorkouts(uid).collectAsState(initial = emptyList())
    val goals by fitnessRepository.observeGoals(uid).collectAsState(initial = emptyList())
    val workoutPlans by fitnessRepository.observeWorkoutPlans(uid).collectAsState(initial = fitnessRepository.getDefaultPlans())
    val reminders by fitnessRepository.observeReminders(uid).collectAsState(initial = fitnessRepository.getDefaultReminders())
    val weightHistory by fitnessRepository.observeWeightHistory(uid).collectAsState(initial = emptyList())
    val activeWorkoutSession by fitnessRepository.observeActiveWorkoutSession(uid).collectAsState(initial = null)

    // Handle Back Button press for sub-screens
    androidx.activity.compose.BackHandler(enabled = currentSubScreen != SubScreen.NONE) {
        currentSubScreen = SubScreen.NONE
    }

    Scaffold(
        topBar = {
            if (currentSubScreen != SubScreen.NONE) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentSubScreen) {
                                SubScreen.WORKOUT_PLANS -> "Workout Plans"
                                SubScreen.GOALS -> "Fitness Goals"
                                SubScreen.REMINDERS -> "Notifications & Reminders"
                                SubScreen.NONE -> ""
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { currentSubScreen = SubScreen.NONE }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                BottomNavItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = currentTab == item && currentSubScreen == SubScreen.NONE,
                        onClick = {
                            currentTab = item
                            currentSubScreen = SubScreen.NONE
                        },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentSubScreen != SubScreen.NONE) {
                when (currentSubScreen) {
                    SubScreen.WORKOUT_PLANS -> WorkoutPlansScreen(
                        plans = workoutPlans,
                        fitnessRepository = fitnessRepository,
                        onSavePlan = { plan ->
                            coroutineScope.launch { fitnessRepository.saveWorkoutPlan(plan) }
                        }
                    )
                    SubScreen.GOALS -> GoalsScreen(
                        goals = goals,
                        fitnessRepository = fitnessRepository,
                        onSaveGoal = { goal ->
                            coroutineScope.launch { fitnessRepository.saveGoal(goal) }
                        },
                        onDeleteGoal = { id ->
                            coroutineScope.launch { fitnessRepository.deleteGoal(id) }
                        }
                    )
                    SubScreen.REMINDERS -> RemindersScreen(
                        reminders = reminders,
                        onSaveReminder = { reminder ->
                            coroutineScope.launch { fitnessRepository.saveReminder(reminder) }
                        }
                    )
                    SubScreen.NONE -> {}
                }
            } else {
                when (currentTab) {
                    BottomNavItem.HOME -> HomeScreen(
                        userProfile = userProfile,
                        todayActivity = todayActivity,
                        recentWorkouts = workouts,
                        onNavigateToStartWorkout = { currentTab = BottomNavItem.WORKOUTS },
                        onNavigateToLogWorkout = { currentTab = BottomNavItem.WORKOUTS },
                        onNavigateToProgress = { currentTab = BottomNavItem.PROGRESS },
                        onNavigateToGoals = { currentSubScreen = SubScreen.GOALS }
                    )

                    BottomNavItem.ACTIVITY -> ActivityScreen(
                        todayActivity = todayActivity,
                        activityHistory = activityHistory,
                        stepTrackerManager = stepTrackerManager,
                        onUpdateSteps = { steps, isSensor ->
                            coroutineScope.launch {
                                fitnessRepository.updateTodaySteps(uid, steps, isSensor)
                            }
                        }
                    )

                    BottomNavItem.WORKOUTS -> WorkoutScreen(
                        workouts = workouts,
                        activeSession = activeWorkoutSession,
                        fitnessRepository = fitnessRepository,
                        userWeightKg = userProfile?.weight ?: 70.0,
                        onSaveWorkout = { record ->
                            coroutineScope.launch {
                                fitnessRepository.saveWorkout(record)
                            }
                        },
                        onDeleteWorkout = { id ->
                            coroutineScope.launch {
                                fitnessRepository.deleteWorkout(id)
                            }
                        },
                        onUpdateActiveSession = { session ->
                            coroutineScope.launch {
                                fitnessRepository.updateActiveWorkoutSession(uid, session)
                            }
                        }
                    )

                    BottomNavItem.PROGRESS -> ProgressScreen(
                        activities = activityHistory,
                        workouts = workouts
                    )

                    BottomNavItem.PROFILE -> ProfileScreen(
                        userProfile = userProfile,
                        weightHistory = weightHistory,
                        authRepository = authRepository,
                        fitnessRepository = fitnessRepository,
                        onSaveProfile = { profile ->
                            coroutineScope.launch {
                                authRepository.saveUserProfile(profile)
                            }
                        },
                        onAddWeightRecord = { weight ->
                            coroutineScope.launch {
                                fitnessRepository.addWeightRecord(uid, weight)
                            }
                        },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
