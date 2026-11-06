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
import com.example.data.model.FitnessGoal
import com.example.data.repository.FitnessRepository

@Composable
fun GoalsScreen(
    goals: List<FitnessGoal>,
    fitnessRepository: FitnessRepository,
    onSaveGoal: (FitnessGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<FitnessGoal?>(null) }

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
                    text = "Fitness Goals",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Set daily & weekly targets to stay motivated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.testTag("create_goal_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Goal")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No custom fitness goals created yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap 'New Goal' to establish your daily step or calorie targets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals) { goal ->
                    GoalCardItem(
                        goal = goal,
                        onEdit = { editingGoal = goal },
                        onDelete = { onDeleteGoal(goal.goalId) }
                    )
                }
            }
        }
    }

    // Create / Edit Goal Dialog
    if (showCreateDialog || editingGoal != null) {
        val targetGoal = editingGoal
        val goalTypes = listOf("Daily Steps", "Daily Calories", "Daily Active Minutes", "Weekly Workouts", "Weekly Duration")
        var selectedType by remember { mutableStateOf(targetGoal?.goalType ?: goalTypes[0]) }
        var targetValueInput by remember { mutableStateOf(targetGoal?.targetValue?.toInt()?.toString() ?: "10000") }
        var periodInput by remember { mutableStateOf(targetGoal?.period ?: "Daily") }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingGoal = null
            },
            title = { Text(if (targetGoal != null) "Edit Fitness Goal" else "Create New Fitness Goal") },
            text = {
                Column {
                    Text("Goal Type", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    ScrollableTabRow(
                        selectedTabIndex = goalTypes.indexOf(selectedType).coerceAtLeast(0),
                        edgePadding = 0.dp
                    ) {
                        goalTypes.forEach { type ->
                            Tab(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    periodInput = if (type.startsWith("Weekly")) "Weekly" else "Daily"
                                },
                                text = { Text(type, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = targetValueInput,
                        onValueChange = { targetValueInput = it },
                        label = { Text("Target Value") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_target_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetVal = targetValueInput.toDoubleOrNull() ?: 10000.0
                        val uid = fitnessRepository.currentUserId ?: ""

                        val goal = FitnessGoal(
                            goalId = targetGoal?.goalId ?: "",
                            userId = uid,
                            goalType = selectedType,
                            targetValue = targetVal,
                            currentValue = targetGoal?.currentValue ?: 0.0,
                            period = periodInput,
                            isCompleted = (targetGoal?.currentValue ?: 0.0) >= targetVal
                        )
                        onSaveGoal(goal)
                        showCreateDialog = false
                        editingGoal = null
                    }
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        editingGoal = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GoalCardItem(
    goal: FitnessGoal,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0).toFloat()
    val isCompleted = progress >= 1.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0EA5E9).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.EmojiEvents else Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (isCompleted) Color(0xFF10B981) else Color(0xFF0EA5E9)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.goalType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Period: ${goal.period}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress: ${String.format("%,d", goal.currentValue.toInt())} / ${String.format("%,d", goal.targetValue.toInt())}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) Color(0xFF10B981) else Color(0xFF0EA5E9)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) Color(0xFF10B981) else Color(0xFF0EA5E9),
                trackColor = Color(0xFF0EA5E9).copy(alpha = 0.15f)
            )

            if (isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🎉 Goal Completed! Excellent dedication!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
