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

package org.gradle.integtests.tooling

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.HttpServer
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion
import org.junit.Rule

import static org.gradle.integtests.fixtures.UserAgentMatcher.matchesNameAndVersion
import static org.hamcrest.Matchers.containsString
import static org.junit.Assert.assertThat

class ToolingApiRemoteIntegrationTest extends AbstractIntegrationSpec {
    @Rule HttpServer server = new HttpServer()
    GradleConnector connector = GradleConnector.newConnector();

    void setup() {
        server.start()
        settingsFile << "";
        buildFile << "task hello << { println hello }"
    }

    public void "downloads distribution with valid useragent information"() {
        assert distribution.binDistribution.exists(): "bin distribution must exist to run this test, you need to run the :binZip task"
        expect:
        server.allowGetOrHead("/dist", distribution.binDistribution)
        server.expectUserAgent(matchesNameAndVersion("Gradle Tooling API", GradleVersion.current().getVersion()))
        when:
        URI gradleDistributionURI = URI.create("http://localhost:${server.port}/dist")

        and:
        def connect = connector.useDistribution(gradleDistributionURI).forProjectDirectory(getTestDir()).connect();
        BuildLauncher launcher = connect.newBuild().forTasks("hello")
        ByteArrayOutputStream buildOutput = new ByteArrayOutputStream()
        launcher.standardOutput = buildOutput;
        launcher.run()

        then:
        assertThat(new String(buildOutput.toByteArray()), containsString('hello'))
    }

}
