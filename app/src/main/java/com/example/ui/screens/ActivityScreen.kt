package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityRecord
import com.example.sensor.StepTrackerManager

@Composable
fun ActivityScreen(
    todayActivity: ActivityRecord?,
    activityHistory: List<ActivityRecord>,
    stepTrackerManager: StepTrackerManager,
    onUpdateSteps: (steps: Int, isSensor: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveSensorSteps by stepTrackerManager.stepCount.collectAsState()
    val isSensorAvailable by stepTrackerManager.isSensorAvailable.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var manualStepsInput by remember { mutableStateOf("") }
    var stepGoal by remember { mutableStateOf(10000) }

    val currentSteps = if (isSensorAvailable && liveSensorSteps > 0) liveSensorSteps else (todayActivity?.steps ?: 0)
    val distanceKm = String.format("%.2f", currentSteps * 0.00075)
    val caloriesEstimated = (currentSteps * 0.04).toInt()
    val activeMinutes = (currentSteps / 100)
    val progress = (currentSteps.toFloat() / stepGoal).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Step & Activity Tracker",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isSensorAvailable) "🟢 Hardware Sensor Active" else "🟠 Manual & Simulated Step Mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSensorAvailable) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }

            IconButton(onClick = { showGoalDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Target Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Step Counter Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = String.format("%,d", currentSteps),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Daily Step Target: ${String.format("%,d", stepGoal)} steps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF10B981).copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ActivityMetricItem(label = "Distance", value = "$distanceKm km", icon = Icons.Default.Place)
                    ActivityMetricItem(label = "Calories", value = "$caloriesEstimated kcal", icon = Icons.Default.LocalFireDepartment)
                    ActivityMetricItem(label = "Active Time", value = "$activeMinutes min", icon = Icons.Default.Timer)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_entry_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Manually")
                    }

                    Button(
                        onClick = {
                            stepTrackerManager.addSimulatedSteps(500)
                            val newSteps = currentSteps + 500
                            onUpdateSteps(newSteps, false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_500_steps_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+500 Steps")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History Section
        Text(
            text = "Activity History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (activityHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No historical activity records yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activityHistory) { record ->
                    ActivityHistoryCard(record = record)
                }
            }
        }
    }

    // Manual Entry Dialog
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Manual Step Entry") },
            text = {
                Column {
                    Text(
                        text = "Enter your recorded step count. Values logged here will be marked as 'Manually Entered'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualStepsInput,
                        onValueChange = { manualStepsInput = it },
                        label = { Text("Total Steps") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_steps_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val steps = manualStepsInput.toIntOrNull() ?: 0
                        stepTrackerManager.setStepsManually(steps)
                        onUpdateSteps(steps, false)
                        showManualDialog = false
                    }
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Step Goal Dialog
    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf(stepGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Adjust Daily Step Target") },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("Daily Target Steps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        goalInput.toIntOrNull()?.let { stepGoal = it }
                        showGoalDialog = false
                    }
                ) {
                    Text("Update Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ActivityMetricItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActivityHistoryCard(record: ActivityRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Source: ${record.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (record.source == "Manual") Color(0xFFF59E0B) else Color(0xFF10B981)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%,d", record.steps)} steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10B981)
                )
                Text(
                    text = "${record.caloriesBurned} kcal • ${record.distance} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
