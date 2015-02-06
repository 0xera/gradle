/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.play.integtest

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.jvm.TestJvmComponent
import org.gradle.language.scala.fixtures.TestScalaComponent
import org.gradle.play.integtest.fixtures.app.BasicPlayApp
import org.gradle.play.integtest.fixtures.app.PlayApp
import org.gradle.test.fixtures.archive.JarTestFixture

class MixedPlayAndScalaLangProjectIntegrationTest extends AbstractIntegrationSpec {
    TestJvmComponent scalaApp = new TestScalaComponent()
    PlayApp playApp = new BasicPlayApp()

    def setup() {
        playApp.writeSources(file("."))
        scalaApp.writeSources(file("src/scalaLib"))
        scalaApp.writeResources(file("src/scalaLib/resources"))
        settingsFile.text = "rootProject.name = 'mixedScalaAndPlay'"
        buildFile.text = """
        plugins {
            id 'jvm-component'
            id '${scalaApp.languageName}-lang'
            id 'play'
        }
        repositories{
            mavenCentral()
            jcenter()
            maven{
                name = "typesafe-maven-release"
                url = "https://repo.typesafe.com/typesafe/maven-releases"
            }
        }

        model {
            components {
                scalaLib(JvmLibrarySpec)
            }
        }
"""
    }

    def "assemble builds jvm component and play component binaries"() {
        when:
        succeeds("assemble")
        then:
        executedAndNotSkipped(":createPlayBinaryAssetsJar", ":routesCompilePlayBinary", ":twirlCompileTwirlTemplatesPlayBinary", ":scalaCompilePlayBinary", ":createPlayBinaryJar", ":playBinary",
                ":compileScalaLibJarScalaLibScala", ":processScalaLibJarScalaLibResources", ":createScalaLibJar", ":scalaLibJar", ":assemble")
        and:
        file("build/classes/scalaLibJar").assertHasDescendants(scalaApp.expectedOutputs*.fullPath as String[])
        new JarTestFixture(file("build/jars/scalaLibJar/scalaLib.jar")).hasDescendants(scalaApp.expectedOutputs*.fullPath as String[])
        file("build/playBinary/lib/mixedScalaAndPlay.jar").exists()
        file("build/playBinary/lib/mixedScalaAndPlay-assets.jar").exists()
    }
}
