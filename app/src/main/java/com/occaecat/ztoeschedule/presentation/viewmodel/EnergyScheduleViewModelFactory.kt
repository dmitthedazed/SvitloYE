package com.occaecat.ztoeschedule.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.network.RetrofitClient
import com.occaecat.ztoeschedule.data.repository.EnergyRepository

/**
 * Factory for creating EnergyScheduleViewModel with required dependencies
 */
class EnergyScheduleViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnergyScheduleViewModel::class.java)) {
            // Create dependencies
            val apiService = RetrofitClient.apiService
            val preferencesManager = EnergyPreferencesManager(context)
            val addressStorage = com.occaecat.ztoeschedule.data.local.AddressStorage(context)
            val repository = EnergyRepository(apiService, preferencesManager, addressStorage)
            val networkObserver = com.occaecat.ztoeschedule.domain.NetworkObserver(context)

            return EnergyScheduleViewModel(repository, networkObserver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

