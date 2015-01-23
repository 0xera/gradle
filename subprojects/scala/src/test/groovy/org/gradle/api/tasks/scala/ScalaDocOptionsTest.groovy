/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.tasks.scala

import spock.lang.Unroll

public class ScalaDocOptionsTest extends BaseScalaOptionTest<ScalaDocOptions> {

    @Override
    ScalaDocOptions newTestObject() {
        return new ScalaDocOptions()
    }

    @Unroll("OnOff #fixture.fieldName maps to #fixture.antProperty with a default value of #fixture.defaultValue")
    def "onOff values"(Map<String, String> fixture) {
        given:
        assert testObject."${fixture.fieldName}" == fixture.defaultValue

        when:
        testObject."${fixture.fieldName}" = true
        then:
        value(fixture.antProperty) == 'on'

        when:
        testObject."${fixture.fieldName}" = false
        then:
        value(fixture.antProperty) == 'off'

        where:
        fixture << [
                [fieldName: 'deprecation', antProperty: 'deprecation', defaultValue: true],
                [fieldName: 'unchecked', antProperty: 'unchecked', defaultValue: true]
        ]
    }

    @Unroll("String #fixture.fieldName maps to #fixture.antProperty with a default value of #fixture.defaultValue")
    def "simple string values"(Map<String, String> fixture) {
        given:
        assert testObject."${fixture.fieldName}" == fixture.defaultValue
        if (fixture.defaultValue == null) {
            assert doesNotContain(fixture.antProperty)
        } else {
            assert value(fixture.antProperty) == fixture.defaultValue
        }
        when:
        testObject."${fixture.fieldName}" = fixture.testValue
        then:
        value(fixture.antProperty) == fixture.testValue
        where:
        fixture << [
                [fieldName: 'windowTitle', antProperty: 'windowTitle', defaultValue: null, testValue: 'title-value'],
                [fieldName: 'docTitle', antProperty: 'docTitle', defaultValue: null, testValue: 'doc-title-value'],
                [fieldName: 'header', antProperty: 'header', defaultValue: null, testValue: 'header-value'],
                [fieldName: 'footer', antProperty: 'footer', defaultValue: null, testValue: 'footer-value'],
                [fieldName: 'top', antProperty: 'top', defaultValue: null, testValue: 'top-value'],
                [fieldName: 'bottom', antProperty: 'bottom', defaultValue: null, testValue: 'bottom-value'],
                [fieldName: 'footer', antProperty: 'footer', defaultValue: null, testValue: 'footer-value']
        ]
    }

    def testOptionMapContainsStyleSheetIfSpecified() {
        String antProperty = 'styleSheet'
        given:
        assert testObject.styleSheet == null
        assert doesNotContain(antProperty)
        when:
        File file = new File('abc')
        testObject.styleSheet = file
        then:
        value(antProperty) == file
    }

    @Unroll("List #fixture.fieldName with value #fixture.args maps to #fixture.antProperty with value #fixture.expected")
    def "addParams"(Map<String, Object> fixture) {
        given:
        assert testObject."${fixture.fieldName}" == null
        assert value(fixture.antProperty as String) == null

        when:
        testObject."${fixture.fieldName}" = fixture.args as List<String>
        then:
        value(fixture.antProperty as String) == fixture.expected

        where:
        fixture << [
                [fieldName: 'additionalParameters', antProperty: 'addparams', args: ['-opt1', '-opt2'], expected: '-opt1 -opt2'],
                [fieldName: 'additionalParameters', antProperty: 'addparams', args: ['arg with spaces'], expected: '\'arg with spaces\''],
                [fieldName: 'additionalParameters', antProperty: 'addparams', args: ['arg with \' and spaces'], expected: '\'arg with \\\' and spaces\''],
                [fieldName: 'additionalParameters', antProperty: 'addparams', args: ['\'arg with spaces\''], expected: '\'arg with spaces\''],
                [fieldName: 'additionalParameters', antProperty: 'addparams', args: ['"arg with spaces"'], expected: '"arg with spaces"'],
        ]
    }


}