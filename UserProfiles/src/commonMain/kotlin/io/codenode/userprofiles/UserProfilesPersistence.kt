package io.codenode.userprofiles

import io.codenode.userprofiles.persistence.UserProfileDao
import io.codenode.userprofiles.persistence.UserProfileRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

/**
 * Koin module for UserProfiles persistence dependencies.
 * The app layer provides the [UserProfileDao] singleton;
 * this module wires it into a [UserProfileRepository].
 */
val userProfilesModule = module {
    single { UserProfileRepository(get()) }
}

/**
 * Koin-backed accessor for persistence dependencies.
 * Used by processing logic tick functions that cannot take constructor parameters.
 */
object UserProfilesPersistence : KoinComponent {
    val dao: UserProfileDao by inject()
}
