package com.occaecat.ztoeschedule.di

import android.content.Context
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock
import com.occaecat.ztoeschedule.domain.time.KronosTimeProvider
import com.occaecat.ztoeschedule.domain.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideKronosClock(@ApplicationContext context: Context): KronosClock {
        return AndroidClockFactory.createKronosClock(context)
    }

    @Provides
    @Singleton
    fun provideTimeProvider(kronosClock: KronosClock): TimeProvider {
        return KronosTimeProvider(kronosClock)
    }
}
