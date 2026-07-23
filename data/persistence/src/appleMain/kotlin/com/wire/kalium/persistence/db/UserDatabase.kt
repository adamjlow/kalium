/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

@file:Suppress("MatchingDeclarationName")

package com.wire.kalium.persistence.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseFileContext.databasePath
import co.touchlab.sqliter.DatabaseFileContext.deleteDatabase
import com.wire.kalium.persistence.UserDatabase
import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.util.FileNameUtil
import kotlinx.coroutines.CoroutineDispatcher
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

@Suppress("LongParameterList")
actual fun userDatabaseBuilder(
    platformDatabaseData: PlatformDatabaseData,
    userId: UserIDEntity,
    passphrase: UserDBSecret?,
    dispatcher: CoroutineDispatcher,
    enableWAL: Boolean,
    dbInvalidationControlEnabled: Boolean
): UserDatabaseBuilder {
    val rawDriver = when (platformDatabaseData.storageData) {
        is StorageData.FileBacked -> {
            NSFileManager.defaultManager.createDirectoryAtPath(
                platformDatabaseData.storageData.storePath,
                true,
                null,
                null
            )
            databaseDriver(platformDatabaseData.storageData.storePath, FileNameUtil.userDBName(userId), UserDatabase.Schema.synchronous()) {
                isWALEnabled = enableWAL
                useGradleSafeSqliterLogging = platformDatabaseData.useGradleSafeSqliterLogging
            }
        }

        StorageData.InMemory ->
            databaseDriver(null, FileNameUtil.userDBName(userId), UserDatabase.Schema.synchronous()) {
                isWALEnabled = false
                useGradleSafeSqliterLogging = platformDatabaseData.useGradleSafeSqliterLogging
            }
    }

    val invalidationController = DbInvalidationController(
        enabled = dbInvalidationControlEnabled,
        notifyKey = { key -> rawDriver.notifyListeners(key) }
    )

    val driver: SqlDriver = MutedSqlDriver(
        delegate = rawDriver,
        invalidationController = invalidationController
    )

    return UserDatabaseBuilder(
        userId = userId,
        sqlDriver = driver,
        dispatcher = dispatcher,
        platformDatabaseData = platformDatabaseData,
        isEncrypted = passphrase != null,
        dbInvalidationController = invalidationController
    )
}

actual fun userDatabaseDriverByPath(
    platformDatabaseData: PlatformDatabaseData,
    path: String,
    passphrase: UserDBSecret?,
    enableWAL: Boolean
): SqlDriver {
    return NativeSqliteDriver(
        UserDatabase.Schema.synchronous(),
        path
    )
}

/**
 * Creates an in-memory user database,
 * or returns an existing one if it already exists.
 *
 * @param userId The ID of the user for whom the database is created.
 * @param dispatcher The coroutine dispatcher to be used for executing database operations.
 * @return The user database builder.
 */
fun inMemoryDatabase(
    userId: UserIDEntity,
    dispatcher: CoroutineDispatcher
): UserDatabaseBuilder = InMemoryDatabaseCache.getOrCreate(userId) {
    val rawDriver = databaseDriver(null, FileNameUtil.userDBName(userId), UserDatabase.Schema.synchronous()) {
        isWALEnabled = false
    }

    val invalidationController = DbInvalidationController(
        enabled = false,
        notifyKey = { key -> rawDriver.notifyListeners(key) }
    )

    val driver: SqlDriver = MutedSqlDriver(
        delegate = rawDriver,
        invalidationController = invalidationController
    )

    UserDatabaseBuilder(
        userId,
        driver,
        dispatcher,
        PlatformDatabaseData(StorageData.InMemory),
        false,
        invalidationController
    )
}

/**
 * Clears the in-memory database for the given user.
 * This closes the database connection and removes it from the cache,
 * causing SQLite to delete the shared in-memory database.
 *
 * @param userId The ID of the user whose database should be cleared.
 * @return `true` if the database was cleared, `false` if it didn't exist.
 */
fun clearInMemoryDatabase(userId: UserIDEntity): Boolean {
    return InMemoryDatabaseCache.clearEntry(userId)
}
internal actual fun nuke(
    userId: UserIDEntity,
    platformDatabaseData: PlatformDatabaseData
): Boolean {
    return when (platformDatabaseData.storageData) {
        is StorageData.FileBacked -> {
            // Delete only this user's DB file (and its -wal/-shm siblings), never the whole storage
            // directory, which is shared with the live user DB and other users' databases.
            deleteDatabase(FileNameUtil.userDBName(userId), platformDatabaseData.storageData.storePath)
            true
        }

        is StorageData.InMemory -> clearInMemoryDatabase(userId)
    }
}

internal actual fun getDatabaseAbsoluteFileLocation(
    platformDatabaseData: PlatformDatabaseData,
    userId: UserIDEntity
): String? {
    if (platformDatabaseData.storageData !is StorageData.FileBacked) {
        return null
    }
    // The DB file lives at storePath/<userDBName> (SQLiter basePath + name), not at storePath itself.
    val dbFilePath = databasePath(FileNameUtil.userDBName(userId), platformDatabaseData.storageData.storePath)
    return if (NSURL.fileURLWithPath(dbFilePath).checkResourceIsReachableAndReturnError(null)) {
        dbFilePath
    } else {
        null
    }
}

internal actual fun createEmptyDatabaseFile(
    platformDatabaseData: PlatformDatabaseData,
    userId: UserIDEntity,
): String? {
    if (platformDatabaseData.storageData !is StorageData.FileBacked) {
        return null
    }
    val storePath = platformDatabaseData.storageData.storePath
    val fileManager = NSFileManager.defaultManager
    // Make sure the storage directory exists before creating the file inside it.
    fileManager.createDirectoryAtPath(storePath, true, null, null)

    val dbFilePath = databasePath(FileNameUtil.userDBName(userId), storePath)
    // Remove any stale DB (and its -wal/-shm siblings) so we start from a truly empty file.
    deleteDatabase(FileNameUtil.userDBName(userId), storePath)

    return if (fileManager.createFileAtPath(dbFilePath, null, null)) {
        dbFilePath
    } else {
        null
    }
}
