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

package io.element.android.features.messages.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.features.messages.impl.actionlist.ActionListEvents
import io.element.android.features.messages.impl.actionlist.ActionListView
import io.element.android.features.messages.impl.actionlist.model.TimelineItemAction
import io.element.android.features.messages.impl.attachments.Attachment
import io.element.android.features.messages.impl.messagecomposer.AttachmentsState
import io.element.android.features.messages.impl.messagecomposer.MessageComposerView
import io.element.android.features.messages.impl.timeline.TimelineView
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionBottomSheet
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionEvents
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryEvents
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryView
import io.element.android.features.messages.impl.timeline.components.retrysendmenu.RetrySendMenuEvents
import io.element.android.features.messages.impl.timeline.components.retrysendmenu.RetrySendMessageMenu
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.networkmonitor.api.ui.ConnectivityIndicatorView
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.atomic.molecules.IconTitlePlaceholdersRowMolecule
import io.element.android.libraries.designsystem.components.ProgressDialog
import io.element.android.libraries.designsystem.components.ProgressDialogType
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.designsystem.utils.LogCompositions
import io.element.android.libraries.designsystem.utils.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.theme.ElementTheme
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import timber.log.Timber

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessagesView(
    state: MessagesState,
    onBackPressed: () -> Unit,
    onRoomDetailsClicked: () -> Unit,
    onEventClicked: (event: TimelineItem.Event) -> Unit,
    onUserDataClicked: (UserId) -> Unit,
    onPreviewAttachments: (ImmutableList<Attachment>) -> Unit,
    onSendLocationClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LogCompositions(tag = "MessagesScreen", msg = "Root")

    AttachmentStateView(state.composerState.attachmentsState, onPreviewAttachments)

    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)

    // This is needed because the composer is inside an AndroidView that can't be affected by the FocusManager in Compose
    val localView = LocalView.current

    LogCompositions(tag = "MessagesScreen", msg = "Content")

    fun onMessageClicked(event: TimelineItem.Event) {
        Timber.v("OnMessageClicked= ${event.id}")
        onEventClicked(event)
    }

    fun onMessageLongClicked(event: TimelineItem.Event) {
        Timber.v("OnMessageLongClicked= ${event.id}")
        localView.hideKeyboard()
        state.actionListState.eventSink(ActionListEvents.ComputeForMessage(event, state.userHasPermissionToRedact))
    }

    fun onActionSelected(action: TimelineItemAction, event: TimelineItem.Event) {
        state.eventSink(MessagesEvents.HandleAction(action, event))
    }

    fun onEmojiReactionClicked(emoji: String, event: TimelineItem.Event) {
        if (event.eventId == null) return
        state.eventSink(MessagesEvents.ToggleReaction(emoji, event.eventId))
    }

    fun onEmojiReactionLongClicked(emoji: String, event: TimelineItem.Event) {
        if (event.eventId == null) return
        state.reactionSummaryState.eventSink(ReactionSummaryEvents.ShowReactionSummary(event.eventId, event.reactionsState.reactions, emoji))
    }

    fun onMoreReactionsClicked(event: TimelineItem.Event) {
        state.customReactionState.eventSink(CustomReactionEvents.UpdateSelectedEvent(event.eventId))
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column {
                ConnectivityIndicatorView(isOnline = state.hasNetworkConnection)
                MessagesViewTopBar(
                    roomName = state.roomName.dataOrNull(),
                    roomAvatar = state.roomAvatar.dataOrNull(),
                    onBackPressed = onBackPressed,
                    onRoomDetailsClicked = onRoomDetailsClicked,
                )
            }
        },
        content = { padding ->
            MessagesViewContent(
                state = state,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                onMessageClicked = ::onMessageClicked,
                onMessageLongClicked = ::onMessageLongClicked,
                onUserDataClicked = onUserDataClicked,
                onTimestampClicked = { event ->
                    if (event.localSendState is LocalEventSendState.SendingFailed) {
                        state.retrySendMenuState.eventSink(RetrySendMenuEvents.EventSelected(event))
                    }
                },
                onReactionClicked = ::onEmojiReactionClicked,
                onReactionLongClicked = ::onEmojiReactionLongClicked,
                onMoreReactionsClicked = ::onMoreReactionsClicked,
                onSendLocationClicked = onSendLocationClicked,
                onSwipeToReply = { targetEvent ->
                    state.eventSink(MessagesEvents.HandleAction(TimelineItemAction.Reply, targetEvent))
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
    )

    ActionListView(
        state = state.actionListState,
        onActionSelected = ::onActionSelected,
        onCustomReactionClicked = { event ->
            state.customReactionState.eventSink(CustomReactionEvents.UpdateSelectedEvent(event.eventId))
        },
        onEmojiReactionClicked = ::onEmojiReactionClicked,
    )

    CustomReactionBottomSheet(
        state = state.customReactionState,
        onEmojiSelected = { emoji ->
            state.customReactionState.selectedEventId?.let { eventId ->
                state.eventSink(MessagesEvents.ToggleReaction(emoji.unicode, eventId))
                state.customReactionState.eventSink(CustomReactionEvents.UpdateSelectedEvent(null))
            }
        }
    )

    ReactionSummaryView(state = state.reactionSummaryState)
    RetrySendMessageMenu(
        state = state.retrySendMenuState
    )

    ReinviteDialog(
        state = state
    )
}

@Composable
private fun ReinviteDialog(state: MessagesState) {
    if (state.showReinvitePrompt) {
        ConfirmationDialog(
            title = stringResource(id = R.string.screen_room_invite_again_alert_title),
            content = stringResource(id = R.string.screen_room_invite_again_alert_message),
            cancelText = stringResource(id = CommonStrings.action_cancel),
            submitText = stringResource(id = CommonStrings.action_invite),
            emphasizeSubmitButton = true,
            onSubmitClicked = { state.eventSink(MessagesEvents.InviteDialogDismissed(InviteDialogAction.Invite)) },
            onDismiss = { state.eventSink(MessagesEvents.InviteDialogDismissed(InviteDialogAction.Cancel)) }
        )
    }
}

@Composable
private fun AttachmentStateView(
    state: AttachmentsState,
    onPreviewAttachments: (ImmutableList<Attachment>) -> Unit
) {
    when (state) {
        AttachmentsState.None -> Unit
        is AttachmentsState.Previewing -> LaunchedEffect(state) {
            onPreviewAttachments(state.attachments)
        }
        is AttachmentsState.Sending -> {
            ProgressDialog(
                type = when (state) {
                    is AttachmentsState.Sending.Uploading -> ProgressDialogType.Determinate(state.progress)
                    is AttachmentsState.Sending.Processing -> ProgressDialogType.Indeterminate
                },
                text = stringResource(id = CommonStrings.common_sending)
            )
        }
    }
}

@Composable
private fun MessagesViewContent(
    state: MessagesState,
    onMessageClicked: (TimelineItem.Event) -> Unit,
    onUserDataClicked: (UserId) -> Unit,
    onReactionClicked: (key: String, TimelineItem.Event) -> Unit,
    onReactionLongClicked: (key: String, TimelineItem.Event) -> Unit,
    onMoreReactionsClicked: (TimelineItem.Event) -> Unit,
    onMessageLongClicked: (TimelineItem.Event) -> Unit,
    onTimestampClicked: (TimelineItem.Event) -> Unit,
    onSendLocationClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeToReply: (TimelineItem.Event) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Hide timeline if composer is full screen
        if (!state.composerState.isFullScreen) {
            TimelineView(
                state = state.timelineState,
                modifier = Modifier.weight(1f),
                onMessageClicked = onMessageClicked,
                onMessageLongClicked = onMessageLongClicked,
                onUserDataClicked = onUserDataClicked,
                onTimestampClicked = onTimestampClicked,
                onReactionClicked = onReactionClicked,
                onReactionLongClicked = onReactionLongClicked,
                onMoreReactionsClicked = onMoreReactionsClicked,
                onSwipeToReply = onSwipeToReply,
            )
        }
        if (state.userHasPermissionToSendMessage) {
            MessageComposerView(
                state = state.composerState,
                onSendLocationClicked = onSendLocationClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Bottom)
            )
        } else {
            CantSendMessageBanner()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesViewTopBar(
    roomName: String?,
    roomAvatar: AvatarData?,
    modifier: Modifier = Modifier,
    onRoomDetailsClicked: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            BackButton(onClick = onBackPressed)
        },
        title = {
            val titleModifier = Modifier.clickable { onRoomDetailsClicked() }
            if (roomName != null && roomAvatar != null) {
                RoomAvatarAndNameRow(
                    roomName = roomName,
                    roomAvatar = roomAvatar,
                    modifier = titleModifier
                )
            } else {
                IconTitlePlaceholdersRowMolecule(
                    iconSize = AvatarSize.TimelineRoom.dp,
                    modifier = titleModifier
                )
            }
        },
        windowInsets = WindowInsets(0.dp)
    )
}

@Composable
private fun RoomAvatarAndNameRow(
    roomName: String,
    roomAvatar: AvatarData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(roomAvatar)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = roomName,
            style = ElementTheme.typography.fontBodyLgMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CantSendMessageBanner(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.screen_room_no_permission_to_post),
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Preview
@Composable
internal fun MessagesViewLightPreview(@PreviewParameter(MessagesStateProvider::class) state: MessagesState) =
    ElementPreviewLight { ContentToPreview(state) }

@Preview
@Composable
internal fun MessagesViewDarkPreview(@PreviewParameter(MessagesStateProvider::class) state: MessagesState) =
    ElementPreviewDark { ContentToPreview(state) }

@Composable
private fun ContentToPreview(state: MessagesState) {
    MessagesView(
        state = state,
        onBackPressed = {},
        onRoomDetailsClicked = {},
        onEventClicked = {},
        onPreviewAttachments = {},
        onUserDataClicked = {},
        onSendLocationClicked = {},
    )
}
