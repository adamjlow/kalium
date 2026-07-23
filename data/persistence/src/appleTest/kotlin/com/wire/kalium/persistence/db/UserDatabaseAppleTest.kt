/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.kalium.persistence.db

import co.touchlab.sqliter.DatabaseFileContext.databasePath
import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.util.FileNameUtil
import kotlinx.coroutines.test.StandardTestDispatcher
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression coverage for the iOS actuals in [UserDatabase.kt], which back the cross-platform
 * backup/restore flow ([com.wire.kalium.persistence.backup.DatabaseExporter] /
 * [com.wire.kalium.persistence.backup.ObfuscatedCopyExporter]). These functions previously
 * returned the storage directory, deleted the whole storage directory, or were left as `TODO()`.
 *
 * The functions under test are synchronous, and [userDatabaseBuilder] materialises the DB file
 * eagerly in its `init` block, so these tests need no coroutine scaffolding.
 */
class UserDatabaseAppleTest {

    private val dispatcher = StandardTestDispatcher()

    private val storePath: String = NSFileManager.defaultManager
        .URLForDirectory(NSCachesDirectory, NSUserDomainMask, null, true, null)!!
        .path!!
        .let { "$it/kalium-userdb-test-${NSUUID.UUID().UUIDString}" }
        .also { NSFileManager.defaultManager.createDirectoryAtPath(it, true, null, null) }

    private val platformDatabaseData = PlatformDatabaseData(
        storageData = StorageData.FileBacked(storePath),
        useGradleSafeSqliterLogging = true
    )

    private val selfUserId = UserIDEntity("selfValue", "selfDomain")

    @AfterTest
    fun tearDown() {
        NSFileManager.defaultManager.removeItemAtPath(storePath, null)
    }

    private fun createFileBackedDatabase(userId: UserIDEntity): UserDatabaseBuilder =
        userDatabaseBuilder(
            platformDatabaseData = platformDatabaseData,
            userId = userId,
            passphrase = null,
            dispatcher = dispatcher,
            enableWAL = false,
            dbInvalidationControlEnabled = false
        )

    private fun fileExists(path: String): Boolean =
        NSURL.fileURLWithPath(path).checkResourceIsReachableAndReturnError(null)

    @Test
    fun givenFileBackedDatabase_whenGettingAbsoluteFileLocation_thenReturnsTheDbFileNotTheDirectory() {
        createFileBackedDatabase(selfUserId)

        val location = getDatabaseAbsoluteFileLocation(platformDatabaseData, selfUserId)

        assertNotNull(location, "Expected a DB file path, got null")
        // Must be the actual DB file, not the storage directory.
        assertTrue(location.endsWith(FileNameUtil.userDBName(selfUserId)), "Expected a path to the DB file, was: $location")
        assertTrue(location != storePath, "Location must not be the storage directory")
        assertTrue(fileExists(location), "The returned DB file path must be reachable")
    }

    @Test
    fun givenNoDatabase_whenGettingAbsoluteFileLocation_thenReturnsNull() {
        assertNull(getDatabaseAbsoluteFileLocation(platformDatabaseData, selfUserId))
    }

    @Test
    fun givenLiveAndBackupDatabases_whenNukingTheBackup_thenOnlyTheBackupFileIsDeleted() {
        val backupUserId = selfUserId.copy(value = "backup-${selfUserId.value}")

        createFileBackedDatabase(selfUserId)
        createFileBackedDatabase(backupUserId)

        val liveDbPath = databasePath(FileNameUtil.userDBName(selfUserId), storePath)
        val backupDbPath = databasePath(FileNameUtil.userDBName(backupUserId), storePath)
        assertTrue(fileExists(liveDbPath), "Live DB should exist before nuking the backup")
        assertTrue(fileExists(backupDbPath), "Backup DB should exist before nuking")

        val result = nuke(backupUserId, platformDatabaseData)

        assertTrue(result, "nuke should report success")
        assertTrue(fileExists(liveDbPath), "The live user DB must NOT be deleted when nuking the backup DB")
        assertFalse(fileExists(backupDbPath), "The backup DB file should be deleted")
        assertTrue(fileExists(storePath), "The storage directory must NOT be deleted")
    }

    @Test
    fun givenFileBackedStorage_whenCreatingEmptyDatabaseFile_thenAnEmptyFileIsCreatedAtTheDbPath() {
        val path = createEmptyDatabaseFile(platformDatabaseData, selfUserId)

        assertNotNull(path, "Expected the created empty DB file path, got null")
        assertEquals(databasePath(FileNameUtil.userDBName(selfUserId), storePath), path)
        assertTrue(fileExists(path), "The created empty DB file must exist on disk")
    }
}
