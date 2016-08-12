/*
 * Copyright 2016 the original author or authors.
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

import org.gradle.script.lang.kotlin.loggerFor

import org.gradle.configuration.ScriptPlugin
import org.gradle.configuration.ScriptPluginFactory

import org.gradle.api.initialization.dsl.ScriptHandler

import org.gradle.api.internal.ClassPathRegistry
import org.gradle.api.internal.initialization.ClassLoaderScope
import org.gradle.api.internal.initialization.ScriptHandlerInternal

import org.gradle.groovy.scripts.ScriptSource

class KotlinScriptPluginFactory(val classPathRegistry: ClassPathRegistry) : ScriptPluginFactory {

    private val logger = loggerFor<KotlinScriptPluginFactory>()

    override fun create(scriptSource: ScriptSource, scriptHandler: ScriptHandler,
                        targetScope: ClassLoaderScope, baseScope: ClassLoaderScope,
                        topLevelScript: Boolean): ScriptPlugin =
        KotlinBuildScriptCompiler(
            classPathRegistry, scriptSource, topLevelScript, scriptHandler as ScriptHandlerInternal,
            targetScope, baseScope, logger).run {

            val script = if (topLevelScript && inClassPathMode()) compileForClassPath() else compile()
            KotlinScriptPlugin(scriptSource, script)
        }

    private fun inClassPathMode() =
        System.getProperty(modeSystemPropertyName) == classPathMode

    companion object {
        val modeSystemPropertyName = "org.gradle.script.lang.kotlin.provider.mode"
        val classPathMode = "classpath"
    }
}
