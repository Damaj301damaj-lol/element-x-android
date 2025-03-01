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

package io.element.android.libraries.designsystem.components.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.preview.ElementThemedPreview
import io.element.android.libraries.designsystem.preview.PreviewGroup
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.theme.ElementTheme

@Composable
fun MainActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = title,
) {
    val ripple = rememberRipple(bounded = false)
    val interactionSource = MutableInteractionSource()
    Column(
        modifier.clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            indication = ripple
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tintColor = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.secondary
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tintColor,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = tintColor,
        )
    }
}

@Preview(group = PreviewGroup.Buttons)
@Composable
internal fun MainActionButtonPreview() {
    ElementThemedPreview {
        ContentsToPreview()
    }
}

@Composable
private fun ContentsToPreview() {
    Row(Modifier.padding(10.dp)) {
        MainActionButton(title = "Share", icon = Icons.Outlined.Share, onClick = { })
        Spacer(modifier = Modifier.width(20.dp))
        MainActionButton(title = "Share", icon = Icons.Outlined.Share, onClick = { }, enabled = false)
    }
}
