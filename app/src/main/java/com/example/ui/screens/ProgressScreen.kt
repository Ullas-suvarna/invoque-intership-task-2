package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityRecord
import com.example.data.model.WorkoutRecord
import com.example.ui.components.ChartDataPoint
import com.example.ui.components.FitBarChart
import com.example.ui.components.FitLineChart

@Composable
fun ProgressScreen(
    activities: List<ActivityRecord>,
    workouts: List<WorkoutRecord>,
    modifier: Modifier = Modifier
) {
    var selectedPeriodTab by remember { mutableStateOf(1) } // 0: Daily, 1: Weekly, 2: Monthly
    val scrollState = rememberScrollState()

    // Process data for charts
    val stepChartData = remember(activities) {
        if (activities.isEmpty()) {
            listOf(
                ChartDataPoint("Mon", 0f), ChartDataPoint("Tue", 0f),
                ChartDataPoint("Wed", 0f), ChartDataPoint("Thu", 0f),
                ChartDataPoint("Fri", 0f), ChartDataPoint("Sat", 0f), ChartDataPoint("Sun", 0f)
            )
        } else {
            activities.take(7).reversed().map {
                ChartDataPoint(
                    label = it.date.takeLast(5),
                    value = it.steps.toFloat()
                )
            }
        }
    }

    val caloriesChartData = remember(activities, workouts) {
        if (activities.isEmpty() && workouts.isEmpty()) {
            listOf(
                ChartDataPoint("Mon", 0f), ChartDataPoint("Tue", 0f),
                ChartDataPoint("Wed", 0f), ChartDataPoint("Thu", 0f),
                ChartDataPoint("Fri", 0f), ChartDataPoint("Sat", 0f), ChartDataPoint("Sun", 0f)
            )
        } else {
            activities.take(7).reversed().map {
                ChartDataPoint(
                    label = it.date.takeLast(5),
                    value = it.caloriesBurned.toFloat()
                )
            }
        }
    }

    val workoutDurationChartData = remember(workouts) {
        if (workouts.isEmpty()) {
            listOf(
                ChartDataPoint("Session 1", 0f),
                ChartDataPoint("Session 2", 0f),
                ChartDataPoint("Session 3", 0f),
                ChartDataPoint("Session 4", 0f)
            )
        } else {
            workouts.take(6).reversed().mapIndexed { index, w ->
                ChartDataPoint(
                    label = w.workoutName.take(6),
                    value = (w.duration / 60).toFloat()
                )
            }
        }
    }

    // Calculated Analytics Summary
    val totalSteps = activities.sumOf { it.steps }
    val avgSteps = if (activities.isNotEmpty()) totalSteps / activities.size else 0
    val totalCalories = activities.sumOf { it.caloriesBurned } + workouts.sumOf { it.caloriesBurned }
    val totalWorkoutSeconds = workouts.sumOf { it.duration }
    val totalWorkoutHours = String.format("%.1f", totalWorkoutSeconds / 3600.0)

    val mostFrequentCategory = remember(workouts) {
        if (workouts.isEmpty()) "Running"
        else workouts.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "Running"
    }

    // Dynamic Fitness Insights
    val insightsList = remember(activities, workouts) {
        val list = mutableListOf<String>()
        if (workouts.isNotEmpty()) {
            list.add("🔥 You have logged ${workouts.size} workout sessions so far!")
            list.add("⭐ Most frequent activity: $mostFrequentCategory")
        }
        if (activities.isNotEmpty()) {
            list.add("👣 Average daily step count is ${String.format("%,d", avgSteps)} steps.")
        }
        if (list.isEmpty()) {
            list.add("💡 Start logging your daily steps and workouts to unlock personalized fitness insights!")
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Progress & Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track your fitness metrics, trends, and milestone insights",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Time Period Selector
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            TabRow(
                selectedTabIndex = selectedPeriodTab,
                containerColor = Color.Transparent
            ) {
                Tab(selected = selectedPeriodTab == 0, onClick = { selectedPeriodTab = 0 }, text = { Text("Daily") })
                Tab(selected = selectedPeriodTab == 1, onClick = { selectedPeriodTab = 1 }, text = { Text("Weekly") })
                Tab(selected = selectedPeriodTab == 2, onClick = { selectedPeriodTab = 2 }, text = { Text("Monthly") })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analytics Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressSummaryCard(title = "Total Steps", value = String.format("%,d", totalSteps), unit = "steps", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
            ProgressSummaryCard(title = "Avg Daily", value = String.format("%,d", avgSteps), unit = "steps/day", color = Color(0xFF0EA5E9), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressSummaryCard(title = "Total Calories", value = "$totalCalories", unit = "kcal", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
            ProgressSummaryCard(title = "Workout Time", value = totalWorkoutHours, unit = "hours", color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Fitness Insights Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Smart Fitness Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                insightsList.forEach { insight ->
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Charts Section
        FitBarChart(
            title = "Daily Steps Chart",
            subtitle = "Step count history over recent days",
            data = stepChartData,
            primaryColor = Color(0xFF10B981),
            accentColor = Color(0xFF0EA5E9),
            unit = "steps"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FitBarChart(
            title = "Weekly Calories Burned",
            subtitle = "Estimated energy expenditure",
            data = caloriesChartData,
            primaryColor = Color(0xFFEF4444),
            accentColor = Color(0xFFF59E0B),
            unit = "kcal"
        )

        Spacer(modifier = Modifier.height(16.dp))

        FitLineChart(
            title = "Workout Duration Trend",
            subtitle = "Active exercise minutes per session",
            data = workoutDurationChartData,
            lineColor = Color(0xFF0EA5E9),
            unit = "min"
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ProgressSummaryCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}
