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

package org.gradle.tooling.internal.provider.runner;

import com.google.gson.Gson;
import org.gradle.api.NonNullApi;
import org.gradle.api.problems.Problem;
import org.gradle.api.problems.ProblemCategory;
import org.gradle.api.problems.Severity;
import org.gradle.api.problems.internal.DefaultProblemProgressDetails;
import org.gradle.internal.build.event.types.DefaultAdditionalData;
import org.gradle.internal.build.event.types.DefaultDetails;
import org.gradle.internal.build.event.types.DefaultLabel;
import org.gradle.internal.build.event.types.DefaultProblemCategory;
import org.gradle.internal.build.event.types.DefaultProblemDescriptor;
import org.gradle.internal.build.event.types.DefaultProblemDetails;
import org.gradle.internal.build.event.types.DefaultProblemEvent;
import org.gradle.internal.build.event.types.DefaultSeverity;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationIdFactory;
import org.gradle.internal.operations.BuildOperationListener;
import org.gradle.internal.operations.OperationFinishEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationProgressEvent;
import org.gradle.tooling.internal.protocol.problem.InternalAdditionalData;
import org.gradle.tooling.internal.protocol.problem.InternalDetails;
import org.gradle.tooling.internal.protocol.problem.InternalLabel;
import org.gradle.tooling.internal.protocol.problem.InternalProblemCategory;
import org.gradle.tooling.internal.protocol.problem.InternalSeverity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Collectors;

@NonNullApi
public class ProblemsProgressEventConsumer extends ClientForwardingBuildOperationListener implements BuildOperationListener {
    private final BuildOperationIdFactory idFactory;

    public ProblemsProgressEventConsumer(ProgressEventConsumer progressEventConsumer, BuildOperationIdFactory idFactory) {
        super(progressEventConsumer);
        this.idFactory = idFactory;
    }

    @Override
    public void progress(OperationIdentifier buildOperationId, OperationProgressEvent progressEvent) {
        Object details = progressEvent.getDetails();
        if (details instanceof DefaultProblemProgressDetails) {
            Problem problem = ((DefaultProblemProgressDetails) details).getProblem();
            eventConsumer.progress(
                new DefaultProblemEvent(
                    new DefaultProblemDescriptor(
                        new OperationIdentifier(
                            idFactory.nextId()
                        ),
                        buildOperationId),
                    new DefaultProblemDetails(
                        new Gson().toJson(problem),
                        mapCategory(problem.getProblemCategory()),
                        mapLabel(problem.getLabel()),
                        mapDetails(problem.getDetails()),
                        mapSeverity(problem.getSeverity()),
                        mapEntries(problem.getAdditionalData())
                    )
                )
            );
        }
    }

    private static InternalProblemCategory mapCategory(ProblemCategory category) {
        return new DefaultProblemCategory(category.getNamespace(), category.getCategory(), category.getSubCategories());
    }

    private static InternalLabel mapLabel(String label) {
        return new DefaultLabel(label);
    }

    private static @Nullable InternalDetails mapDetails(@Nullable String details) {
        return details == null ? null : new DefaultDetails(details);
    }

    private static InternalSeverity mapSeverity(Severity severity) {
        // TODO we could probably send the same instance
        switch (severity) {
            case ADVICE: return new DefaultSeverity(0);
            case WARNING: return new DefaultSeverity(1);
            case ERROR: return new DefaultSeverity(2);
            default: return new DefaultSeverity(3); // should not happen
        }
    }

    private static InternalAdditionalData mapEntries(Map<String, Object> additionalData) {
        return new DefaultAdditionalData(
            additionalData.entrySet().stream().filter(entry -> isSupportedType(entry.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private static boolean isSupportedType(Object type) {
        return type instanceof String || type instanceof Integer;
    }

    @Override
    public void finished(BuildOperationDescriptor buildOperation, OperationFinishEvent result) {
        super.finished(buildOperation, result);
    }

}
