package com.occaecat.ztoeschedule.presentation.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EnergyCarAppService : CarAppService() {

    @Inject
    lateinit var repository: EnergyRepository

    @Inject
    lateinit var timeProvider: TimeProvider

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return EnergyCarSession(repository, timeProvider)
    }
}
