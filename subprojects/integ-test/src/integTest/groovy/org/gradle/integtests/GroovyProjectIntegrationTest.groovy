/*
 * Copyright 2012 the original author or authors.
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
package org.gradle.integtests

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Issue
import spock.lang.FailsWith

class GroovyProjectIntegrationTest extends AbstractIntegrationSpec {
    
    def handlesJavaSourceOnly() {
        given:
        buildFile << "apply plugin: 'groovy'"

        and:
        file("src/main/java/somepackage/SomeClass.java") << "public class SomeClass { }"
        file("settings.gradle") << "rootProject.name='javaOnly'"
        
        when:
        run "build"

        then:
        file("build/libs/javaOnly.jar").exists()
    }

    @Issue("http://issues.gradle.org/browse/GRADLE-2232")
    @FailsWith(RuntimeException)
    def "can extend GroovyTestCase"() {
        when:
        buildFile << """
            apply plugin: 'groovy'
            repositories {
                mavenCentral()
            }
            dependencies {
                groovy localGroovy()
                testCompile "junit:junit:4.10"
            }
        """
        
        and:
        file("src/test/groovy/Test.groovy") << """
            class SomethingElse extends GroovyTestCase {
                void testIt() {}
            }
        """

        then:
        succeeds "test"
    }

}
