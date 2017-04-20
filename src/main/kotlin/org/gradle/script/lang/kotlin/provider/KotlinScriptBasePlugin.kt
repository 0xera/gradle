/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.script.lang.kotlin.provider

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.script.lang.kotlin.accessors.DisplayAccessors
import org.gradle.script.lang.kotlin.accessors.GenerateProjectSchema
import org.gradle.script.lang.kotlin.accessors.ProjectExtensionsBuildSrcConfigurationAction

class KotlinScriptBasePlugin : Plugin<Project> {
    override fun apply(project: Project): Unit =
        project.run {
            rootProject.plugins.apply(KotlinScriptRootPlugin::class.java)
            tasks.create("gskProjectAccessors", DisplayAccessors::class.java)
        }
}

class KotlinScriptRootPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit =
        project.run {
            tasks.create("gskGenerateAccessors", GenerateProjectSchema::class.java) {
                it.destinationFile = file(
                    "buildSrc/${ProjectExtensionsBuildSrcConfigurationAction.PROJECT_SCHEMA_RESOURCE_PATH}")
            }
        }
}
