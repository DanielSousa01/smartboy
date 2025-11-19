package com.fcul.smartboy.di

import com.fcul.smartboy.repository.MapRouteRepository
import com.fcul.smartboy.repository.radiation.RadiationRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMapRouteRepository(): MapRouteRepository {
        return MapRouteRepository()
    }

    @Provides
    @Singleton
    fun provideRadiationRepository(
        firestore: FirebaseFirestore
    ): RadiationRepository {
        return RadiationRepository(firestore)
    }
}

