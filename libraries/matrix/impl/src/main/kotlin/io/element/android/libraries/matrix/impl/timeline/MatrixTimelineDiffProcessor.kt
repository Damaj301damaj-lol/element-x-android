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

package io.element.android.libraries.matrix.impl.timeline

import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.rustcomponents.sdk.TimelineChange
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem

internal class MatrixTimelineDiffProcessor(
    private val timelineItems: MutableStateFlow<List<MatrixTimelineItem>>,
    private val timelineItemFactory: MatrixTimelineItemMapper,
) {

    private val mutex = Mutex()

    suspend fun postItems(items: List<TimelineItem>) {
        updateTimelineItems {
            val mappedItems = items.map { it.asMatrixTimelineItem() }
            addAll(0, mappedItems)
        }
    }

    suspend fun postDiff(diff: TimelineDiff) {
        updateTimelineItems {
            applyDiff(diff)
        }
    }

    private suspend fun updateTimelineItems(block: MutableList<MatrixTimelineItem>.() -> Unit) =
        mutex.withLock {
            val mutableTimelineItems = timelineItems.value.toMutableList()
            block(mutableTimelineItems)
            timelineItems.value = mutableTimelineItems
        }

    private fun MutableList<MatrixTimelineItem>.applyDiff(diff: TimelineDiff) {
        when (diff.change()) {
            TimelineChange.APPEND -> {
                val items = diff.append()?.map { it.asMatrixTimelineItem() } ?: return
                addAll(items)
            }
            TimelineChange.PUSH_BACK -> {
                val item = diff.pushBack()?.asMatrixTimelineItem() ?: return
                add(item)
            }
            TimelineChange.PUSH_FRONT -> {
                val item = diff.pushFront()?.asMatrixTimelineItem() ?: return
                add(0, item)
            }
            TimelineChange.SET -> {
                val updateAtData = diff.set() ?: return
                val item = updateAtData.item.asMatrixTimelineItem()
                set(updateAtData.index.toInt(), item)
            }
            TimelineChange.INSERT -> {
                val insertAtData = diff.insert() ?: return
                val item = insertAtData.item.asMatrixTimelineItem()
                add(insertAtData.index.toInt(), item)
            }
            TimelineChange.REMOVE -> {
                val removeAtData = diff.remove() ?: return
                removeAt(removeAtData.toInt())
            }
            TimelineChange.RESET -> {
                clear()
                val items = diff.reset()?.map { it.asMatrixTimelineItem() } ?: return
                addAll(items)
            }
            TimelineChange.POP_FRONT -> {
                removeFirstOrNull()
            }
            TimelineChange.POP_BACK -> {
                removeLastOrNull()
            }
            TimelineChange.CLEAR -> {
                clear()
            }
        }
    }

    private fun TimelineItem.asMatrixTimelineItem(): MatrixTimelineItem {
        return timelineItemFactory.map(this)
    }
}
