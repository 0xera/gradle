/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.ide.visualstudio.internal
import org.gradle.nativebinaries.BuildType
import org.gradle.nativebinaries.Flavor
import org.gradle.nativebinaries.ProjectNativeExecutable
import org.gradle.nativebinaries.ProjectNativeLibrary
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal
import org.gradle.nativebinaries.internal.ProjectNativeExecutableBinaryInternal
import org.gradle.nativebinaries.internal.ProjectSharedLibraryBinaryInternal
import org.gradle.nativebinaries.internal.ProjectStaticLibraryBinaryInternal
import org.gradle.nativebinaries.platform.Architecture
import org.gradle.nativebinaries.platform.Platform
import org.gradle.nativebinaries.platform.internal.ArchitectureNotationParser
import org.gradle.nativebinaries.test.ProjectNativeTestSuite
import org.gradle.nativebinaries.test.internal.ProjectNativeTestSuiteBinaryInternal
import org.gradle.runtime.base.internal.BinaryNamingScheme
import spock.lang.Specification

class VisualStudioProjectMapperTest extends Specification {
    def mapper = new VisualStudioProjectMapper()

    def executable = Mock(ProjectNativeExecutable)
    def library = Mock(ProjectNativeLibrary)
    def namingScheme = Mock(BinaryNamingScheme)
    ProjectNativeExecutableBinaryInternal executableBinary

    def flavorOne = Mock(Flavor)
    def buildTypeOne = Mock(BuildType)
    def buildTypeTwo = Mock(BuildType)
    def platformOne = Mock(Platform)

    def setup() {
        executableBinary = createExecutableBinary("exeBinaryName", buildTypeOne, platformOne)
        executableBinary.namingScheme >> namingScheme

        executable.name >> "exeName"
        library.name >> "libName"

        flavorOne.name >> "flavorOne"
        buildTypeOne.name >> "buildTypeOne"
        buildTypeTwo.name >> "buildTypeTwo"
        platformOne.name >> "platformOne"
        platformOne.architecture >> arch("i386")
    }

    def "maps executable binary to visual studio project"() {
        when:
        executable.projectPath >> ":"
        namingScheme.variantDimensions >> []

        then:
        checkNames executableBinary, "exeNameExe", 'buildTypeOne', 'Win32'
    }

    def "maps library binary types to visual studio projects"() {
        when:
        def sharedLibraryBinary = libraryBinary(ProjectSharedLibraryBinaryInternal)
        def staticLibraryBinary = libraryBinary(ProjectStaticLibraryBinaryInternal)

        library.projectPath >> ":"
        namingScheme.variantDimensions >> []

        then:
        checkNames sharedLibraryBinary, "libNameDll", 'buildTypeOne', 'Win32'
        checkNames staticLibraryBinary, "libNameLib", 'buildTypeOne', 'Win32'
    }

    def "maps test binary to visual studio project"() {
        def testExecutable = Mock(ProjectNativeTestSuite)
        def binary = Mock(ProjectNativeTestSuiteBinaryInternal)

        when:
        testExecutable.name >> "testSuiteName"
        testExecutable.projectPath >> ":"
        binary.component >> testExecutable
        binary.buildType >> buildTypeOne
        binary.flavor >> flavorOne
        binary.targetPlatform >> platformOne
        binary.namingScheme >> namingScheme
        namingScheme.variantDimensions >> []

        then:
        checkNames binary, "testSuiteNameExe", 'buildTypeOne', 'Win32'
    }

    def "includes project path in visual studio project name"() {
        when:
        executable.projectPath >> ":subproject:name"

        and:
        namingScheme.variantDimensions >> []

        then:
        checkNames executableBinary, "subproject_name_exeNameExe", 'buildTypeOne', 'Win32'
    }

    def "uses single variant dimension for configuration name where not empty"() {
        when:
        executable.projectPath >> ":"
        namingScheme.variantDimensions >> ["flavorOne"]

        then:
        checkNames executableBinary, "exeNameExe", 'flavorOne', 'Win32'
    }

    def "includes variant dimensions in configuration where component has multiple dimensions"() {
        when:
        executable.projectPath >> ":"
        namingScheme.variantDimensions >> ["platformOne", "buildTypeOne", "flavorOne"]

        then:
        checkNames executableBinary, "exeNameExe", 'platformOneBuildTypeOneFlavorOne', 'Win32'
    }

    private def createExecutableBinary(String binaryName, def buildType, def platform) {
        def binary = Mock(ProjectNativeExecutableBinaryInternal)
        binary.name >> binaryName
        binary.component >> executable
        binary.buildType >> buildType
        binary.flavor >> flavorOne
        binary.targetPlatform >> platform
        return binary
    }

    private checkNames(def binary, def projectName, def configurationName, def platformName) {
        def names = mapper.mapToConfiguration(binary)
        assert names.project == projectName
        assert names.configuration == configurationName
        assert names.platform == platformName
        true
    }

    private static Architecture arch(String name) {
        return ArchitectureNotationParser.parser().parseNotation(name)
    }

    private libraryBinary(Class<? extends ProjectNativeBinaryInternal> type) {
        def binary = Mock(type)
        binary.component >> library
        binary.flavor >> flavorOne
        binary.targetPlatform >> platformOne
        binary.buildType >> buildTypeOne
        binary.namingScheme >> namingScheme
        return binary
    }
}
