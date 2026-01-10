package com.occaecat.ztoeschedule.widget.data

import com.occaecat.ztoeschedule.data.model.Schedule
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.domain.ScheduleDomainLogic
import com.occaecat.ztoeschedule.domain.GroupedSchedule
import com.occaecat.ztoeschedule.domain.ScheduleMapper
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Централізований провайдер даних для всіх віджетів.
 * Уникає дублювання логіки між різними типами віджетів.
 */
@Singleton
class WidgetDataProvider @Inject constructor(
    private val repository: EnergyRepository,
    private val preferencesManager: EnergyPreferencesManager
) {
    
    /**
     * Отримує дані для відображення у віджеті
     */
    suspend fun getWidgetData(): WidgetData {
        val selection = preferencesManager.savedSelectionFlow.first()
        
        // Перевірка чи налаштований віджет
        if (selection == null || selection.cherga == 0) {
            return WidgetData.NotConfigured
        }
        
        // Отримання даних з репозиторію
        return repository.getCachedScheduleWithMessages(selection.cherga, selection.pidcherga)
            .fold(
                onSuccess = { data ->
                    val currentStatus = ScheduleDomainLogic.getCurrentStatus(data.schedules)
                    val nextStatus = ScheduleDomainLogic.getNextStatus(data.schedules)
                    val grouped = com.occaecat.ztoeschedule.domain.ScheduleMapper.getGroupedSchedule(data.schedules)
                    
                    WidgetData.Loaded(
                        currentStatus = currentStatus,
                        nextStatus = nextStatus,
                        schedules = grouped,
                        addressName = selection.addressName,
                        cityName = selection.cityName ?: "",
                        cherga = selection.cherga,
                        pidcherga = selection.pidcherga
                    )
                },
                onFailure = { error ->
                    WidgetData.Error(
                        message = error.message ?: "Помилка завантаження",
                        addressName = selection.addressName
                    )
                }
            )
    }
    
    /**
     * Отримує дані для конкретної адреси (для віджетів з вибором адреси)
     */
    suspend fun getWidgetDataForAddress(cherga: Int, pidcherga: Int): WidgetData {
        if (cherga == 0) {
            return WidgetData.NotConfigured
        }
        
        return repository.getCachedScheduleWithMessages(cherga, pidcherga)
            .fold(
                onSuccess = { data ->
                    val currentStatus = ScheduleDomainLogic.getCurrentStatus(data.schedules)
                    val nextStatus = ScheduleDomainLogic.getNextStatus(data.schedules)
                    val grouped = com.occaecat.ztoeschedule.domain.ScheduleMapper.getGroupedSchedule(data.schedules)
                    
                    WidgetData.Loaded(
                        currentStatus = currentStatus,
                        nextStatus = nextStatus,
                        schedules = grouped,
                        addressName = "Адреса",
                        cityName = "",
                        cherga = cherga,
                        pidcherga = pidcherga
                    )
                },
                onFailure = { error ->
                    WidgetData.Error(
                        message = error.message ?: "Помилка завантаження",
                        addressName = null
                    )
                }
            )
    }
}

/**
 * Sealed клас для різних станів даних віджета
 */
sealed class WidgetData {
    /**
     * Віджет не налаштований (немає вибраної адреси)
     */
    object NotConfigured : WidgetData()
    
    /**
     * Дані успішно завантажені
     */
    data class Loaded(
        val currentStatus: Schedule?,
        val nextStatus: Schedule?,
        val schedules: List<GroupedSchedule>, // Changed to GroupedSchedule
        val addressName: String,
        val cityName: String,
        val cherga: Int,
        val pidcherga: Int
    ) : WidgetData()
    
    /**
     * Помилка завантаження даних
     */
    data class Error(
        val message: String,
        val addressName: String?
    ) : WidgetData()
}
