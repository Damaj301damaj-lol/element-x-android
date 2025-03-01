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

package io.element.android.libraries.matrix.impl.timeline.postprocessor

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.timeline.MatrixTimeline
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import io.element.android.libraries.matrix.test.room.anEventTimelineItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Date

class TimelineEncryptedHistoryPostProcessorTest {

    private val defaultLastLoginTimestamp = Date(1_689_061_264L)

    @Test
    fun `given an unencrypted room, nothing is done`() = runTest {
        val processor = createPostProcessor(isRoomEncrypted = false)
        val items = listOf(
            MatrixTimelineItem.Event(0L, anEventTimelineItem())
        )
        assertThat(processor.process(items)).isSameInstanceAs(items)
    }

    @Test
    fun `given a null lastLoginTimestamp, nothing is done`() = runTest {
        val processor = createPostProcessor(lastLoginTimestamp = null)
        val items = listOf(
            MatrixTimelineItem.Event(0L, anEventTimelineItem())
        )
        assertThat(processor.process(items)).isSameInstanceAs(items)
    }

    @Test
    fun `given an empty list, nothing is done`() = runTest {
        val processor = createPostProcessor()
        val items = emptyList<MatrixTimelineItem>()
        assertThat(processor.process(items)).isSameInstanceAs(items)
    }

    @Test
    fun `given a list with no items before lastLoginTimestamp, nothing is done`() = runTest {
        val processor = createPostProcessor()
        val items = listOf(
            MatrixTimelineItem.Event(0L, anEventTimelineItem(timestamp = defaultLastLoginTimestamp.time + 1))
        )
        assertThat(processor.process(items)).isSameInstanceAs(items)
    }

    @Test
    fun `given a list with an item with equal timestamp as lastLoginTimestamp, it's replaced`() = runTest {
        val processor = createPostProcessor()
        val items = listOf(
            MatrixTimelineItem.Event(0L, anEventTimelineItem(timestamp = defaultLastLoginTimestamp.time))
        )
        assertThat(processor.process(items))
            .isEqualTo(listOf(MatrixTimelineItem.Virtual(0L, VirtualTimelineItem.EncryptedHistoryBanner)))
    }

    @Test
    fun `given a list with an item with a lower timestamp than lastLoginTimestamp, it's replaced`() = runTest {
        val processor = createPostProcessor()
        val items = listOf(
            MatrixTimelineItem.Event(0L, anEventTimelineItem(timestamp = defaultLastLoginTimestamp.time - 1))
        )
        assertThat(processor.process(items)).isEqualTo(
            listOf(MatrixTimelineItem.Virtual(0L, VirtualTimelineItem.EncryptedHistoryBanner))
        )
    }

    @Test
    fun `given a list with several with lower or equal timestamps than lastLoginTimestamp, they're replaced and the user can't back paginate`() = runTest {
        val paginationStateFlow = MutableStateFlow(MatrixTimeline.PaginationState(hasMoreToLoadBackwards = true, isBackPaginating = false))
        val processor = createPostProcessor(paginationStateFlow = paginationStateFlow)
        val items = listOf(
            MatrixTimelineItem.Event(0L, anEventTimelineItem(timestamp = defaultLastLoginTimestamp.time - 1)),
            MatrixTimelineItem.Event(0L, anEventTimelineItem(timestamp = defaultLastLoginTimestamp.time)),
            MatrixTimelineItem.Event(0L, anEventTimelineItem(timestamp = defaultLastLoginTimestamp.time + 1)),
        )
        assertThat(processor.process(items)).isEqualTo(
            listOf(
                MatrixTimelineItem.Virtual(0L, VirtualTimelineItem.EncryptedHistoryBanner),
                MatrixTimelineItem.Event(0L, anEventTimelineItem(timestamp = defaultLastLoginTimestamp.time + 1))
            )
        )
        assertThat(paginationStateFlow.value).isEqualTo(MatrixTimeline.PaginationState(hasMoreToLoadBackwards = false, isBackPaginating = false))
    }

    private fun TestScope.createPostProcessor(
        lastLoginTimestamp: Date? = defaultLastLoginTimestamp,
        isRoomEncrypted: Boolean = true,
        paginationStateFlow: MutableStateFlow<MatrixTimeline.PaginationState> =
            MutableStateFlow(MatrixTimeline.PaginationState(hasMoreToLoadBackwards = true, isBackPaginating = false))
    ) = TimelineEncryptedHistoryPostProcessor(
        lastLoginTimestamp = lastLoginTimestamp,
        isRoomEncrypted = isRoomEncrypted,
        paginationStateFlow = paginationStateFlow,
        dispatcher = StandardTestDispatcher(testScheduler)
    )
}
