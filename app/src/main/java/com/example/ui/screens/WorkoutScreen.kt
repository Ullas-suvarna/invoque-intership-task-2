package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveWorkoutSession
import com.example.data.model.WorkoutRecord
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CategoryItem(val name: String, val icon: ImageVector, val color: Color)

@Composable
fun WorkoutScreen(
    workouts: List<WorkoutRecord>,
    activeSession: ActiveWorkoutSession?,
    fitnessRepository: FitnessRepository,
    onSaveWorkout: (WorkoutRecord) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onUpdateActiveSession: (ActiveWorkoutSession?) -> Unit,
    userWeightKg: Double = 70.0,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            CategoryItem("Walking", Icons.Default.DirectionsWalk, Color(0xFF10B981)),
            CategoryItem("Running", Icons.Default.DirectionsRun, Color(0xFFEF4444)),
            CategoryItem("Cycling", Icons.Default.DirectionsBike, Color(0xFF0EA5E9)),
            CategoryItem("Swimming", Icons.Default.Pool, Color(0xFF3B82F6)),
            CategoryItem("Yoga", Icons.Default.SelfImprovement, Color(0xFF8B5CF6)),
            CategoryItem("Gym Workout", Icons.Default.FitnessCenter, Color(0xFFF59E0B)),
            CategoryItem("Strength Training", Icons.Default.SportsGymnastics, Color(0xFFEC4899)),
            CategoryItem("Cardio", Icons.Default.MonitorHeart, Color(0xFF10B981)),
            CategoryItem("Meditation", Icons.Default.Spa, Color(0xFF6366F1)),
            CategoryItem("Other Activities", Icons.Default.MoreHoriz, Color(0xFF6B7280))
        )
    }

    var selectedCategory by remember { mutableStateOf(categories[1]) } // Running
    var customWorkoutName by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("Moderate") }
    var notes by remember { mutableStateOf("") }

    // Live Workout State
    var isTimerRunning by remember { mutableStateOf(activeSession != null && !activeSession.isPaused) }
    var isPaused by remember { mutableStateOf(activeSession?.isPaused ?: false) }
    var elapsedSeconds by remember { mutableStateOf(activeSession?.elapsedSeconds ?: 0) }
    var startTimeMillis by remember { mutableStateOf(activeSession?.startTime ?: System.currentTimeMillis()) }

    var showLogManualDialog by remember { mutableStateOf(false) }
    var editingWorkout by remember { mutableStateOf<WorkoutRecord?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Timer Effect
    LaunchedEffect(isTimerRunning, isPaused) {
        while (isTimerRunning && !isPaused) {
            delay(1000L)
            elapsedSeconds += 1
            // Sync active workout state to RTDB
            val uid = fitnessRepository.currentUserId ?: ""
            if (uid.isNotEmpty()) {
                val session = ActiveWorkoutSession(
                    workoutId = UUID.randomUUID().toString(),
                    workoutName = customWorkoutName.ifBlank { selectedCategory.name },
                    category = selectedCategory.name,
                    startTime = startTimeMillis,
                    elapsedSeconds = elapsedSeconds,
                    isPaused = isPaused,
                    lastUpdated = System.currentTimeMillis()
                )
                fitnessRepository.updateActiveWorkoutSession(uid, session)
            }
        }
    }

    val estimatedCalories = remember(selectedCategory, elapsedSeconds, userWeightKg) {
        fitnessRepository.calculateCaloriesBurned(selectedCategory.name, elapsedSeconds, userWeightKg)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workout Manager",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = { showLogManualDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                modifier = Modifier.testTag("log_manual_workout_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Past")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Workout Timer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isTimerRunning) Color(0xFF0F172A) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTimerRunning) "🔴 Live Workout Session" else "Start New Workout Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isTimerRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                    if (isTimerRunning) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Synced to RTDB",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer Display (HH:MM:SS)
                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isTimerRunning) Color.White else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${selectedCategory.name} • ~$estimatedCalories kcal burned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isTimerRunning) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Timer Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!isTimerRunning) {
                        Button(
                            onClick = {
                                isTimerRunning = true
                                isPaused = false
                                startTimeMillis = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_workout_timer_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Timer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                isPaused = !isPaused
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pause_resume_timer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPaused) "Resume" else "Pause")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                // Finish workout and save to Firestore
                                val uid = fitnessRepository.currentUserId ?: ""
                                val sdf = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                                val record = WorkoutRecord(
                                    userId = uid,
                                    workoutName = customWorkoutName.ifBlank { selectedCategory.name },
                                    category = selectedCategory.name,
                                    duration = elapsedSeconds,
                                    caloriesBurned = estimatedCalories,
                                    intensity = intensity,
                                    date = sdf.format(Date(startTimeMillis)),
                                    startTime = timeFormat.format(Date(startTimeMillis)),
                                    endTime = timeFormat.format(Date()),
                                    notes = notes
                                )
                                onSaveWorkout(record)

                                // Reset Timer State
                                isTimerRunning = false
                                isPaused = false
                                elapsedSeconds = 0
                                onUpdateActiveSession(null)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("finish_workout_timer_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finish & Save")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Predefined Exercise Category Selection
        Text(
            text = "Select Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat.name == selectedCategory.name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) cat.color.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectedCategory = cat }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) cat.color else cat.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else cat.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workout History List
        Text(
            text = "Previous Workouts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (workouts.isEmpty()) {
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
                    text = "No recorded workouts yet. Start a timer or log one!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workouts) { record ->
                    WorkoutItemCard(
                        record = record,
                        onEdit = { editingWorkout = record },
                        onDelete = { onDeleteWorkout(record.workoutId) }
                    )
                }
            }
        }
    }

    // Log Past Workout / Edit Dialog
    if (showLogManualDialog || editingWorkout != null) {
        val targetRecord = editingWorkout
        var nameInput by remember { mutableStateOf(targetRecord?.workoutName ?: selectedCategory.name) }
        var categoryInput by remember { mutableStateOf(targetRecord?.category ?: selectedCategory.name) }
        var durationMinInput by remember { mutableStateOf(if (targetRecord != null) (targetRecord.duration / 60).toString() else "30") }
        var caloriesInput by remember { mutableStateOf(if (targetRecord != null) targetRecord.caloriesBurned.toString() else "200") }
        var notesInput by remember { mutableStateOf(targetRecord?.notes ?: "") }

        AlertDialog(
            onDismissRequest = {
                showLogManualDialog = false
                editingWorkout = null
            },
            title = { Text(if (targetRecord != null) "Edit Workout Record" else "Log Workout Details") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Workout Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_workout_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = durationMinInput,
                            onValueChange = { durationMinInput = it },
                            label = { Text("Duration (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = caloriesInput,
                            onValueChange = { caloriesInput = it },
                            label = { Text("Calories (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val durationSec = (durationMinInput.toIntOrNull() ?: 30) * 60
                        val cals = caloriesInput.toIntOrNull() ?: 200
                        val uid = fitnessRepository.currentUserId ?: ""
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        val record = WorkoutRecord(
                            workoutId = targetRecord?.workoutId ?: "",
                            userId = uid,
                            workoutName = nameInput.ifBlank { categoryInput },
                            category = categoryInput,
                            duration = durationSec,
                            caloriesBurned = cals,
                            intensity = "Moderate",
                            date = targetRecord?.date ?: sdf.format(Date()),
                            notes = notesInput
                        )
                        onSaveWorkout(record)
                        showLogManualDialog = false
                        editingWorkout = null
                    }
                ) {
                    Text("Save Workout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogManualDialog = false
                        editingWorkout = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkoutItemCard(
    record: WorkoutRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.workoutName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${record.category} • ${record.duration / 60} min • ${record.caloriesBurned} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}
