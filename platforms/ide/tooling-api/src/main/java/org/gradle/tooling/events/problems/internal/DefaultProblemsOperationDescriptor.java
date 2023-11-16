/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.tooling.events.problems.internal;

import org.gradle.api.NonNullApi;
import org.gradle.tooling.events.OperationDescriptor;
import org.gradle.tooling.events.internal.DefaultOperationDescriptor;
import org.gradle.tooling.events.problems.AdditionalData;
import org.gradle.tooling.events.problems.Details;
import org.gradle.tooling.events.problems.Label;
import org.gradle.tooling.events.problems.ProblemCategory;
import org.gradle.tooling.events.problems.ProblemDescriptor;
import org.gradle.tooling.events.problems.Severity;
import org.gradle.tooling.internal.protocol.events.InternalOperationDescriptor;

import javax.annotation.Nullable;

@NonNullApi
public class DefaultProblemsOperationDescriptor extends DefaultOperationDescriptor implements ProblemDescriptor {
    private final String json;
    private final ProblemCategory category;
    private final Label label;
    private final Details details;
    private final Severity severity;
    private final AdditionalData additionalData;
    private final Throwable throwable;

    public DefaultProblemsOperationDescriptor(
        InternalOperationDescriptor internalDescriptor,
        OperationDescriptor parent,
        String json,
        ProblemCategory category,
        Label label,
        @Nullable Details details,
        Severity severity,
        AdditionalData additionalData,
        Throwable throwable
    ) {
        super(internalDescriptor, parent);
        this.json = json;
        this.category = category;
        this.label = label;
        this.details = details;
        this.severity = severity;
        this.additionalData = additionalData;
        this.throwable = throwable;
    }
    public ProblemCategory getCategory() {
        return category;
    }

    @Override
    public Label getLabel() {
        return label;
    }

    @Override
    public Details getDetails() {
        return details;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public AdditionalData getAdditionalData() {
        return additionalData;
    }
    @Override
    public String getJson() {
        return json;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }
}
