/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.navigation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForegroundAppTrackerTest {

    private val context: Context = mockk()
    private val packageManager: PackageManager = mockk()
    private val gameLaunchIntent: Intent = mockk(relaxed = true)

    private lateinit var tracker: ForegroundAppTracker

    @Before
    fun setUp() {
        every { context.packageName } returns KLICKR_PACKAGE
        every { context.packageManager } returns packageManager
        every { context.startActivity(any()) } just Runs
        every { packageManager.getLaunchIntentForPackage(any()) } returns null
        every { packageManager.getLaunchIntentForPackage(GAME_PACKAGE) } returns gameLaunchIntent

        tracker = ForegroundAppTracker(context, HOME_PACKAGE)
    }

    @Test
    fun onWindowStateChanged_launchableApp_updatesSnapshot() {
        tracker.onWindowStateChanged(GAME_PACKAGE, isFullScreen = true)

        assertEquals(GAME_PACKAGE, tracker.snapshot())
    }

    @Test
    fun onWindowStateChanged_transientSystemWindow_keepsPreviousApp() {
        tracker.onWindowStateChanged(GAME_PACKAGE, isFullScreen = true)
        tracker.onWindowStateChanged(SYSTEM_UI_PACKAGE, isFullScreen = true)

        assertEquals(GAME_PACKAGE, tracker.snapshot())
    }

    @Test
    fun onWindowStateChanged_floatingApp_keepsFullScreenApp() {
        tracker.onWindowStateChanged(GAME_PACKAGE, isFullScreen = true)
        tracker.onWindowStateChanged(CHAT_PACKAGE, isFullScreen = false)

        assertEquals(GAME_PACKAGE, tracker.snapshot())
    }

    @Test
    fun onWindowStateChanged_home_clearsPreviousApp() {
        tracker.onWindowStateChanged(GAME_PACKAGE, isFullScreen = true)
        tracker.onWindowStateChanged(HOME_PACKAGE, isFullScreen = true)

        assertNull(tracker.snapshot())
    }

    @Test
    fun restore_launchableApp_startsItsTask() {
        assertTrue(tracker.restore(GAME_PACKAGE))

        verify { gameLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) }
        verify { context.startActivity(gameLaunchIntent) }
    }

    @Test
    fun restore_withoutSnapshot_doesNothing() {
        assertFalse(tracker.restore(null))

        verify(exactly = 0) { context.startActivity(any()) }
    }
}

private const val KLICKR_PACKAGE = "com.buzbuz.smartautoclicker"
private const val GAME_PACKAGE = "com.example.game"
private const val CHAT_PACKAGE = "com.example.chat"
private const val HOME_PACKAGE = "com.example.launcher"
private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
