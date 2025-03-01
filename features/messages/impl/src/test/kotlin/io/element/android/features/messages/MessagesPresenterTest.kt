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

package io.element.android.features.messages

import android.net.Uri
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.features.analytics.test.FakeAnalyticsService
import io.element.android.features.messages.fixtures.aMessageEvent
import io.element.android.features.messages.fixtures.aTimelineItemsFactory
import io.element.android.features.messages.impl.InviteDialogAction
import io.element.android.features.messages.impl.MessagesEvents
import io.element.android.features.messages.impl.MessagesPresenter
import io.element.android.features.messages.impl.actionlist.ActionListPresenter
import io.element.android.features.messages.impl.actionlist.ActionListState
import io.element.android.features.messages.impl.actionlist.model.TimelineItemAction
import io.element.android.features.messages.impl.messagecomposer.MessageComposerContextImpl
import io.element.android.features.messages.impl.messagecomposer.MessageComposerEvents
import io.element.android.features.messages.impl.messagecomposer.MessageComposerPresenter
import io.element.android.features.messages.impl.timeline.TimelinePresenter
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionPresenter
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryPresenter
import io.element.android.features.messages.impl.timeline.components.retrysendmenu.RetrySendMenuPresenter
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemFileContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVideoContent
import io.element.android.features.messages.media.FakeLocalMediaFactory
import io.element.android.features.messages.utils.messagesummary.FakeMessageSummaryFormatter
import io.element.android.features.networkmonitor.test.FakeNetworkMonitor
import io.element.android.libraries.androidutils.clipboard.FakeClipboardHelper
import io.element.android.libraries.architecture.Async
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.utils.SnackbarDispatcher
import io.element.android.libraries.featureflag.test.FakeFeatureFlagService
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.matrix.api.room.MatrixRoomMembersState
import io.element.android.libraries.matrix.api.room.MessageEventType
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import io.element.android.libraries.matrix.test.AN_AVATAR_URL
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID_2
import io.element.android.libraries.matrix.test.core.aBuildMeta
import io.element.android.libraries.matrix.test.room.FakeMatrixRoom
import io.element.android.libraries.matrix.test.room.aRoomMember
import io.element.android.libraries.mediapickers.test.FakePickerProvider
import io.element.android.libraries.mediaupload.api.MediaSender
import io.element.android.libraries.mediaupload.test.FakeMediaPreProcessor
import io.element.android.libraries.textcomposer.MessageComposerMode
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.consumeItemsUntilTimeout
import io.element.android.tests.testutils.testCoroutineDispatchers
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MessagesPresenterTest {

    private val mockMediaUrl: Uri = mockk("localMediaUri")

    @Test
    fun `present - initial state`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = consumeItemsUntilTimeout().last()
            assertThat(initialState.roomId).isEqualTo(A_ROOM_ID)
            assertThat(initialState.roomName).isEqualTo(Async.Success(""))
            assertThat(initialState.roomAvatar).isEqualTo(Async.Success(AvatarData(id = A_ROOM_ID.value, name = "", size = AvatarSize.TimelineRoom)))
            assertThat(initialState.userHasPermissionToSendMessage).isTrue()
            assertThat(initialState.userHasPermissionToRedact).isFalse()
            assertThat(initialState.hasNetworkConnection).isTrue()
            assertThat(initialState.snackbarMessage).isNull()
            assertThat(initialState.inviteProgress).isEqualTo(Async.Uninitialized)
            assertThat(initialState.showReinvitePrompt).isFalse()
        }
    }

    @Test
    fun `present - handle toggling a reaction`() = runTest {
        val coroutineDispatchers = testCoroutineDispatchers(useUnconfinedTestDispatcher = true)
        val room = FakeMatrixRoom()
        val presenter = createMessagePresenter(matrixRoom = room, coroutineDispatchers = coroutineDispatchers)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.ToggleReaction("👍", AN_EVENT_ID))
            assertThat(room.myReactions.count()).isEqualTo(1)
            // No crashes when sending a reaction failed
            room.givenToggleReactionResult(Result.failure(IllegalStateException("Failed to send reaction")))
            initialState.eventSink.invoke(MessagesEvents.ToggleReaction("👍", AN_EVENT_ID))
            assertThat(room.myReactions.count()).isEqualTo(1)
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
        }
    }

    @Test
    fun `present - handle toggling a reaction twice`() = runTest {
        val coroutineDispatchers = testCoroutineDispatchers(useUnconfinedTestDispatcher = true)
        val room = FakeMatrixRoom()
        val presenter = createMessagePresenter(matrixRoom = room, coroutineDispatchers = coroutineDispatchers)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.ToggleReaction("👍", AN_EVENT_ID))
            assertThat(room.myReactions.count()).isEqualTo(1)

            initialState.eventSink.invoke(MessagesEvents.ToggleReaction("👍", AN_EVENT_ID))
            assertThat(room.myReactions.count()).isEqualTo(0)
        }
    }

    @Test
    fun `present - handle action forward`() = runTest {
        val navigator = FakeMessagesNavigator()
        val presenter = createMessagePresenter(navigator = navigator)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Forward, aMessageEvent()))
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
            assertThat(navigator.onForwardEventClickedCount).isEqualTo(1)
        }
    }

    @Test
    fun `present - handle action copy`() = runTest {
        val clipboardHelper = FakeClipboardHelper()
        val event = aMessageEvent()
        val presenter = createMessagePresenter(clipboardHelper = clipboardHelper)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Copy, event))
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
            assertThat(clipboardHelper.clipboardContents).isEqualTo((event.content as TimelineItemTextContent).body)
        }
    }

    @Test
    fun `present - handle action reply`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Reply, aMessageEvent()))
            val finalState = awaitItem()
            assertThat(finalState.composerState.mode).isInstanceOf(MessageComposerMode.Reply::class.java)
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
        }
    }

    @Test
    fun `present - handle action reply to an event with no id does nothing`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Reply, aMessageEvent(eventId = null)))
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
            // Otherwise we would have some extra items here
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `present - handle action reply to an image media message`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            val mediaMessage = aMessageEvent(
                content = TimelineItemImageContent(
                    body = "image.jpg",
                    mediaSource = MediaSource(AN_AVATAR_URL),
                    thumbnailSource = null,
                    mimeType = MimeTypes.Jpeg,
                    blurhash = null,
                    width = 20,
                    height = 20,
                    aspectRatio = 1.0f,
                    fileExtension = "jpg",
                    formattedFileSize = "4MB"
                )
            )
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Reply, mediaMessage))
            val finalState = awaitItem()
            assertThat(finalState.composerState.mode).isInstanceOf(MessageComposerMode.Reply::class.java)
            val replyMode = finalState.composerState.mode as MessageComposerMode.Reply
            assertThat(replyMode.attachmentThumbnailInfo).isNotNull()
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
        }
    }

    @Test
    fun `present - handle action reply to a video media message`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            val mediaMessage = aMessageEvent(
                content = TimelineItemVideoContent(
                    body = "video.mp4",
                    duration = 10L,
                    videoSource = MediaSource(AN_AVATAR_URL),
                    thumbnailSource = MediaSource(AN_AVATAR_URL),
                    mimeType = MimeTypes.Mp4,
                    blurHash = null,
                    width = 20,
                    height = 20,
                    aspectRatio = 1.0f,
                    fileExtension = "mp4",
                    formattedFileSize = "50MB"
                )
            )
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Reply, mediaMessage))
            val finalState = awaitItem()
            assertThat(finalState.composerState.mode).isInstanceOf(MessageComposerMode.Reply::class.java)
            val replyMode = finalState.composerState.mode as MessageComposerMode.Reply
            assertThat(replyMode.attachmentThumbnailInfo).isNotNull()
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
        }
    }

    @Test
    fun `present - handle action reply to a file media message`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            val mediaMessage = aMessageEvent(
                content = TimelineItemFileContent(
                    body = "file.pdf",
                    fileSource = MediaSource(AN_AVATAR_URL),
                    thumbnailSource = MediaSource(AN_AVATAR_URL),
                    formattedFileSize = "10 MB",
                    mimeType = MimeTypes.Pdf,
                    fileExtension = "pdf",
                )
            )
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Reply, mediaMessage))
            val finalState = awaitItem()
            assertThat(finalState.composerState.mode).isInstanceOf(MessageComposerMode.Reply::class.java)
            val replyMode = finalState.composerState.mode as MessageComposerMode.Reply
            assertThat(replyMode.attachmentThumbnailInfo).isNotNull()
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
        }
    }

    @Test
    fun `present - handle action edit`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Edit, aMessageEvent()))
            val finalState = awaitItem()
            assertThat(finalState.composerState.mode).isInstanceOf(MessageComposerMode.Edit::class.java)
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
        }
    }

    @Test
    fun `present - handle action redact`() = runTest {
        val coroutineDispatchers = testCoroutineDispatchers(useUnconfinedTestDispatcher = true)
        val matrixRoom = FakeMatrixRoom()
        val presenter = createMessagePresenter(matrixRoom = matrixRoom, coroutineDispatchers = coroutineDispatchers)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Redact, aMessageEvent()))
            assertThat(matrixRoom.redactEventEventIdParam).isEqualTo(AN_EVENT_ID)
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
        }
    }

    @Test
    fun `present - handle action report content`() = runTest {
        val navigator = FakeMessagesNavigator()
        val presenter = createMessagePresenter(navigator = navigator)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.ReportContent, aMessageEvent()))
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
            assertThat(navigator.onReportContentClickedCount).isEqualTo(1)
        }
    }

    @Test
    fun `present - handle dismiss action`() = runTest {
        val presenter = createMessagePresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.Dismiss)
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)

        }
    }

    @Test
    fun `present - handle action show developer info`() = runTest {
        val navigator = FakeMessagesNavigator()
        val presenter = createMessagePresenter(navigator = navigator)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = awaitItem()
            initialState.eventSink.invoke(MessagesEvents.HandleAction(TimelineItemAction.Developer, aMessageEvent()))
            assertThat(awaitItem().actionListState.target).isEqualTo(ActionListState.Target.None)
            assertThat(navigator.onShowEventDebugInfoClickedCount).isEqualTo(1)
        }
    }

    @Test
    fun `present - shows prompt to reinvite users in DM`() = runTest {
        val room = FakeMatrixRoom(sessionId = A_SESSION_ID, isDirect = true, activeMemberCount = 1L)
        val presenter = createMessagePresenter(matrixRoom = room)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            // Initially the composer doesn't have focus, so we don't show the alert
            assertThat(initialState.showReinvitePrompt).isFalse()
            // When the input field is focused we show the alert
            initialState.composerState.eventSink(MessageComposerEvents.FocusChanged(true))
            val focusedState = consumeItemsUntilPredicate { state ->
                state.showReinvitePrompt
            }.last()
            assertThat(focusedState.showReinvitePrompt).isTrue()
            // If it's dismissed then we stop showing the alert
            initialState.eventSink(MessagesEvents.InviteDialogDismissed(InviteDialogAction.Cancel))
            val dismissedState = consumeItemsUntilPredicate { state ->
                !state.showReinvitePrompt
            }.last()
            assertThat(dismissedState.showReinvitePrompt).isFalse()
        }
    }

    @Test
    fun `present - doesn't show reinvite prompt in non-direct room`() = runTest {
        val room = FakeMatrixRoom(sessionId = A_SESSION_ID, isDirect = false, activeMemberCount = 1L)
        val presenter = createMessagePresenter(matrixRoom = room)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            assertThat(initialState.showReinvitePrompt).isFalse()
            initialState.composerState.eventSink(MessageComposerEvents.FocusChanged(true))
            val focusedState = awaitItem()
            assertThat(focusedState.showReinvitePrompt).isFalse()
        }
    }

    @Test
    fun `present - doesn't show reinvite prompt if other party is present`() = runTest {
        val room = FakeMatrixRoom(sessionId = A_SESSION_ID, isDirect = true, activeMemberCount = 2L)
        val presenter = createMessagePresenter(matrixRoom = room)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            val initialState = awaitItem()
            assertThat(initialState.showReinvitePrompt).isFalse()
            initialState.composerState.eventSink(MessageComposerEvents.FocusChanged(true))
            val focusedState = awaitItem()
            assertThat(focusedState.showReinvitePrompt).isFalse()
        }
    }

    @Test
    fun `present - handle reinviting other user when memberlist is ready`() = runTest {
        val room = FakeMatrixRoom(sessionId = A_SESSION_ID)
        room.givenRoomMembersState(
            MatrixRoomMembersState.Ready(
                listOf(
                    aRoomMember(userId = A_SESSION_ID, membership = RoomMembershipState.JOIN),
                    aRoomMember(userId = A_SESSION_ID_2, membership = RoomMembershipState.LEAVE),
                )
            )
        )
        val presenter = createMessagePresenter(matrixRoom = room)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = consumeItemsUntilTimeout().last()
            initialState.eventSink(MessagesEvents.InviteDialogDismissed(InviteDialogAction.Invite))
            skipItems(1)
            val loadingState = awaitItem()
            assertThat(loadingState.inviteProgress.isLoading()).isTrue()
            val newState = awaitItem()
            assertThat(newState.inviteProgress.isSuccess()).isTrue()
            assertThat(room.invitedUserId).isEqualTo(A_SESSION_ID_2)
        }
    }

    @Test
    fun `present - handle reinviting other user when memberlist is error`() = runTest {
        val room = FakeMatrixRoom(sessionId = A_SESSION_ID)
        room.givenRoomMembersState(
            MatrixRoomMembersState.Error(
                failure = Throwable(),
                prevRoomMembers = listOf(
                    aRoomMember(userId = A_SESSION_ID, membership = RoomMembershipState.JOIN),
                    aRoomMember(userId = A_SESSION_ID_2, membership = RoomMembershipState.LEAVE),
                )
            )
        )
        val presenter = createMessagePresenter(matrixRoom = room)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = consumeItemsUntilTimeout().last()
            initialState.eventSink(MessagesEvents.InviteDialogDismissed(InviteDialogAction.Invite))
            skipItems(1)
            val loadingState = awaitItem()
            assertThat(loadingState.inviteProgress.isLoading()).isTrue()
            val newState = awaitItem()
            assertThat(newState.inviteProgress.isSuccess()).isTrue()
            assertThat(room.invitedUserId).isEqualTo(A_SESSION_ID_2)
        }
    }

    @Test
    fun `present - handle reinviting other user when memberlist is not ready`() = runTest {
        val room = FakeMatrixRoom(sessionId = A_SESSION_ID)
        room.givenRoomMembersState(MatrixRoomMembersState.Unknown)
        val presenter = createMessagePresenter(matrixRoom = room)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = consumeItemsUntilTimeout().last()
            initialState.eventSink(MessagesEvents.InviteDialogDismissed(InviteDialogAction.Invite))
            skipItems(1)
            val loadingState = awaitItem()
            assertThat(loadingState.inviteProgress.isLoading()).isTrue()
            val newState = awaitItem()
            assertThat(newState.inviteProgress.isFailure()).isTrue()
        }
    }

    @Test
    fun `present - handle reinviting other user when inviting fails`() = runTest {
        val room = FakeMatrixRoom(sessionId = A_SESSION_ID)
        room.givenRoomMembersState(
            MatrixRoomMembersState.Ready(
                listOf(
                    aRoomMember(userId = A_SESSION_ID, membership = RoomMembershipState.JOIN),
                    aRoomMember(userId = A_SESSION_ID_2, membership = RoomMembershipState.LEAVE),
                )
            )
        )
        room.givenInviteUserResult(Result.failure(Throwable("Oops!")))
        val presenter = createMessagePresenter(matrixRoom = room)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = consumeItemsUntilTimeout().last()
            initialState.eventSink(MessagesEvents.InviteDialogDismissed(InviteDialogAction.Invite))
            val loadingState = consumeItemsUntilPredicate { state ->
                state.inviteProgress.isLoading()
            }.last()
            assertThat(loadingState.inviteProgress.isLoading()).isTrue()
            val failureState = consumeItemsUntilPredicate { state ->
                state.inviteProgress.isFailure()
            }.last()
            assertThat(failureState.inviteProgress.isFailure()).isTrue()
        }
    }

    @Test
    fun `present - permission to post`() = runTest {
        val matrixRoom = FakeMatrixRoom()
        matrixRoom.givenCanSendEventResult(MessageEventType.ROOM_MESSAGE, Result.success(true))
        val presenter = createMessagePresenter(matrixRoom = matrixRoom)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            assertThat(awaitItem().userHasPermissionToSendMessage).isTrue()
        }
    }

    @Test
    fun `present - no permission to post`() = runTest {
        val matrixRoom = FakeMatrixRoom()
        matrixRoom.givenCanSendEventResult(MessageEventType.ROOM_MESSAGE, Result.success(false))
        val presenter = createMessagePresenter(matrixRoom = matrixRoom)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Default value
            assertThat(awaitItem().userHasPermissionToSendMessage).isTrue()
            skipItems(1)
            assertThat(awaitItem().userHasPermissionToSendMessage).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - permission to redact`() = runTest {
        val matrixRoom = FakeMatrixRoom(canRedact = true)
        val presenter = createMessagePresenter(matrixRoom = matrixRoom)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val initialState = consumeItemsUntilPredicate { it.userHasPermissionToRedact }.last()
            assertThat(initialState.userHasPermissionToRedact).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TestScope.createMessagePresenter(
        coroutineDispatchers: CoroutineDispatchers = testCoroutineDispatchers(),
        matrixRoom: MatrixRoom = FakeMatrixRoom(),
        navigator: FakeMessagesNavigator = FakeMessagesNavigator(),
        clipboardHelper: FakeClipboardHelper = FakeClipboardHelper(),
    ): MessagesPresenter {
        val messageComposerPresenter = MessageComposerPresenter(
            appCoroutineScope = this,
            room = matrixRoom,
            mediaPickerProvider = FakePickerProvider(),
            featureFlagService = FakeFeatureFlagService(),
            localMediaFactory = FakeLocalMediaFactory(mockMediaUrl),
            mediaSender = MediaSender(FakeMediaPreProcessor(), matrixRoom),
            snackbarDispatcher = SnackbarDispatcher(),
            analyticsService = FakeAnalyticsService(),
            messageComposerContext = MessageComposerContextImpl(),
        )
        val timelinePresenter = TimelinePresenter(
            timelineItemsFactory = aTimelineItemsFactory(),
            room = matrixRoom,
            dispatchers = coroutineDispatchers,
            appScope = this
        )
        val buildMeta = aBuildMeta()
        val actionListPresenter = ActionListPresenter(buildMeta = buildMeta)
        val customReactionPresenter = CustomReactionPresenter()
        val reactionSummaryPresenter = ReactionSummaryPresenter(room = matrixRoom)
        val retrySendMenuPresenter = RetrySendMenuPresenter(room = matrixRoom)
        return MessagesPresenter(
            room = matrixRoom,
            composerPresenter = messageComposerPresenter,
            timelinePresenter = timelinePresenter,
            actionListPresenter = actionListPresenter,
            customReactionPresenter = customReactionPresenter,
            reactionSummaryPresenter = reactionSummaryPresenter,
            retrySendMenuPresenter = retrySendMenuPresenter,
            networkMonitor = FakeNetworkMonitor(),
            snackbarDispatcher = SnackbarDispatcher(),
            messageSummaryFormatter = FakeMessageSummaryFormatter(),
            navigator = navigator,
            clipboardHelper = clipboardHelper,
            dispatchers = coroutineDispatchers,
        )
    }
}
