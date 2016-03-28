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

package org.gradle.tooling.internal.connection;

import org.gradle.api.Nullable;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.connection.BuildIdentity;
import org.gradle.tooling.connection.FailureModelResult;
import org.gradle.tooling.connection.ProjectIdentity;

public class DefaultFailureModelResult<T> implements FailureModelResult<T> {
    private final GradleConnectionException failure;
    private final BuildIdentity buildIdentity;
    private final ProjectIdentity projectIdentity;

    public DefaultFailureModelResult(BuildIdentity buildIdentity, GradleConnectionException failure) {
        this.failure = failure;
        this.buildIdentity = buildIdentity;
        this.projectIdentity = null;
    }

    public DefaultFailureModelResult(ProjectIdentity projectIdentity, GradleConnectionException failure) {
        this.failure = failure;
        this.buildIdentity = projectIdentity.getBuild();
        this.projectIdentity = projectIdentity;
    }

    @Override
    public T getModel() {
        throw failure;
    }

    @Override
    public GradleConnectionException getFailure() {
        return failure;
    }

    @Override
    public BuildIdentity getBuildIdentity() {
        return buildIdentity;
    }

    @Nullable
    @Override
    public ProjectIdentity getProjectIdentity() {
        return projectIdentity;
    }

    @Override
    public String toString() {
        return String.format("result={ failure=%s }", failure.getMessage());
    }
}
