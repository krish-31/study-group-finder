package com.studygroup.finder.core.di

import com.studygroup.finder.data.repository.AuthRepository
import com.studygroup.finder.data.repository.ChatRepository
import com.studygroup.finder.data.repository.JoinRequestRepository
import com.studygroup.finder.data.repository.NotificationRepository
import com.studygroup.finder.data.repository.ReviewRepository
import com.studygroup.finder.data.repository.SessionRepository
import com.studygroup.finder.data.repository.StudyGroupRepository
import com.studygroup.finder.data.repository.UserRepository
import com.studygroup.finder.data.repository.impl.AuthRepositoryImpl
import com.studygroup.finder.data.repository.impl.ChatRepositoryImpl
import com.studygroup.finder.data.repository.impl.JoinRequestRepositoryImpl
import com.studygroup.finder.data.repository.impl.NotificationRepositoryImpl
import com.studygroup.finder.data.repository.impl.ReviewRepositoryImpl
import com.studygroup.finder.data.repository.impl.SessionRepositoryImpl
import com.studygroup.finder.data.repository.impl.StudyGroupRepositoryImpl
import com.studygroup.finder.data.repository.impl.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds each repository interface to its concrete implementation.
 *
 * Using [Binds] (rather than [dagger.Provides]) is more efficient because Dagger
 * generates less code — it only needs to delegate to the implementation's
 * [javax.inject.Inject] constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindStudyGroupRepository(impl: StudyGroupRepositoryImpl): StudyGroupRepository

    @Binds
    @Singleton
    abstract fun bindJoinRequestRepository(impl: JoinRequestRepositoryImpl): JoinRequestRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
