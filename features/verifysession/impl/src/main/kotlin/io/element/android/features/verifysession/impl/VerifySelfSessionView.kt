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

package io.element.android.features.verifysession.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.libraries.architecture.Async
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.atomic.pages.HeaderFooterPage
import io.element.android.libraries.designsystem.components.button.ButtonWithProgress
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.theme.aliasButtonText
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.verification.VerificationEmoji
import io.element.android.libraries.theme.ElementTheme
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.features.verifysession.impl.VerifySelfSessionState.VerificationStep as FlowStep

@Composable
fun VerifySelfSessionView(
    state: VerifySelfSessionState,
    modifier: Modifier = Modifier,
    goBack: () -> Unit,
) {
    fun goBackAndCancelIfNeeded() {
        state.eventSink(VerifySelfSessionViewEvents.CancelAndClose)
        goBack()
    }
    if (state.verificationFlowStep is FlowStep.Completed) {
        goBack()
    }
    BackHandler {
        goBackAndCancelIfNeeded()
    }
    val verificationFlowStep = state.verificationFlowStep
    val buttonsVisible by remember(verificationFlowStep) {
        derivedStateOf { verificationFlowStep != FlowStep.AwaitingOtherDeviceResponse && verificationFlowStep != FlowStep.Completed }
    }
    HeaderFooterPage(
        modifier = modifier,
        header = {
            HeaderContent(verificationFlowStep = verificationFlowStep)
        },
        footer = {
            if (buttonsVisible) {
                BottomMenu(screenState = state, goBack = ::goBackAndCancelIfNeeded)
            }
        }
    ) {
        Content(flowState = verificationFlowStep)
    }
}

@Composable
internal fun HeaderContent(verificationFlowStep: FlowStep, modifier: Modifier = Modifier) {
    val iconResourceId = when (verificationFlowStep) {
        FlowStep.Initial -> R.drawable.ic_verification_devices
        FlowStep.Canceled -> R.drawable.ic_verification_warning
        FlowStep.AwaitingOtherDeviceResponse -> R.drawable.ic_verification_waiting
        FlowStep.Ready, is FlowStep.Verifying, FlowStep.Completed -> R.drawable.ic_verification_emoji
    }
    val titleTextId = when (verificationFlowStep) {
        FlowStep.Initial -> R.string.screen_session_verification_open_existing_session_title
        FlowStep.Canceled -> R.string.screen_session_verification_cancelled_title
        FlowStep.AwaitingOtherDeviceResponse -> R.string.screen_session_verification_waiting_to_accept_title
        FlowStep.Ready, is FlowStep.Verifying, FlowStep.Completed -> R.string.screen_session_verification_compare_emojis_title
    }
    val subtitleTextId = when (verificationFlowStep) {
        FlowStep.Initial -> R.string.screen_session_verification_open_existing_session_subtitle
        FlowStep.Canceled -> R.string.screen_session_verification_cancelled_subtitle
        FlowStep.AwaitingOtherDeviceResponse -> R.string.screen_session_verification_waiting_to_accept_subtitle
        FlowStep.Ready, is FlowStep.Verifying, FlowStep.Completed -> R.string.screen_session_verification_compare_emojis_subtitle
    }

    IconTitleSubtitleMolecule(
        modifier = modifier.padding(top = 60.dp),
        iconResourceId = iconResourceId,
        title = stringResource(id = titleTextId),
        subTitle = stringResource(id = subtitleTextId)
    )
}

@Composable
internal fun Content(flowState: FlowStep, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
        when (flowState) {
            FlowStep.Initial, FlowStep.Ready, FlowStep.Canceled, FlowStep.Completed -> Unit
            FlowStep.AwaitingOtherDeviceResponse -> ContentWaiting()
            is FlowStep.Verifying -> ContentVerifying(flowState)
        }
    }
}

@Composable
internal fun ContentWaiting(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun ContentVerifying(verificationFlowStep: FlowStep.Verifying, modifier: Modifier = Modifier) {
    // We want each row to have up to 4 emojis
    val rows = verificationFlowStep.emojiList.chunked(4)
    Column(modifier = modifier.fillMaxWidth()) {
        for ((rowIndex, emojis) in rows.withIndex()) {
            // Vertical spacing between rows
            if (rowIndex > 0) {
                Spacer(modifier = Modifier.height(40.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (emoji in emojis) {
                    EmojiItemView(emoji = emoji, modifier = Modifier.widthIn(max = 60.dp))
                }
            }
        }
    }
}

@Composable
internal fun EmojiItemView(emoji: VerificationEmoji, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = emoji.code,
            style = ElementTheme.typography.fontBodyMdRegular.copy(fontSize = 34.sp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            emoji.name,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BottomMenu(screenState: VerifySelfSessionState, goBack: () -> Unit) {
    val verificationViewState = screenState.verificationFlowStep
    val eventSink = screenState.eventSink

    val isVerifying = (verificationViewState as? FlowStep.Verifying)?.state is Async.Loading<Unit>
    val positiveButtonTitle = when (verificationViewState) {
        FlowStep.Initial -> R.string.screen_session_verification_positive_button_initial
        FlowStep.Canceled -> R.string.screen_session_verification_positive_button_canceled
        is FlowStep.Verifying -> {
            if (isVerifying) {
                R.string.screen_session_verification_positive_button_verifying_ongoing
            } else {
                R.string.screen_session_verification_they_match
            }
        }
        FlowStep.Ready -> R.string.screen_session_verification_positive_button_ready
        else -> null
    }
    val negativeButtonTitle = when (verificationViewState) {
        FlowStep.Initial -> CommonStrings.action_cancel
        FlowStep.Canceled -> CommonStrings.action_cancel
        is FlowStep.Verifying -> R.string.screen_session_verification_they_dont_match
        else -> null
    }
    val negativeButtonEnabled = !isVerifying

    val positiveButtonEvent = when (verificationViewState) {
        FlowStep.Initial -> VerifySelfSessionViewEvents.RequestVerification
        FlowStep.Ready -> VerifySelfSessionViewEvents.StartSasVerification
        is FlowStep.Verifying -> if (!isVerifying) VerifySelfSessionViewEvents.ConfirmVerification else null
        FlowStep.Canceled -> VerifySelfSessionViewEvents.Restart
        else -> null
    }

    val negativeButtonCallback: () -> Unit = when (verificationViewState) {
        is FlowStep.Verifying -> {
            { eventSink(VerifySelfSessionViewEvents.DeclineVerification) }
        }
        else -> goBack
    }

    ButtonColumnMolecule(
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        ButtonWithProgress(
            text = positiveButtonTitle?.let { stringResource(it) },
            showProgress = isVerifying,
            modifier = Modifier.fillMaxWidth(),
            onClick = { positiveButtonEvent?.let { eventSink(it) } }
        )
        if (negativeButtonTitle != null) {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = negativeButtonCallback,
                enabled = negativeButtonEnabled,
            ) {
                Text(
                    text = stringResource(negativeButtonTitle),
                    style = ElementTheme.typography.aliasButtonText,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun VerifySelfSessionViewLightPreview(@PreviewParameter(VerifySelfSessionStateProvider::class) state: VerifySelfSessionState) =
    ElementPreviewLight { ContentToPreview(state) }

@Preview
@Composable
internal fun VerifySelfSessionViewDarkPreview(@PreviewParameter(VerifySelfSessionStateProvider::class) state: VerifySelfSessionState) =
    ElementPreviewDark { ContentToPreview(state) }

@Composable
private fun ContentToPreview(state: VerifySelfSessionState) {
    VerifySelfSessionView(
        state = state,
        goBack = {},
    )
}
