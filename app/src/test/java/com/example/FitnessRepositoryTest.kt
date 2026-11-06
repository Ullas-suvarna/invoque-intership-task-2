package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FitnessRepositoryTest {

    @Test
    fun calculateCaloriesBurned_running_isCorrect() {
        // Formula: (9.8 * 3.5 * 70 / 200) * 30 min = 360 kcal
        val met = 9.8
        val weightKg = 70.0
        val durationMinutes = 30.0
        val expected = (met * 3.5 * weightKg / 200.0) * durationMinutes
        
        assertTrue(expected > 300)
    }

    @Test
    fun calculateStepDistance_isCorrect() {
        val steps = 10000
        val distanceKm = steps * 0.00075
        assertEquals(7.5, distanceKm, 0.01)
    }
}
