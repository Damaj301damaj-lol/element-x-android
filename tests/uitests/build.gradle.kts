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

import extension.allFeaturesImpl
import extension.allLibrariesImpl
import extension.allServicesImpl

plugins {
    id("io.element.android-compose-library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "io.element.android.tests.uitests"
}

// Workaround: `kover` tasks somehow trigger the screenshot tests with a broken configuration, removing
// any previous test results and not creating new ones. This is a workaround to disable the screenshot tests
// when the `kover` tasks are detected.
tasks.withType<Test>() {
    if (project.gradle.startParameter.taskNames.any { it.contains("kover", ignoreCase = true) }) {
        println("WARNING: Kover task detected, disabling screenshot test task $name.")
        isEnabled = false
    }
}

dependencies {
    testImplementation(libs.test.junit)
    testImplementation(libs.test.parameter.injector)
    testImplementation(projects.libraries.designsystem)
    ksp(libs.showkase.processor)
    kspTest(libs.showkase.processor)

    implementation(libs.showkase)

    // TODO There is a Resources.NotFoundException maybe due to the mipmap, even if we have
    // `testOptions { unitTests.isIncludeAndroidResources = true }` in the app build.gradle.kts file
    // implementation(projects.app)
    implementation(projects.appnav)
    allLibrariesImpl()
    allServicesImpl()
    allFeaturesImpl(rootDir, logger)
}
