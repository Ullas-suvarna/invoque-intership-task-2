package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.repository.AuthRepository
import com.example.data.repository.FitnessRepository
import com.example.sensor.StepTrackerManager
import com.example.ui.MainApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.FitPulseTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private lateinit var stepTrackerManager: StepTrackerManager
    private lateinit var authRepository: AuthRepository
    private lateinit var fitnessRepository: FitnessRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        stepTrackerManager = StepTrackerManager(this)
        authRepository = AuthRepository(this)
        fitnessRepository = FitnessRepository(this)

        setContent {
            FitPulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUserState by authRepository.authStateFlow.collectAsState(initial = authRepository.currentUser)

                    if (currentUserState == null) {
                        AuthScreen(
                            authRepository = authRepository,
                            onAuthSuccess = { /* authStateFlow updates state automatically */ }
                        )
                    } else {
                        MainApp(
                            currentUser = currentUserState!!,
                            authRepository = authRepository,
                            fitnessRepository = fitnessRepository,
                            stepTrackerManager = stepTrackerManager,
                            onLogout = {
                                authRepository.signOut()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stepTrackerManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        stepTrackerManager.stopListening()
    }
}
