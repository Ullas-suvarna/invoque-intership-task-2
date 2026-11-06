package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FitnessRepository(
    private val db: FirebaseFirestore,
    private val rtdb: FirebaseDatabase
) {

    constructor(context: Context) : this(
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Failed to get Firestore instance", e)
            FirebaseFirestore.getInstance()
        },
        try {
            FirebaseDatabase.getInstance("https://health-fitness-app-8ecb7-default-rtdb.asia-southeast1.firebasedatabase.app")
        } catch (e: Exception) {
            Log.w("FitnessRepository", "Fallback to default database instance", e)
            Firebase.database
        }
    )

    private val auth = Firebase.auth
    val currentUserId: String?
        get() = try { auth.currentUser?.uid } catch (e: Exception) { null }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // ----------------------------------------------------
    // DAILY ACTIVITY & STEP TRACKING
    // ----------------------------------------------------

    fun observeTodayActivity(userId: String): Flow<ActivityRecord?> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val today = getCurrentDateString()
        val docId = "${userId}_$today"

        val listener = try {
            db.collection("activities").document(docId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FitnessRepository", "Error observing today activity", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    try {
                        if (snapshot != null && snapshot.exists()) {
                            val record = snapshot.toObject(ActivityRecord::class.java)?.copy(recordId = snapshot.id)
                            trySend(record)
                        } else {
                            trySend(null)
                        }
                    } catch (e: Exception) {
                        Log.e("FitnessRepository", "Error parsing today activity", e)
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Exception attaching today activity listener", e)
            trySend(null)
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }

    suspend fun updateTodaySteps(userId: String, steps: Int, isSensor: Boolean = false): Result<Unit> {
        if (userId.isEmpty()) return Result.failure(Exception("Not logged in"))
        val today = getCurrentDateString()
        val docId = "${userId}_$today"
        val docRef = db.collection("activities").document(docId)

        return try {
            val snapshot = docRef.get().await()
            val distanceKm = (steps * 0.00075) // avg step ~0.75 meter
            val calories = (steps * 0.04).toInt()
            val activeMin = (steps / 100)

            val payload = mapOf(
                "userId" to userId,
                "date" to today,
                "steps" to steps,
                "distance" to Math.round(distanceKm * 100.0) / 100.0,
                "caloriesBurned" to calories,
                "activeMinutes" to activeMin,
                "source" to (if (isSensor) "Sensor" else "Manual"),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!snapshot.exists()) {
                val fullPayload = payload + mapOf("createdAt" to FieldValue.serverTimestamp())
                docRef.set(fullPayload).await()
            } else {
                docRef.update(payload).await()
            }

            // Sync to Realtime Database live activity safely
            try {
                val rtdbRef = rtdb.getReference("users/$userId/liveActivity")
                rtdbRef.setValue(
                    mapOf(
                        "steps" to steps,
                        "caloriesBurned" to calories,
                        "activeMinutes" to activeMin,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w("FitnessRepository", "RTDB live activity sync failed", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Failed to update today steps", e)
            Result.failure(e)
        }
    }

    fun observeActivityHistory(userId: String): Flow<List<ActivityRecord>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = try {
            db.collection("activities")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FitnessRepository", "Error loading activity history", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    try {
                        val items = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(ActivityRecord::class.java)?.copy(recordId = doc.id)
                        }?.sortedByDescending { it.date } ?: emptyList()
                        trySend(items)
                    } catch (e: Exception) {
                        Log.e("FitnessRepository", "Error parsing activity history", e)
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Exception attaching activity history listener", e)
            trySend(emptyList())
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }

    // ----------------------------------------------------
    // WORKOUT MANAGEMENT
    // ----------------------------------------------------

    fun observeWorkouts(userId: String): Flow<List<WorkoutRecord>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = try {
            db.collection("workouts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FitnessRepository", "Error observing workouts", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    try {
                        val workouts = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(WorkoutRecord::class.java)?.copy(workoutId = doc.id)
                        }?.sortedByDescending { it.createdAt?.seconds ?: 0L } ?: emptyList()
                        trySend(workouts)
                    } catch (e: Exception) {
                        Log.e("FitnessRepository", "Error parsing workouts", e)
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Exception attaching workouts listener", e)
            trySend(emptyList())
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }

    suspend fun saveWorkout(workout: WorkoutRecord): Result<String> {
        val uid = workout.userId.ifEmpty { currentUserId ?: return Result.failure(Exception("Not logged in")) }
        val docRef = if (workout.workoutId.isNotEmpty()) {
            db.collection("workouts").document(workout.workoutId)
        } else {
            db.collection("workouts").document()
        }

        return try {
            val payload = mapOf(
                "workoutId" to docRef.id,
                "userId" to uid,
                "workoutName" to workout.workoutName,
                "category" to workout.category,
                "duration" to workout.duration,
                "caloriesBurned" to workout.caloriesBurned,
                "intensity" to workout.intensity,
                "date" to workout.date.ifEmpty { getCurrentDateString() },
                "startTime" to workout.startTime,
                "endTime" to workout.endTime,
                "notes" to workout.notes,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Update daily activity record with additional workout calories & active minutes
            try {
                val today = getCurrentDateString()
                val activityDocId = "${uid}_$today"
                val actRef = db.collection("activities").document(activityDocId)
                val actSnap = actRef.get().await()

                val workoutMin = workout.duration / 60
                if (actSnap.exists()) {
                    val currentCals = actSnap.getLong("caloriesBurned")?.toInt() ?: 0
                    val currentMin = actSnap.getLong("activeMinutes")?.toInt() ?: 0
                    actRef.update(
                        mapOf(
                            "caloriesBurned" to (currentCals + workout.caloriesBurned),
                            "activeMinutes" to (currentMin + workoutMin),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    actRef.set(
                        mapOf(
                            "userId" to uid,
                            "date" to today,
                            "steps" to 0,
                            "distance" to 0.0,
                            "caloriesBurned" to workout.caloriesBurned,
                            "activeMinutes" to workoutMin,
                            "source" to "Workout",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("FitnessRepository", "Activity summary update failed", e)
            }

            // Clear RTDB active workout session safely
            try {
                rtdb.getReference("users/$uid/activeWorkout").removeValue()
            } catch (e: Exception) {
                Log.w("FitnessRepository", "Clear RTDB active workout failed", e)
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Save workout failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            db.collection("workouts").document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Delete workout failed", e)
            Result.failure(e)
        }
    }

    // Realtime Database Active Workout Session
    fun observeActiveWorkoutSession(userId: String): Flow<ActiveWorkoutSession?> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        val session = snapshot.getValue(ActiveWorkoutSession::class.java)
                        trySend(session)
                    } else {
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e("FitnessRepository", "Error parsing active workout from RTDB", e)
                    trySend(null)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FitnessRepository", "RTDB active workout cancelled", error.toException())
                trySend(null)
            }
        }
        val ref = try {
            val rRef = rtdb.getReference("users/$userId/activeWorkout")
            rRef.addValueEventListener(listener)
            rRef
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Failed to add RTDB listener", e)
            trySend(null)
            null
        }

        awaitClose {
            try { ref?.removeEventListener(listener) } catch (e: Exception) {}
        }
    }

    suspend fun updateActiveWorkoutSession(userId: String, session: ActiveWorkoutSession?): Result<Unit> {
        if (userId.isEmpty()) return Result.failure(Exception("Not logged in"))
        return try {
            val ref = rtdb.getReference("users/$userId/activeWorkout")
            if (session == null) {
                ref.removeValue().await()
            } else {
                ref.setValue(session).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Failed to update RTDB active workout", e)
            Result.failure(e)
        }
    }

    fun calculateCaloriesBurned(category: String, durationSeconds: Int, userWeightKg: Double = 70.0): Int {
        val met = when (category.lowercase(Locale.ROOT)) {
            "running" -> 9.8
            "walking" -> 3.8
            "cycling" -> 7.5
            "swimming" -> 8.0
            "yoga" -> 3.0
            "gym workout" -> 6.0
            "strength training" -> 5.0
            "cardio" -> 8.5
            "meditation" -> 1.5
            else -> 4.5
        }
        val durationMinutes = durationSeconds / 60.0
        val calories = (met * 3.5 * userWeightKg / 200.0) * durationMinutes
        return Math.max(1, calories.toInt())
    }

    // ----------------------------------------------------
    // FITNESS GOALS
    // ----------------------------------------------------

    fun observeGoals(userId: String): Flow<List<FitnessGoal>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = try {
            db.collection("goals")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FitnessRepository", "Error observing goals", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    try {
                        val goals = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(FitnessGoal::class.java)?.copy(goalId = doc.id)
                        } ?: emptyList()
                        trySend(goals)
                    } catch (e: Exception) {
                        Log.e("FitnessRepository", "Error parsing goals", e)
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Exception attaching goals listener", e)
            trySend(emptyList())
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }

    suspend fun saveGoal(goal: FitnessGoal): Result<Unit> {
        val uid = goal.userId.ifEmpty { currentUserId ?: return Result.failure(Exception("Not logged in")) }
        val docRef = if (goal.goalId.isNotEmpty()) {
            db.collection("goals").document(goal.goalId)
        } else {
            db.collection("goals").document()
        }

        return try {
            val payload = mapOf(
                "goalId" to docRef.id,
                "userId" to uid,
                "goalType" to goal.goalType,
                "targetValue" to goal.targetValue,
                "currentValue" to goal.currentValue,
                "period" to goal.period,
                "startDate" to goal.startDate.ifEmpty { getCurrentDateString() },
                "endDate" to goal.endDate,
                "isCompleted" to goal.isCompleted,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Save goal failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> {
        return try {
            db.collection("goals").document(goalId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Delete goal failed", e)
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // WORKOUT PLANS
    // ----------------------------------------------------

    fun observeWorkoutPlans(userId: String): Flow<List<WorkoutPlan>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(getDefaultPlans())
            close()
            return@callbackFlow
        }
        val listener = try {
            db.collection("workout_plans")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FitnessRepository", "Error observing workout plans", error)
                        trySend(getDefaultPlans())
                        return@addSnapshotListener
                    }
                    try {
                        val userPlans = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(WorkoutPlan::class.java)?.copy(planId = doc.id)
                        } ?: emptyList()

                        val combined = getDefaultPlans() + userPlans
                        trySend(combined)
                    } catch (e: Exception) {
                        Log.e("FitnessRepository", "Error parsing workout plans", e)
                        trySend(getDefaultPlans())
                    }
                }
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Exception attaching workout plans listener", e)
            trySend(getDefaultPlans())
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }

    suspend fun saveWorkoutPlan(plan: WorkoutPlan): Result<Unit> {
        val uid = plan.userId.ifEmpty { currentUserId ?: return Result.failure(Exception("Not logged in")) }
        val docRef = if (plan.planId.isNotEmpty()) {
            db.collection("workout_plans").document(plan.planId)
        } else {
            db.collection("workout_plans").document()
        }

        return try {
            val payload = mapOf(
                "planId" to docRef.id,
                "userId" to uid,
                "planName" to plan.planName,
                "description" to plan.description,
                "difficulty" to plan.difficulty,
                "exercises" to plan.exercises,
                "durationWeeks" to plan.durationWeeks,
                "isCustom" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Save workout plan failed", e)
            Result.failure(e)
        }
    }

    fun getDefaultPlans(): List<WorkoutPlan> {
        return listOf(
            WorkoutPlan(
                planId = "default_beginner",
                planName = "Beginner Fitness Plan",
                description = "Build a solid foundation with light cardio, walking, and bodyweight exercises.",
                difficulty = "Beginner",
                exercises = listOf("15 min Brisk Walk", "10 min Light Stretching", "10 Bodyweight Squats", "5 min Breathing Meditation"),
                durationWeeks = 4
            ),
            WorkoutPlan(
                planId = "default_weight_loss",
                planName = "Weight Loss Activity Plan",
                description = "High-energy fat burning plan combining running intervals, cycling, and HIIT routines.",
                difficulty = "Intermediate",
                exercises = listOf("20 min Interval Run", "15 min Indoor Cycling", "12 Jumping Jacks", "10 Burpees", "10 min Cool Down Walk"),
                durationWeeks = 6
            ),
            WorkoutPlan(
                planId = "default_strength",
                planName = "Strength Training Plan",
                description = "Progressive resistance training targeting core, upper body, and lower body strength.",
                difficulty = "Intermediate",
                exercises = listOf("3x12 Dumbbell Bench Press", "3x10 Barbell Squats", "3x8 Push Ups", "3x12 Bent Over Rows", "1 min Plank"),
                durationWeeks = 8
            ),
            WorkoutPlan(
                planId = "default_cardio",
                planName = "Cardio Training Plan",
                description = "Boost stamina, lung capacity, and cardiovascular health with targeted aerobic Workouts.",
                difficulty = "Advanced",
                exercises = listOf("30 min Outdoor Running", "15 min Jump Rope", "15 min Rowing Machine", "10 min Stretching"),
                durationWeeks = 6
            ),
            WorkoutPlan(
                planId = "default_yoga",
                planName = "Yoga and Flexibility Plan",
                description = "Enhance mobility, joint health, core posture, and mental relaxation.",
                difficulty = "Beginner",
                exercises = listOf("Sun Salutations", "Warrior Poses", "Tree Balance Pose", "Child's Pose", "10 min Meditation"),
                durationWeeks = 4
            ),
            WorkoutPlan(
                planId = "default_general",
                planName = "General Fitness Plan",
                description = "Balanced daily routine maintaining energy, mobility, and healthy active minutes.",
                difficulty = "Beginner",
                exercises = listOf("10,000 Steps Daily Target", "20 min Evening Jog", "15 min Core Workout"),
                durationWeeks = 12
            )
        )
    }

    // ----------------------------------------------------
    // REMINDERS & NOTIFICATIONS
    // ----------------------------------------------------

    fun observeReminders(userId: String): Flow<List<ReminderItem>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(getDefaultReminders())
            close()
            return@callbackFlow
        }
        val listener = try {
            db.collection("reminders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FitnessRepository", "Error observing reminders", error)
                        trySend(getDefaultReminders())
                        return@addSnapshotListener
                    }
                    try {
                        val items = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(ReminderItem::class.java)?.copy(reminderId = doc.id)
                        } ?: emptyList()

                        if (items.isEmpty()) {
                            trySend(getDefaultReminders())
                        } else {
                            trySend(items)
                        }
                    } catch (e: Exception) {
                        Log.e("FitnessRepository", "Error parsing reminders", e)
                        trySend(getDefaultReminders())
                    }
                }
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Exception attaching reminders listener", e)
            trySend(getDefaultReminders())
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }

    suspend fun saveReminder(reminder: ReminderItem): Result<Unit> {
        val uid = reminder.userId.ifEmpty { currentUserId ?: return Result.failure(Exception("Not logged in")) }
        val docRef = if (reminder.reminderId.isNotEmpty() && !reminder.reminderId.startsWith("def_")) {
            db.collection("reminders").document(reminder.reminderId)
        } else {
            db.collection("reminders").document()
        }

        return try {
            val payload = mapOf(
                "reminderId" to docRef.id,
                "userId" to uid,
                "reminderType" to reminder.reminderType,
                "reminderTime" to reminder.reminderTime,
                "isEnabled" to reminder.isEnabled,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Save reminder failed", e)
            Result.failure(e)
        }
    }

    fun getDefaultReminders(): List<ReminderItem> {
        return listOf(
            ReminderItem("def_1", "", "Daily Workout", "07:30", true),
            ReminderItem("def_2", "", "Step Goal", "18:00", true),
            ReminderItem("def_3", "", "Water Reminder", "11:00", true),
            ReminderItem("def_4", "", "Weekly Progress", "20:00", true)
        )
    }

    // ----------------------------------------------------
    // WEIGHT HISTORY
    // ----------------------------------------------------

    fun observeWeightHistory(userId: String): Flow<List<WeightRecord>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = try {
            db.collection("weight_history")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FitnessRepository", "Error observing weight history", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    try {
                        val records = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(WeightRecord::class.java)?.copy(recordId = doc.id)
                        }?.sortedBy { it.date } ?: emptyList()
                        trySend(records)
                    } catch (e: Exception) {
                        Log.e("FitnessRepository", "Error parsing weight history", e)
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Exception attaching weight history listener", e)
            trySend(emptyList())
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }

    suspend fun addWeightRecord(userId: String, weightKg: Double, dateStr: String = getCurrentDateString()): Result<Unit> {
        if (userId.isEmpty()) return Result.failure(Exception("Not logged in"))
        val docRef = db.collection("weight_history").document()
        return try {
            val payload = mapOf(
                "recordId" to docRef.id,
                "userId" to userId,
                "weight" to weightKg,
                "date" to dateStr,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            try {
                db.collection("users").document(userId)
                    .update("weight", weightKg, "updatedAt", FieldValue.serverTimestamp()).await()
            } catch (e: Exception) {
                Log.w("FitnessRepository", "Update user profile weight failed", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FitnessRepository", "Add weight record failed", e)
            Result.failure(e)
        }
    }
}
