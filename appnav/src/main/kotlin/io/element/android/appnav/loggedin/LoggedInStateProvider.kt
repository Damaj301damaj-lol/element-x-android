/*
 * Copyright (c) 2023 New Vector Ltd
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

package io.element.android.appnav.loggedin

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.matrix.api.sync.SyncState
import io.element.android.libraries.permissions.api.createDummyPostNotificationPermissionsState

open class LoggedInStateProvider : PreviewParameterProvider<LoggedInState> {
    override val values: Sequence<LoggedInState>
        get() = sequenceOf(
            aLoggedInState(),
            aLoggedInState(syncState = SyncState.Idle),
            // Add other state here
        )
}

fun aLoggedInState(
    syncState: SyncState = SyncState.Running,
) = LoggedInState(
    syncState = syncState,
    permissionsState = createDummyPostNotificationPermissionsState(),
    // eventSink = {}
)
