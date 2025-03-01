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

package io.element.android.services.appnavstate.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_SPACE_ID
import io.element.android.libraries.matrix.test.A_THREAD_ID
import io.element.android.services.appnavstate.api.NavigationState
import io.element.android.services.appnavstate.test.A_ROOM_OWNER
import io.element.android.services.appnavstate.test.A_SESSION_OWNER
import io.element.android.services.appnavstate.test.A_SPACE_OWNER
import io.element.android.services.appnavstate.test.A_THREAD_OWNER
import io.element.android.tests.testutils.runCancellableScopeTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertThrows
import org.junit.Test

class DefaultNavigationStateServiceTest {

    @Test
    fun testNavigation() = runCancellableScopeTest { scope ->
        val service = createStateService(scope)
        service.onNavigateToSession(A_SESSION_OWNER, A_SESSION_ID)
        service.onNavigateToSpace(A_SPACE_OWNER, A_SPACE_ID)
        service.onNavigateToRoom(A_ROOM_OWNER, A_ROOM_ID)
        service.onNavigateToThread(A_THREAD_OWNER, A_THREAD_ID)
        assertThat(service.appNavigationState.first().navigationState).isEqualTo(
            NavigationState.Thread(
                A_THREAD_OWNER, A_THREAD_ID,
                NavigationState.Room(
                    A_ROOM_OWNER,
                    A_ROOM_ID,
                    NavigationState.Space(
                        A_SPACE_OWNER,
                        A_SPACE_ID,
                        NavigationState.Session(
                            A_SESSION_OWNER,
                            A_SESSION_ID
                        )
                    )
                )
            )
        )
    }

    @Test
    fun testFailure() = runCancellableScopeTest { scope ->
        val service = createStateService(scope)

        assertThrows(IllegalStateException::class.java) { service.onNavigateToSpace(A_SPACE_OWNER, A_SPACE_ID) }
    }

    private fun createStateService(
        coroutineScope: CoroutineScope
    ) = DefaultAppNavigationStateService(FakeAppForegroundStateService(), coroutineScope)
}
