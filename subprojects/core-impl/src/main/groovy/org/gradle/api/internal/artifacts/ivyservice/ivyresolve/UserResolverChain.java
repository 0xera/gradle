/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.ivyresolve;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.latest.ArtifactInfo;
import org.apache.ivy.plugins.latest.ComparatorLatestStrategy;
import org.apache.ivy.plugins.resolver.ResolverSettings;
import org.apache.ivy.util.StringUtils;
import org.gradle.api.internal.artifacts.ivyservice.DependencyToModuleResolver;
import org.gradle.api.internal.artifacts.ivyservice.ModuleVersionIdResolveResult;
import org.gradle.api.internal.artifacts.ivyservice.ModuleVersionResolveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UserResolverChain implements DependencyToModuleResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserResolverChain.class);

    private final List<ModuleVersionRepository> moduleVersionRepositories = new ArrayList<ModuleVersionRepository>();
    private ResolverSettings settings;

    public void setSettings(ResolverSettings settings) {
        this.settings = settings;
    }

    public void add(ModuleVersionRepository repository) {
        moduleVersionRepositories.add(repository);
    }

    public ModuleVersionIdResolveResult resolve(DependencyDescriptor dependencyDescriptor) {
        final ModuleResolution latestResolved = findLatestModule(dependencyDescriptor);
        if (latestResolved != null) {
            final ModuleVersionDescriptor downloadedModule = latestResolved.module;
            LOGGER.debug("Found module '{}' using resolver '{}'", downloadedModule.getId(), latestResolved.repository);
            return new ModuleVersionIdResolveResult() {
                public ModuleRevisionId getId() throws ModuleVersionResolveException {
                    return downloadedModule.getId();
                }

                public ModuleDescriptor getDescriptor() throws ModuleVersionResolveException {
                    return downloadedModule.getDescriptor();
                }

                public File getArtifact(Artifact artifact) throws ArtifactResolveException {
                    LOGGER.debug("Attempting to download {} using resolver {}", artifact, latestResolved.repository);
                    return latestResolved.repository.download(artifact);
                }
            };
        }
        return null;
    }

    private ModuleResolution findLatestModule(DependencyDescriptor dependencyDescriptor) {
        List<RuntimeException> errors = new ArrayList<RuntimeException>();
        boolean isStaticVersion = !settings.getVersionMatcher().isDynamic(dependencyDescriptor.getDependencyRevisionId());
        
        ModuleResolution best = null;
        for (ModuleVersionRepository repository : moduleVersionRepositories) {
            try {
                ModuleVersionDescriptor module = repository.getDependency(dependencyDescriptor);
                if (module != null) {
                    ModuleResolution moduleResolution = new ModuleResolution(repository, module);
                    if (isStaticVersion && !moduleResolution.isGeneratedModuleDescriptor()) {
                        return moduleResolution;
                    }
                    best = chooseBest(best, moduleResolution);
                }
            } catch (RuntimeException e) {
                errors.add(e);
            }
        }

        if (best == null && !errors.isEmpty()) {
            throwResolutionFailure(errors);
        }
        return best;
    }

    private ModuleResolution chooseBest(ModuleResolution one, ModuleResolution two) {
        if (one == null || two == null) {
            return two == null ? one : two;
        }
        if (one.module == null || two.module == null) {
            return two.module == null ? one : two;
        }

        ComparatorLatestStrategy latestStrategy = (ComparatorLatestStrategy) settings.getDefaultLatestStrategy();
        Comparator<ArtifactInfo> comparator = latestStrategy.getComparator();
        int comparison = comparator.compare(one, two);

        if (comparison == 0) {
            if (one.isGeneratedModuleDescriptor() && !two.isGeneratedModuleDescriptor()) {
                return two;
            }
            return one;
        }

        return comparison < 0 ? two : one;
    }

    private void throwResolutionFailure(List<RuntimeException> errors) {
        if (errors.size() == 1) {
            throw errors.get(0);
        } else {
            StringBuilder err = new StringBuilder();
            for (Exception ex : errors) {
                err.append("\t").append(StringUtils.getErrorMessage(ex)).append("\n");
            }
            err.setLength(err.length() - 1);
            throw new RuntimeException("several problems occurred while resolving :\n" + err);
        }
    }

    private class ModuleResolution implements ArtifactInfo {
        public final ModuleVersionRepository repository;
        public final ModuleVersionDescriptor module;

        public ModuleResolution(ModuleVersionRepository repository, ModuleVersionDescriptor module) {
            this.repository = repository;
            this.module = module;
        }

        public boolean isGeneratedModuleDescriptor() {
            if (module == null) {
                throw new IllegalStateException();
            }
            return module.getDescriptor().isDefault();
        }

        public long getLastModified() {
            return module.getDescriptor().getResolvedPublicationDate().getTime();
        }

        public String getRevision() {
            return module.getId().getRevision();
        }
    }
}
