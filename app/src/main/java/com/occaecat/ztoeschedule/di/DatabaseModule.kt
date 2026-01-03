package com.occaecat.ztoeschedule.di

import android.content.Context
import androidx.room.Room
import com.occaecat.ztoeschedule.data.local.EnergyDatabase
import com.occaecat.ztoeschedule.data.local.dao.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EnergyDatabase {
        return Room.databaseBuilder(
            context,
            EnergyDatabase::class.java,
            "energy_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideScheduleDao(database: EnergyDatabase): ScheduleDao {
        return database.scheduleDao()
    }
}
