package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get Firestore instance", e)
            FirebaseFirestore.getInstance()
        }
    )

    private val auth: FirebaseAuth = Firebase.auth

    val currentUser: FirebaseUser?
        get() = try { auth.currentUser } catch (e: Exception) { null }

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            try {
                trySend(firebaseAuth.currentUser)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error sending auth state", e)
                trySend(null)
            }
        }
        try {
            auth.addAuthStateListener(listener)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to add auth listener", e)
        }
        awaitClose {
            try { auth.removeAuthStateListener(listener) } catch (e: Exception) {}
        }
    }

    suspend fun registerUser(fullName: String, email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("User creation failed")

            val profile = UserProfile(
                uid = user.uid,
                fullName = fullName,
                email = email,
                age = 25,
                height = 170.0,
                weight = 70.0,
                fitnessGoal = "General Fitness",
                preferredActivity = "Running"
            )
            saveUserProfile(profile)

            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Register failed", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("Login failed")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google sign in failed")

            try {
                val docRef = db.collection("users").document(user.uid)
                val snapshot = docRef.get().await()
                if (!snapshot.exists()) {
                    val profile = UserProfile(
                        uid = user.uid,
                        fullName = user.displayName ?: "Fitness User",
                        email = user.email ?: "",
                        photoURL = user.photoUrl?.toString(),
                        age = 25,
                        height = 170.0,
                        weight = 70.0,
                        fitnessGoal = "General Fitness",
                        preferredActivity = "Running"
                    )
                    saveUserProfile(profile)
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to check/create user profile document", e)
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Auth failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Password reset failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign out error", e)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = profile.uid.ifEmpty { auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in")) }
        return try {
            val map = mapOf(
                "uid" to uid,
                "fullName" to profile.fullName,
                "email" to profile.email,
                "photoURL" to profile.photoURL,
                "age" to profile.age,
                "height" to profile.height,
                "weight" to profile.weight,
                "gender" to profile.gender,
                "fitnessGoal" to profile.fitnessGoal,
                "preferredActivity" to profile.preferredActivity,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(map, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Save profile failed", e)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = try {
            db.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AuthRepository", "Error observing profile", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    try {
                        if (snapshot != null && snapshot.exists()) {
                            val profile = snapshot.toObject(UserProfile::class.java)?.copy(uid = snapshot.id)
                            trySend(profile)
                        } else {
                            trySend(null)
                        }
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Parsing profile error", e)
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception attaching profile listener", e)
            trySend(null)
            null
        }

        awaitClose {
            try { listener?.remove() } catch (e: Exception) {}
        }
    }
}
