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

package io.element.android.libraries.matrix.impl.room

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.RoomSummaryDetails
import io.element.android.libraries.matrix.impl.room.message.RoomMessageFactory
import org.matrix.rustcomponents.sdk.Room
import org.matrix.rustcomponents.sdk.RoomListItem

class RoomSummaryDetailsFactory(private val roomMessageFactory: RoomMessageFactory = RoomMessageFactory()) {

    suspend fun create(roomListItem: RoomListItem, room: Room?): RoomSummaryDetails {
        val latestRoomMessage = roomListItem.latestEvent()?.use {
            roomMessageFactory.create(it)
        }
        return RoomSummaryDetails(
            roomId = RoomId(roomListItem.id()),
            name = roomListItem.name() ?: roomListItem.id(),
            canonicalAlias = roomListItem.canonicalAlias(),
            isDirect = roomListItem.isDirect(),
            avatarURLString = roomListItem.avatarUrl(),
            unreadNotificationCount = roomListItem.unreadNotifications().use { it.notificationCount().toInt() },
            lastMessage = latestRoomMessage,
            lastMessageTimestamp = latestRoomMessage?.originServerTs,
            inviter = room?.inviter()?.let(RoomMemberMapper::map),
        )
    }
}
