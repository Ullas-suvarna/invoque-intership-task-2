package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ActivityRecord
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutRecord
import com.example.ui.components.FitCircularProgress
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    todayActivity: ActivityRecord?,
    recentWorkouts: List<WorkoutRecord>,
    onNavigateToStartWorkout: () -> Unit,
    onNavigateToLogWorkout: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val currentDateStr = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    val steps = todayActivity?.steps ?: 0
    val stepGoal = 10000
    val stepProgress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)

    val calories = todayActivity?.caloriesBurned ?: 0
    val calorieGoal = 500
    val calorieProgress = (calories.toFloat() / calorieGoal).coerceIn(0f, 1f)

    val activeMin = todayActivity?.activeMinutes ?: 0
    val activeMinGoal = 60
    val activeMinProgress = (activeMin.toFloat() / activeMinGoal).coerceIn(0f, 1f)

    val completedWorkoutsCount = recentWorkouts.count {
        it.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val workoutGoalCount = 3
    val workoutProgress = (completedWorkoutsCount.toFloat() / workoutGoalCount).coerceIn(0f, 1f)

    // Dynamic Motivational Message
    val avgProgress = (stepProgress + calorieProgress + activeMinProgress + workoutProgress) / 4f
    val motivationalMessage = when {
        avgProgress >= 1.0f -> "🏆 Outstanding job! You have crushed all your fitness targets today!"
        avgProgress >= 0.75f -> "🔥 High energy day! You're super close to hitting your daily targets!"
        avgProgress >= 0.40f -> "⚡ Great momentum! Keep moving to achieve your daily fitness goals."
        else -> "💪 Ready to get active? Take a walk or start a workout session now!"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, ${userProfile?.fullName?.ifEmpty { "Athlete" } ?: "Athlete"} 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = currentDateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Profile Avatar",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Banner Graphic
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = coil.compose.rememberAsyncImagePainter(model = R.drawable.img_fitness_hero_1793930454435),
                    contentDescription = "Fitness Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0F172A).copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "FUEL YOUR AMBITION",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track Workouts &\nAchieve Daily Goals",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Motivational Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = motivationalMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Circular Progress Ring & Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onNavigateToProgress) {
                Text("Detailed View", color = Color(0xFF0EA5E9))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FitCircularProgress(
                progress = stepProgress,
                size = 130.dp,
                centerValue = String.format("%,d", steps),
                centerTarget = "10k",
                label = "Steps",
                primaryColor = Color(0xFF10B981),
                secondaryColor = Color(0xFF0EA5E9)
            )

            FitCircularProgress(
                progress = calorieProgress,
                size = 130.dp,
                centerValue = "$calories",
                centerTarget = "500 kcal",
                label = "Calories",
                primaryColor = Color(0xFFF59E0B),
                secondaryColor = Color(0xFFEF4444)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dashboard Stat Cards Grid
        Text(
            text = "Daily Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                title = "Steps Today",
                value = String.format("%,d", steps),
                target = "Goal: 10,000",
                progress = stepProgress,
                icon = Icons.Default.DirectionsWalk,
                color = Color(0xFF10B981),
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_steps")
            )

            DashboardStatCard(
                title = "Calories Burned",
                value = "$calories kcal",
                target = "Goal: 500 kcal",
                progress = calorieProgress,
                icon = Icons.Default.LocalFireDepartment,
                color = Color(0xFFEF4444),
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_calories")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                title = "Active Minutes",
                value = "$activeMin min",
                target = "Goal: 60 min",
                progress = activeMinProgress,
                icon = Icons.Default.Timer,
                color = Color(0xFF0EA5E9),
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_active_minutes")
            )

            DashboardStatCard(
                title = "Workouts Done",
                value = "$completedWorkoutsCount",
                target = "Goal: 3 session",
                progress = workoutProgress,
                icon = Icons.Default.FitnessCenter,
                color = Color(0xFF8B5CF6),
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_workouts")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Access Action Grid
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                label = "Start Workout",
                icon = Icons.Default.PlayArrow,
                containerColor = Color(0xFF10B981),
                onClick = onNavigateToStartWorkout,
                modifier = Modifier.weight(1f).testTag("quick_start_workout")
            )

            QuickActionButton(
                label = "Log Workout",
                icon = Icons.Default.AddTask,
                containerColor = Color(0xFF0EA5E9),
                onClick = onNavigateToLogWorkout,
                modifier = Modifier.weight(1f).testTag("quick_log_workout")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                label = "View Progress",
                icon = Icons.Default.ShowChart,
                containerColor = Color(0xFF8B5CF6),
                onClick = onNavigateToProgress,
                modifier = Modifier.weight(1f).testTag("quick_view_progress")
            )

            QuickActionButton(
                label = "Set Goals",
                icon = Icons.Default.TrackChanges,
                containerColor = Color(0xFFF59E0B),
                onClick = onNavigateToGoals,
                modifier = Modifier.weight(1f).testTag("quick_set_goals")
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    target: String,
    progress: Float,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = target,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }
    }
}
