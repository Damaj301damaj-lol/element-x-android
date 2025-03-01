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

package extension

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project
import org.gradle.api.logging.Logger
import java.io.File

private fun DependencyHandlerScope.implementation(dependency: Any) = dependencies.add("implementation", dependency)

private fun DependencyHandlerScope.androidTestImplementation(dependency: Any) = dependencies.add("androidTestImplementation", dependency)

private fun DependencyHandlerScope.debugImplementation(dependency: Any) = dependencies.add("debugImplementation", dependency)

/**
 * Dependencies used by all the modules
 */
fun DependencyHandlerScope.commonDependencies(libs: LibrariesForLibs) {
    implementation(libs.timber)
}

/**
 * Dependencies used by all the modules with composable items
 */
fun DependencyHandlerScope.composeDependencies(libs: LibrariesForLibs) {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.activity.compose)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(libs.showkase)
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
}

private fun DependencyHandlerScope.addImplementationProjects(
    directory: File,
    path: String,
    nameFilter: String,
    logger: Logger,
) {
    directory.listFiles().orEmpty().also { it.sort() }.forEach { file ->
        if (file.isDirectory) {
            val newPath = "$path:${file.name}"
            val buildFile = File(file, "build.gradle.kts")
            if (buildFile.exists() && file.name == nameFilter) {
                implementation(project(newPath))
                logger.lifecycle("Added implementation(project($newPath))")
            } else {
                addImplementationProjects(file, newPath, nameFilter, logger)
            }
        }
    }
}

fun DependencyHandlerScope.allLibrariesImpl() {
    implementation(project(":libraries:androidutils"))
    implementation(project(":libraries:deeplink"))
    implementation(project(":libraries:designsystem"))
    implementation(project(":libraries:matrix:impl"))
    implementation(project(":libraries:matrixui"))
    implementation(project(":libraries:network"))
    implementation(project(":libraries:core"))
    implementation(project(":libraries:eventformatter:impl"))
    implementation(project(":libraries:permissions:impl"))
    implementation(project(":libraries:push:impl"))
    implementation(project(":libraries:push:impl"))
    // Comment to not include firebase in the project
    implementation(project(":libraries:pushproviders:firebase"))
    // Comment to not include unified push in the project
    implementation(project(":libraries:pushproviders:unifiedpush"))
    implementation(project(":libraries:featureflag:impl"))
    implementation(project(":libraries:pushstore:impl"))
    implementation(project(":libraries:architecture"))
    implementation(project(":libraries:dateformatter:impl"))
    implementation(project(":libraries:di"))
    implementation(project(":libraries:session-storage:impl"))
    implementation(project(":libraries:mediapickers:impl"))
    implementation(project(":libraries:mediaupload:impl"))
    implementation(project(":libraries:usersearch:impl"))
    implementation(project(":libraries:textcomposer"))
}

fun DependencyHandlerScope.allServicesImpl() {
    // For analytics configuration, either use noop, or use the impl, with at least one analyticsproviders implementation
    // implementation(project(":services:analytics:noop"))
    implementation(project(":services:analytics:impl"))
    implementation(project(":services:analyticsproviders:posthog"))
    implementation(project(":services:analyticsproviders:sentry"))

    implementation(project(":services:apperror:impl"))
    implementation(project(":services:appnavstate:impl"))
    implementation(project(":services:toolbox:impl"))
}

fun DependencyHandlerScope.allFeaturesApi(rootDir: File, logger: Logger) {
    val featuresDir = File(rootDir, "features")
    addImplementationProjects(featuresDir, ":features", "api", logger)
}

fun DependencyHandlerScope.allFeaturesImpl(rootDir: File, logger: Logger) {
    val featuresDir = File(rootDir, "features")
    addImplementationProjects(featuresDir, ":features", "impl", logger)
}
