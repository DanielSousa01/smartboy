package com.fcul.smartboy.di

import com.fcul.smartboy.repository.ChatRepository
import com.fcul.smartboy.repository.InventoryRepository
import com.fcul.smartboy.repository.MapRouteRepository
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.SellingRepository
import com.fcul.smartboy.repository.radiation.RadiationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    fun provideMapRouteRepository(
        auth: FirebaseAuth,
        database: FirebaseDatabase,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): MapRouteRepository {
        val user = auth.currentUser
            ?: throw IllegalStateException("User must be logged in to access MapRouteRepository")
        return MapRouteRepository(user, database, firestore, storage)
    }

    @Provides
    @Singleton
    fun provideRadiationRepository(
        firestore: FirebaseFirestore
    ): RadiationRepository {
        return RadiationRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): InventoryRepository {
        return InventoryRepository(
            auth,
            firestore
        )
    }

    @Provides
    @Singleton
    fun provideSellingRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): SellingRepository {
        return SellingRepository(
            auth,
            firestore
        )
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        auth: FirebaseAuth,
        database: FirebaseDatabase,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): ChatRepository {
        return ChatRepository(auth, database, firestore, storage)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        database: FirebaseDatabase
    ): ProfileRepository {
        return ProfileRepository(database)
    }
}

