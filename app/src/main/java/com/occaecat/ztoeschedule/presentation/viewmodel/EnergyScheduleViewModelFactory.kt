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
            val repository = EnergyRepository(apiService, preferencesManager)

            return EnergyScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

