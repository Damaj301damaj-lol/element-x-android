/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.x.features.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.element.android.x.designsystem.components.preferences.PreferenceScreen
import io.element.android.x.element.resources.R as ElementR
import io.element.android.x.features.logout.LogoutPreference
import io.element.android.x.features.rageshake.preferences.RageshakePreferences

@Composable
fun PreferencesScreen(
    onBackPressed: () -> Unit = {},
    onOpenRageShake: () -> Unit = {},
    onSuccessLogout: () -> Unit = {},
) {
    // TODO Hierarchy!
    // Include pref from other modules
    PreferencesContent(
        onBackPressed = onBackPressed,
        onOpenRageShake = onOpenRageShake,
        onSuccessLogout = onSuccessLogout,
    )
}

@Composable
fun PreferencesContent(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onOpenRageShake: () -> Unit = {},
    onSuccessLogout: () -> Unit = {},
) {
    PreferenceScreen(
        modifier = modifier,
        onBackPressed = onBackPressed,
        title = stringResource(id = ElementR.string.settings)
    ) {
        LogoutPreference(onSuccessLogout = onSuccessLogout)
        RageshakePreferences(onOpenRageShake = onOpenRageShake)
    }
}

@Preview
@Composable
fun PreferencesContentPreview() {
    PreferencesContent()
}