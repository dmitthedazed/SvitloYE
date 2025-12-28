package com.occaecat.ztoeschedule.presentation.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.time.TimeProvider

class EnergyCarSession(
    private val repository: EnergyRepository,
    private val timeProvider: TimeProvider
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return MainCarScreen(carContext, repository, timeProvider)
    }
}
