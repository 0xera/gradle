/*
 * Copyright 2018 the original author or authors.
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

package plugins

import org.gradle.api.Named
import org.gradle.language.jvm.tasks.ProcessResources


class KotlinDslPlugin(private val name: String) : Named {
    override fun getName() = name
    lateinit var displayName: String
    lateinit var id: String
    lateinit var implementationClass: String
}


val ProcessResources.futurePluginVersionsFile
    get() = destinationDir.resolve("future-plugin-versions.properties")
