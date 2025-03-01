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

package io.element.android.features.messages.impl.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstraintLayout
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReactions
import io.element.android.features.messages.impl.timeline.components.event.TimelineItemEventContentView
import io.element.android.features.messages.impl.timeline.components.event.toExtraPadding
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.TimelineItemGroupPosition
import io.element.android.features.messages.impl.timeline.model.bubble.BubbleState
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemLocationContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVideoContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemTextContent
import io.element.android.libraries.designsystem.components.EqualWidthColumn
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.swipe.SwipeableActionsState
import io.element.android.libraries.designsystem.swipe.rememberSwipeableActionsState
import io.element.android.libraries.designsystem.text.toPx
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.ui.components.AttachmentThumbnail
import io.element.android.libraries.matrix.ui.components.AttachmentThumbnailInfo
import io.element.android.libraries.matrix.ui.components.AttachmentThumbnailType
import io.element.android.libraries.theme.ElementTheme
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TimelineItemEventRow(
    event: TimelineItem.Event,
    isHighlighted: Boolean,
    canReply: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onUserDataClick: (UserId) -> Unit,
    inReplyToClick: (EventId) -> Unit,
    onTimestampClicked: (TimelineItem.Event) -> Unit,
    onReactionClick: (emoji: String, eventId: TimelineItem.Event) -> Unit,
    onReactionLongClick: (emoji: String, eventId: TimelineItem.Event) -> Unit,
    onMoreReactionsClick: (eventId: TimelineItem.Event) -> Unit,
    onSwipeToReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    fun onUserDataClicked() {
        onUserDataClick(event.senderId)
    }

    fun inReplyToClicked() {
        val inReplyToEventId = (event.inReplyTo as? InReplyTo.Ready)?.eventId ?: return
        inReplyToClick(inReplyToEventId)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (event.groupPosition.isNew()) {
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
        if (canReply) {
            val state: SwipeableActionsState = rememberSwipeableActionsState()
            val offset = state.offset.value
            val swipeThresholdPx = 40.dp.toPx()
            val thresholdCrossed = abs(offset) > swipeThresholdPx
            SwipeSensitivity(3f) {
                Box(Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.matchParentSize()) {
                        ReplySwipeIndicator({ offset / 120 })
                    }
                    TimelineItemEventRowContent(
                        modifier = Modifier
                            .absoluteOffset { IntOffset(x = offset.roundToInt(), y = 0) }
                            .draggable(
                                orientation = Orientation.Horizontal,
                                enabled = !state.isResettingOnRelease,
                                onDragStopped = {
                                    coroutineScope.launch {
                                        if (thresholdCrossed) {
                                            onSwipeToReply()
                                        }
                                        state.resetOffset()
                                    }
                                },
                                state = state.draggableState,
                            ),
                        event = event,
                        isHighlighted = isHighlighted,
                        interactionSource = interactionSource,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onTimestampClicked = onTimestampClicked,
                        inReplyToClicked = ::inReplyToClicked,
                        onUserDataClicked = ::onUserDataClicked,
                        onReactionClicked = { emoji -> onReactionClick(emoji, event) },
                        onReactionLongClicked = { emoji -> onReactionLongClick(emoji, event) },
                        onMoreReactionsClicked = { onMoreReactionsClick(event) },
                    )
                }
            }
        } else {
            TimelineItemEventRowContent(
                event = event,
                isHighlighted = isHighlighted,
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick,
                onTimestampClicked = onTimestampClicked,
                inReplyToClicked = ::inReplyToClicked,
                onUserDataClicked = ::onUserDataClicked,
                onReactionClicked = { emoji -> onReactionClick(emoji, event) },
                onReactionLongClicked = { emoji -> onReactionLongClick(emoji, event) },
                onMoreReactionsClicked = { onMoreReactionsClick(event) },
            )
        }
    }
}

/**
 * Impact ViewConfiguration.touchSlop by [sensitivityFactor].
 * Inspired from https://issuetracker.google.com/u/1/issues/269627294.
 * @param sensitivityFactor the factor to multiply the touchSlop by. The highest value, the more the user will
 * have to drag to start the drag.
 * @param content the content to display.
 */
@Composable
fun SwipeSensitivity(
    sensitivityFactor: Float,
    content: @Composable () -> Unit,
) {
    val current = LocalViewConfiguration.current
    CompositionLocalProvider(
        LocalViewConfiguration provides object : ViewConfiguration by current {
            override val touchSlop: Float
                get() = current.touchSlop * sensitivityFactor
        }
    ) {
        content()
    }
}

@Composable
private fun TimelineItemEventRowContent(
    event: TimelineItem.Event,
    isHighlighted: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTimestampClicked: (TimelineItem.Event) -> Unit,
    inReplyToClicked: () -> Unit,
    onUserDataClicked: () -> Unit,
    onReactionClicked: (emoji: String) -> Unit,
    onReactionLongClicked: (emoji: String) -> Unit,
    onMoreReactionsClicked: (event: TimelineItem.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun ConstrainScope.linkStartOrEnd(event: TimelineItem.Event) = if (event.isMine) {
        end.linkTo(parent.end)
    } else {
        start.linkTo(parent.start)
    }

    ConstraintLayout(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth(),
    ) {
        val (sender, message, reactions) = createRefs()

        // Sender
        val avatarStrokeSize = 3.dp
        if (event.showSenderInformation) {
            MessageSenderInformation(
                event.safeSenderName,
                event.senderAvatar,
                avatarStrokeSize,
                Modifier
                    .constrainAs(sender) {
                        top.linkTo(parent.top)
                    }
                    .padding(horizontal = 16.dp)
                    .zIndex(1f)
                    .clickable(onClick = onUserDataClicked)
            )
        }

        // Message bubble
        val bubbleState = BubbleState(
            groupPosition = event.groupPosition,
            isMine = event.isMine,
            isHighlighted = isHighlighted,
        )
        MessageEventBubble(
            modifier = Modifier
                .constrainAs(message) {
                    top.linkTo(sender.bottom, margin = -avatarStrokeSize - 8.dp)
                    this.linkStartOrEnd(event)
                },
            state = bubbleState,
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick,
        ) {
            MessageEventBubbleContent(
                event = event,
                interactionSource = interactionSource,
                onMessageClick = onClick,
                onMessageLongClick = onLongClick,
                inReplyToClick = inReplyToClicked,
                onTimestampClicked = {
                    onTimestampClicked(event)
                }
            )
        }

        // Reactions
        if (event.reactionsState.reactions.isNotEmpty()) {
            TimelineItemReactions(
                reactionsState = event.reactionsState,
                isOutgoing = event.isMine,
                onReactionClicked = onReactionClicked,
                onReactionLongClicked = onReactionLongClicked,
                onMoreReactionsClicked = { onMoreReactionsClicked(event) },
                modifier = Modifier
                    .constrainAs(reactions) {
                        top.linkTo(message.bottom, margin = (-4).dp)
                        this.linkStartOrEnd(event)
                    }
                    .zIndex(1f)
                    .padding(start = if (event.isMine) 16.dp else 36.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun MessageSenderInformation(
    sender: String,
    senderAvatar: AvatarData,
    avatarStrokeSize: Dp,
    modifier: Modifier = Modifier
) {
    val avatarStrokeColor = MaterialTheme.colorScheme.background
    val avatarSize = senderAvatar.size.dp
    Box(
        modifier = modifier
    ) {
        // Background of Avatar, to erase the corner of the message content
        Canvas(
            modifier = Modifier
                .size(size = avatarSize + avatarStrokeSize)
                .clipToBounds()
        ) {
            drawCircle(
                color = avatarStrokeColor,
                center = Offset(x = (avatarSize / 2).toPx(), y = (avatarSize / 2).toPx()),
                radius = (avatarSize / 2 + avatarStrokeSize).toPx()
            )
        }
        // Content
        Row {
            Avatar(senderAvatar)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = sender,
                color = MaterialTheme.colorScheme.primary,
                style = ElementTheme.typography.fontBodyMdMedium,
            )
        }
    }
}

@Composable
private fun MessageEventBubbleContent(
    event: TimelineItem.Event,
    interactionSource: MutableInteractionSource,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    inReplyToClick: () -> Unit,
    onTimestampClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMediaItem = event.content is TimelineItemImageContent
        || event.content is TimelineItemVideoContent
        || event.content is TimelineItemLocationContent
    val replyToDetails = event.inReplyTo as? InReplyTo.Ready

    // Long clicks are not not automatically propagated from a `clickable`
    // to its `combinedClickable` parent so we do it manually
    fun onTimestampLongClick() = onMessageLongClick()

    @Composable
    fun ContentView(
        modifier: Modifier = Modifier
    ) {
        TimelineItemEventContentView(
            content = event.content,
            interactionSource = interactionSource,
            onClick = onMessageClick,
            onLongClick = onMessageLongClick,
            extraPadding = event.toExtraPadding(),
            modifier = modifier,
        )
    }

    @Composable
    fun ContentAndTimestampView(
        overlayTimestamp: Boolean,
        modifier: Modifier = Modifier,
        contentModifier: Modifier = Modifier,
        timestampModifier: Modifier = Modifier,
    ) {
        if (overlayTimestamp) {
            Box(modifier) {
                ContentView(modifier = contentModifier)
                TimelineEventTimestampView(
                    event = event,
                    onClick = onTimestampClicked,
                    onLongClick = ::onTimestampLongClick,
                    modifier = timestampModifier
                        .padding(horizontal = 4.dp, vertical = 4.dp) // Outer padding
                        .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(10.0.dp))
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 4.dp, vertical = 2.dp) // Inner padding
                )
            }
        } else {
            Box(modifier) {
                ContentView(modifier = contentModifier)
                TimelineEventTimestampView(
                    event = event,
                    onClick = onTimestampClicked,
                    onLongClick = ::onTimestampLongClick,
                    modifier = timestampModifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    /** Used only for media items, with no reply to metadata. It displays the contents with no paddings. */
    @Composable
    fun SimpleMediaItemLayout(modifier: Modifier = Modifier) {
        ContentAndTimestampView(overlayTimestamp = true, modifier = modifier)
    }

    /** Used for every other type of message, groups the different components in a Column with some space between them. */
    @Composable
    fun CommonLayout(
        inReplyToDetails: InReplyTo.Ready?,
        modifier: Modifier = Modifier
    ) {
        EqualWidthColumn(modifier = modifier, spacing = 8.dp) {
            if (inReplyToDetails != null) {
                val senderName = inReplyToDetails.senderDisplayName ?: inReplyToDetails.senderId.value
                val attachmentThumbnailInfo = attachmentThumbnailInfoForInReplyTo(inReplyToDetails)
                val text = textForInReplyTo(inReplyToDetails)
                ReplyToContent(
                    senderName = senderName,
                    text = text,
                    attachmentThumbnailInfo = attachmentThumbnailInfo,
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = true, onClick = inReplyToClick),
                )
            }
            val modifierWithPadding = if (isMediaItem) {
                Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            } else {
                Modifier
            }

            val contentModifier = if (isMediaItem) {
                Modifier.clip(RoundedCornerShape(12.dp))
            } else {
                if (inReplyToDetails != null) {
                    Modifier.padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 8.dp)
                } else {
                    Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                }
            }

            ContentAndTimestampView(
                overlayTimestamp = isMediaItem,
                contentModifier = contentModifier,
                modifier = modifierWithPadding,
            )
        }
    }

    if (isMediaItem && replyToDetails == null) {
        SimpleMediaItemLayout()
    } else {
        CommonLayout(inReplyToDetails = replyToDetails, modifier = modifier)
    }
}

@Composable
private fun ReplyToContent(
    senderName: String,
    text: String?,
    attachmentThumbnailInfo: AttachmentThumbnailInfo?,
    modifier: Modifier = Modifier,
) {
    val paddings = if (attachmentThumbnailInfo != null) {
        PaddingValues(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    } else {
        PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    }
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(paddings)
    ) {
        if (attachmentThumbnailInfo != null) {
            AttachmentThumbnail(
                info = attachmentThumbnailInfo,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = senderName,
                style = ElementTheme.typography.fontBodySmMedium,
                textAlign = TextAlign.Start,
                color = ElementTheme.materialColors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = text.orEmpty(),
                style = ElementTheme.typography.fontBodyMdRegular,
                textAlign = TextAlign.Start,
                color = ElementTheme.materialColors.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun attachmentThumbnailInfoForInReplyTo(inReplyTo: InReplyTo.Ready): AttachmentThumbnailInfo? {
    val messageContent = inReplyTo.content as? MessageContent ?: return null
    return when (val type = messageContent.type) {
        is ImageMessageType -> AttachmentThumbnailInfo(
            thumbnailSource = type.info?.thumbnailSource,
            textContent = messageContent.body,
            type = AttachmentThumbnailType.Image,
            blurHash = type.info?.blurhash,
        )
        is VideoMessageType -> AttachmentThumbnailInfo(
            thumbnailSource = type.info?.thumbnailSource,
            textContent = messageContent.body,
            type = AttachmentThumbnailType.Video,
            blurHash = type.info?.blurhash,
        )
        is FileMessageType -> AttachmentThumbnailInfo(
            thumbnailSource = type.info?.thumbnailSource,
            textContent = messageContent.body,
            type = AttachmentThumbnailType.File,
        )
        is LocationMessageType -> AttachmentThumbnailInfo(
            textContent = messageContent.body,
            type = AttachmentThumbnailType.Location,
        )
        is AudioMessageType -> AttachmentThumbnailInfo(
            textContent = messageContent.body,
            type = AttachmentThumbnailType.Audio,
        )
        else -> null
    }
}

@Composable
private fun textForInReplyTo(inReplyTo: InReplyTo.Ready): String {
    val messageContent = inReplyTo.content as? MessageContent ?: return ""
    return when (messageContent.type) {
        is LocationMessageType -> stringResource(CommonStrings.common_shared_location)
        else -> messageContent.body
    }
}

@Preview
@Composable
internal fun TimelineItemEventRowLightPreview() =
    ElementPreviewLight { ContentToPreview() }

@Preview
@Composable
internal fun TimelineItemEventRowDarkPreview() =
    ElementPreviewDark { ContentToPreview() }

@Composable
private fun ContentToPreview() {
    Column {
        sequenceOf(false, true).forEach {
            TimelineItemEventRow(
                event = aTimelineItemEvent(
                    isMine = it,
                    content = aTimelineItemTextContent().copy(
                        body = "A long text which will be displayed on several lines and" +
                            " hopefully can be manually adjusted to test different behaviors."
                    ),
                    groupPosition = TimelineItemGroupPosition.First,
                ),
                isHighlighted = false,
                canReply = true,
                onClick = {},
                onLongClick = {},
                onUserDataClick = {},
                inReplyToClick = {},
                onReactionClick = { _, _ -> },
                onReactionLongClick = { _, _ -> },
                onMoreReactionsClick = {},
                onTimestampClicked = {},
                onSwipeToReply = {},
            )
            TimelineItemEventRow(
                event = aTimelineItemEvent(
                    isMine = it,
                    content = aTimelineItemImageContent().copy(
                        aspectRatio = 5f
                    ),
                    groupPosition = TimelineItemGroupPosition.Last,
                ),
                isHighlighted = false,
                canReply = true,
                onClick = {},
                onLongClick = {},
                onUserDataClick = {},
                inReplyToClick = {},
                onReactionClick = { _, _ -> },
                onReactionLongClick = { _, _ -> },
                onMoreReactionsClick = {},
                onTimestampClicked = {},
                onSwipeToReply = {},
            )
        }
    }
}

@Preview
@Composable
internal fun TimelineItemEventRowWithReplyLightPreview() =
    ElementPreviewLight { ContentToPreviewWithReply() }

@Preview
@Composable
internal fun TimelineItemEventRowWithReplyDarkPreview() =
    ElementPreviewDark { ContentToPreviewWithReply() }

@Composable
private fun ContentToPreviewWithReply() {
    Column {
        sequenceOf(false, true).forEach {
            val replyContent = if (it) {
                // Short
                "Message which are being replied."
            } else {
                // Long, to test 2 lines and ellipsis)
                "Message which are being replied, and which was long enough to be displayed on two lines (only!)."
            }
            TimelineItemEventRow(
                event = aTimelineItemEvent(
                    isMine = it,
                    content = aTimelineItemTextContent().copy(
                        body = "A long text which will be displayed on several lines and" +
                            " hopefully can be manually adjusted to test different behaviors."
                    ),
                    inReplyTo = aInReplyToReady(replyContent),
                    groupPosition = TimelineItemGroupPosition.First,
                ),
                isHighlighted = false,
                canReply = true,
                onClick = {},
                onLongClick = {},
                onUserDataClick = {},
                inReplyToClick = {},
                onReactionClick = { _, _ -> },
                onReactionLongClick = { _, _ -> },
                onMoreReactionsClick = {},
                onTimestampClicked = {},
                onSwipeToReply = {},
            )
            TimelineItemEventRow(
                event = aTimelineItemEvent(
                    isMine = it,
                    content = aTimelineItemImageContent().copy(
                        aspectRatio = 5f
                    ),
                    inReplyTo = aInReplyToReady(replyContent),
                    groupPosition = TimelineItemGroupPosition.Last,
                ),
                isHighlighted = false,
                canReply = true,
                onClick = {},
                onLongClick = {},
                onUserDataClick = {},
                inReplyToClick = {},
                onReactionClick = { _, _ -> },
                onReactionLongClick = { _, _ -> },
                onMoreReactionsClick = {},
                onTimestampClicked = {},
                onSwipeToReply = {},
            )
        }
    }
}

private fun aInReplyToReady(
    replyContent: String
): InReplyTo.Ready {
    return InReplyTo.Ready(
        eventId = EventId("\$event"),
        content = MessageContent(replyContent, null, false, TextMessageType(replyContent, null)),
        senderId = UserId("@Sender:domain"),
        senderDisplayName = "Sender",
        senderAvatarUrl = null,
    )
}

@Preview
@Composable
internal fun TimelineItemEventRowTimestampLightPreview(@PreviewParameter(TimelineItemEventForTimestampViewProvider::class) event: TimelineItem.Event) =
    ElementPreviewLight { ContentTimestampToPreview(event) }

@Preview
@Composable
internal fun TimelineItemEventRowTimestampDarkPreview(@PreviewParameter(TimelineItemEventForTimestampViewProvider::class) event: TimelineItem.Event) =
    ElementPreviewDark { ContentTimestampToPreview(event) }

@Composable
private fun ContentTimestampToPreview(event: TimelineItem.Event) {
    Column {
        val oldContent = event.content as TimelineItemTextContent
        listOf(
            "Text",
            "Text longer, displayed on 1 line",
            "Text which should be rendered on several lines",
        ).forEach { str ->
            listOf(false, true).forEach { useDocument ->
                TimelineItemEventRow(
                    event = event.copy(
                        content = oldContent.copy(
                            body = str,
                            htmlDocument = if (useDocument) Jsoup.parse(str) else null,
                        ),
                        reactionsState = aTimelineItemReactions(count = 0),
                        senderDisplayName = if (useDocument) "Document case" else "Text case",
                    ),
                    isHighlighted = false,
                    canReply = true,
                    onClick = {},
                    onLongClick = {},
                    onUserDataClick = {},
                    inReplyToClick = {},
                    onReactionClick = { _, _ -> },
                    onReactionLongClick = { _, _ -> },
                    onMoreReactionsClick = {},
                    onTimestampClicked = {},
                    onSwipeToReply = {},
                )
            }
        }
    }
}

@Preview
@Composable
internal fun TimelineItemEventRowWithManyReactionsLightPreview() =
    ElementPreviewLight { ContentWithManyReactionsToPreview() }

@Preview
@Composable
internal fun TimelineItemEventRowWithManyReactionsDarkPreview() =
    ElementPreviewDark { ContentWithManyReactionsToPreview() }

@Composable
private fun ContentWithManyReactionsToPreview() {
    Column {
        listOf(false, true).forEach { isMine ->
            TimelineItemEventRow(
                event = aTimelineItemEvent(
                    isMine = isMine,
                    content = aTimelineItemTextContent().copy(
                        body = "A couple of multi-line messages with many reactions attached." +
                            " One sent by me and another from someone else."
                    ),
                    timelineItemReactions = aTimelineItemReactions(count = 20),
                ),
                isHighlighted = false,
                canReply = true,
                onClick = {},
                onLongClick = {},
                onUserDataClick = {},
                inReplyToClick = {},
                onReactionClick = { _, _ -> },
                onReactionLongClick = { _, _ -> },
                onMoreReactionsClick = {},
                onSwipeToReply = {},
                onTimestampClicked = {},
            )
        }
    }
}
