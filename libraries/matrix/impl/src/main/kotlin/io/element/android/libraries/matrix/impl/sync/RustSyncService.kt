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

package io.element.android.libraries.matrix.impl.sync

import io.element.android.libraries.matrix.api.sync.SyncService
import io.element.android.libraries.matrix.api.sync.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.matrix.rustcomponents.sdk.RoomListServiceState
import org.matrix.rustcomponents.sdk.SyncServiceInterface
import timber.log.Timber

class RustSyncService(
    private val innerSyncService: SyncServiceInterface,
    roomListStateFlow: Flow<RoomListServiceState>,
    sessionCoroutineScope: CoroutineScope
) : SyncService {

    override suspend fun startSync() = runCatching {
        Timber.v("Start sync")
        innerSyncService.start()
    }

    override fun stopSync() = runCatching {
        Timber.v("Stop sync")
        innerSyncService.pause()
    }

    override val syncState: StateFlow<SyncState> =
        roomListStateFlow
            .map(RoomListServiceState::toSyncState)
            .onEach { state ->
                Timber.v("Sync state=$state")
            }
            .distinctUntilChanged()
            .stateIn(sessionCoroutineScope, SharingStarted.Eagerly, SyncState.Idle)
}
