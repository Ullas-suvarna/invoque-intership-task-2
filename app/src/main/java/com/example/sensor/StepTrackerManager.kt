package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepTrackerManager(context: Context) : SensorEventListener {

    private val sensorManager = try {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    } catch (e: Exception) {
        Log.e("StepTrackerManager", "Error getting SensorManager", e)
        null
    }

    private val stepSensor: Sensor? = try {
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    } catch (e: Exception) {
        Log.e("StepTrackerManager", "Error getting step sensor", e)
        null
    }

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(stepSensor != null)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    private var initialStepCount = -1

    fun startListening() {
        try {
            if (stepSensor != null && sensorManager != null) {
                val registered = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
                Log.d("StepTrackerManager", "Step sensor registered: $registered")
            } else {
                Log.w("StepTrackerManager", "Step counter sensor not available on this device")
            }
        } catch (e: Exception) {
            Log.e("StepTrackerManager", "SecurityException or error registering step listener", e)
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.e("StepTrackerManager", "Error unregistering listener", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                val totalStepsSinceBoot = event.values[0].toInt()
                if (initialStepCount < 0) {
                    initialStepCount = totalStepsSinceBoot
                }
                val stepsToday = totalStepsSinceBoot - initialStepCount
                _stepCount.value = Math.max(0, stepsToday)
            }
        } catch (e: Exception) {
            Log.e("StepTrackerManager", "Error in onSensorChanged", e)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    fun setStepsManually(newSteps: Int) {
        _stepCount.value = Math.max(0, newSteps)
    }

    fun addSimulatedSteps(additionalSteps: Int) {
        _stepCount.value += additionalSteps
    }
}
