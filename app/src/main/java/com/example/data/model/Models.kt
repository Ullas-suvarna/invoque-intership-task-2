package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val photoURL: String? = null,
    val age: Int = 25,
    val height: Double = 175.0, // in cm
    val weight: Double = 70.0,  // in kg
    val gender: String? = "Not Specified",
    val fitnessGoal: String = "General Fitness",
    val preferredActivity: String = "Running",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityRecord(
    val recordId: String = "",
    val userId: String = "",
    val date: String = "", // format YYYY-MM-DD
    val steps: Int = 0,
    val distance: Double = 0.0, // in km
    val caloriesBurned: Int = 0,
    val activeMinutes: Int = 0,
    val source: String = "Sensor", // "Sensor" or "Manual"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkoutRecord(
    val workoutId: String = "",
    val userId: String = "",
    val workoutName: String = "",
    val category: String = "Running",
    val duration: Int = 0, // in seconds
    val caloriesBurned: Int = 0,
    val intensity: String = "Moderate", // "Low", "Moderate", "High"
    val date: String = "", // YYYY-MM-DD
    val startTime: String = "",
    val endTime: String = "",
    val notes: String? = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FitnessGoal(
    val goalId: String = "",
    val userId: String = "",
    val goalType: String = "Daily Steps", // "Daily Steps", "Daily Calories", "Daily Active Minutes", "Weekly Workouts", "Weekly Duration"
    val targetValue: Double = 10000.0,
    val currentValue: Double = 0.0,
    val period: String = "Daily", // "Daily", "Weekly"
    val startDate: String = "",
    val endDate: String? = "",
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkoutPlan(
    val planId: String = "",
    val userId: String = "",
    val planName: String = "",
    val description: String = "",
    val difficulty: String = "Beginner", // "Beginner", "Intermediate", "Advanced"
    val exercises: List<String> = emptyList(),
    val durationWeeks: Int = 4,
    val isCustom: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReminderItem(
    val reminderId: String = "",
    val userId: String = "",
    val reminderType: String = "Daily Workout", // "Daily Workout", "Step Goal", "Water Reminder", "Weekly Progress"
    val reminderTime: String = "08:00",
    val isEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AchievementItem(
    val achievementId: String = "",
    val userId: String = "",
    val achievementName: String = "",
    val description: String = "",
    val icon: String = "emoji_events",
    @ServerTimestamp val achievedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WeightRecord(
    val recordId: String = "",
    val userId: String = "",
    val weight: Double = 70.0,
    val date: String = "", // YYYY-MM-DD
    @ServerTimestamp val createdAt: Timestamp? = null
)

// Active workout state model for Realtime Database / UI session
data class ActiveWorkoutSession(
    val workoutId: String = "",
    val workoutName: String = "",
    val category: String = "Running",
    val startTime: Long = 0L,
    val elapsedSeconds: Int = 0,
    val isPaused: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
