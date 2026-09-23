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
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers the user application that was visible before Klick'r was opened. */
@Singleton
class ForegroundAppTracker private constructor(
    private val context: Context,
    private val homePackageNameProvider: () -> String?,
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context = context,
        homePackageNameProvider = { resolveHomePackageName(context) },
    )

    internal constructor(context: Context, homePackageName: String?) : this(
        context = context,
        homePackageNameProvider = { homePackageName },
    )

    private val packageManager = context.packageManager
    private val homePackageName: String? by lazy(homePackageNameProvider)

    @Volatile
    private var foregroundPackageName: String? = null

    /** Records a foreground-window change reported by the accessibility service. */
    fun onWindowStateChanged(packageName: CharSequence?, isFullScreen: Boolean) {
        if (!isFullScreen) return

        val newPackageName = packageName?.toString() ?: return

        when {
            newPackageName == context.packageName -> Unit
            newPackageName == homePackageName -> foregroundPackageName = null
            packageManager.getLaunchIntentForPackage(newPackageName) != null -> {
                foregroundPackageName = newPackageName
            }
        }
    }

    /** Takes a stable snapshot before Klick'r and system permission windows become foreground. */
    fun snapshot(): String? = foregroundPackageName

    /** Brings the snapshotted application's existing task to the foreground when possible. */
    fun restore(packageName: String?): Boolean {
        if (packageName == null || packageName == context.packageName || packageName == homePackageName) return false

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

        return try {
            context.startActivity(launchIntent)
            true
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Can't restore previous foreground package $packageName", exception)
            false
        }
    }

    private companion object {
        fun resolveHomePackageName(context: Context): String? =
            context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0,
            )?.activityInfo?.packageName
    }
}

private const val TAG = "ForegroundAppTracker"
