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

package io.element.android.features.location.impl.show

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import io.element.android.features.location.api.internal.rememberTileStyleUrl
import io.element.android.features.location.impl.MapDefaults
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.FloatingActionButton
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.maplibre.compose.CameraMode
import io.element.android.libraries.maplibre.compose.CameraMoveStartedReason
import io.element.android.libraries.maplibre.compose.IconAnchor
import io.element.android.libraries.maplibre.compose.MapboxMap
import io.element.android.libraries.maplibre.compose.Symbol
import io.element.android.libraries.maplibre.compose.rememberCameraPositionState
import io.element.android.libraries.maplibre.compose.rememberSymbolState
import io.element.android.libraries.theme.ElementTheme
import io.element.android.libraries.theme.compound.generated.TypographyTokens
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.toImmutableMap
import io.element.android.libraries.designsystem.R as DesignSystemR

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShowLocationView(
    state: ShowLocationState,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(LatLng(state.location.lat, state.location.lon))
            .zoom(MapDefaults.DEFAULT_ZOOM)
            .build()
    }

    LaunchedEffect(state.isTrackMyLocation) {
        when (state.isTrackMyLocation) {
            false -> cameraPositionState.cameraMode = CameraMode.NONE
            true -> {
                cameraPositionState.position = CameraPosition.Builder()
                    .zoom(MapDefaults.DEFAULT_ZOOM)
                    .build()
                cameraPositionState.cameraMode = CameraMode.TRACKING
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            state.eventSink(ShowLocationEvents.TrackMyLocation(false))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(CommonStrings.screen_view_location_title),
                        style = ElementTheme.typography.aliasScreenTitle,
                    )
                },
                navigationIcon = {
                    BackButton(onClick = onBackPressed)
                },
                actions = {
                    IconButton(onClick = { state.eventSink(ShowLocationEvents.Share) }) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = stringResource(CommonStrings.action_share))
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.hasLocationPermission) {
                FloatingActionButton(
                    onClick = { state.eventSink(ShowLocationEvents.TrackMyLocation(true)) },
                ) {
                    when (state.isTrackMyLocation) {
                        false -> Icon(imageVector = Icons.Default.LocationSearching, contentDescription = null)
                        true -> Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .fillMaxSize(),
        ) {
            state.description?.let {
                Text(
                    text = it,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TypographyTokens.fontBodyMdRegular,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                )
            }

            MapboxMap(
                styleUri = rememberTileStyleUrl(),
                modifier = Modifier.fillMaxSize(),
                images = mapOf(PIN_ID to DesignSystemR.drawable.pin).toImmutableMap(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapDefaults.uiSettings,
                symbolManagerSettings = MapDefaults.symbolManagerSettings,
                locationSettings = MapDefaults.locationSettings.copy(
                    locationEnabled = state.hasLocationPermission,
                ),
            ) {
                Symbol(
                    iconId = PIN_ID,
                    state = rememberSymbolState(
                        position = LatLng(state.location.lat, state.location.lon)
                    ),
                    iconAnchor = IconAnchor.BOTTOM,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun ShowLocationViewLightPreview(@PreviewParameter(ShowLocationStateProvider::class) state: ShowLocationState) =
    ElementPreviewLight { ContentToPreview(state) }

@Preview
@Composable
internal fun ShowLocationViewDarkPreview(@PreviewParameter(ShowLocationStateProvider::class) state: ShowLocationState) =
    ElementPreviewDark { ContentToPreview(state) }

@Composable
private fun ContentToPreview(state: ShowLocationState) {
    ShowLocationView(
        state = state,
        onBackPressed = {},
    )
}

private const val PIN_ID = "pin"

