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
import java.io.File

internal const val RETURN_MODE_REMOVE_TASK = 0
internal const val RETURN_MODE_RESTORE_APP = 1
internal const val RETURN_MODE_TRANSLUCENT = 2

/** Reads the experimental scenario return strategy from app-specific external storage. */
internal fun Context.getScenarioReturnMode(): Int {
    val configuredMode = getExternalFilesDir(null)
        ?.let { externalFilesDir -> File(externalFilesDir, FEATURES_FILE_NAME) }
        ?.takeIf(File::isFile)
        ?.runCatching { readText().trim().toInt() }
        ?.getOrNull()

    return configuredMode?.takeIf { mode -> mode in RETURN_MODE_REMOVE_TASK..RETURN_MODE_TRANSLUCENT }
        ?: RETURN_MODE_REMOVE_TASK
}

private const val FEATURES_FILE_NAME = "features"
