package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutPlan
import com.example.data.repository.FitnessRepository

@Composable
fun WorkoutPlansScreen(
    plans: List<WorkoutPlan>,
    fitnessRepository: FitnessRepository,
    onSavePlan: (WorkoutPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlanForDetail by remember { mutableStateOf<WorkoutPlan?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

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
                    text = "Workout Plans",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Structured routines tailored to your fitness goals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                modifier = Modifier.testTag("create_plan_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Custom Plan")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plans) { plan ->
                WorkoutPlanCard(
                    plan = plan,
                    onClick = { selectedPlanForDetail = plan }
                )
            }
        }
    }

    // Detail Dialog
    selectedPlanForDetail?.let { plan ->
        AlertDialog(
            onDismissRequest = { selectedPlanForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.planName, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Surface(
                        color = when (plan.difficulty.lowercase()) {
                            "beginner" -> Color(0xFF10B981).copy(alpha = 0.2f)
                            "intermediate" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Difficulty: ${plan.difficulty} • ${plan.durationWeeks} Weeks",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(plan.description, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Recommended Exercises:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))

                    plan.exercises.forEach { exercise ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(exercise, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedPlanForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Select & Track Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlanForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Create Custom Plan Dialog
    if (showCreateDialog) {
        var nameInput by remember { mutableStateOf("") }
        var descInput by remember { mutableStateOf("") }
        var difficultyInput by remember { mutableStateOf("Beginner") }
        var exercisesInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Custom Workout Plan") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Plan Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = exercisesInput,
                        onValueChange = { exercisesInput = it },
                        label = { Text("Exercises (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = fitnessRepository.currentUserId ?: ""
                        val exList = exercisesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val plan = WorkoutPlan(
                            userId = uid,
                            planName = nameInput.ifBlank { "Custom Plan" },
                            description = descInput.ifBlank { "User created custom fitness plan" },
                            difficulty = difficultyInput,
                            exercises = if (exList.isEmpty()) listOf("20 min Walk", "10 min Stretching") else exList,
                            durationWeeks = 4,
                            isCustom = true
                        )
                        onSavePlan(plan)
                        showCreateDialog = false
                    }
                ) {
                    Text("Save Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkoutPlanCard(
    plan: WorkoutPlan,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.planName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = when (plan.difficulty.lowercase()) {
                        "beginner" -> Color(0xFF10B981).copy(alpha = 0.15f)
                        "intermediate" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = plan.difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (plan.difficulty.lowercase()) {
                            "beginner" -> Color(0xFF10B981)
                            "intermediate" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Includes ${plan.exercises.size} Exercises • ${plan.durationWeeks} Weeks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
