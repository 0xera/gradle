/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.nativeplatform.test.cpp.plugins;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppPlatform;
import org.gradle.language.cpp.ProductionCppComponent;
import org.gradle.language.cpp.internal.DefaultCppBinary;
import org.gradle.language.cpp.internal.DefaultUsageContext;
import org.gradle.language.cpp.internal.NativeVariantIdentity;
import org.gradle.language.cpp.plugins.CppBasePlugin;
import org.gradle.language.internal.NativeComponentFactory;
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithLinkUsage;
import org.gradle.language.nativeplatform.internal.toolchains.ToolChainSelector;
import org.gradle.language.swift.tasks.UnexportMainSymbol;
import org.gradle.nativeplatform.Linkage;
import org.gradle.nativeplatform.MachineArchitecture;
import org.gradle.nativeplatform.OperatingSystemFamily;
import org.gradle.nativeplatform.TargetMachine;
import org.gradle.nativeplatform.TargetMachineFactory;
import org.gradle.nativeplatform.internal.DefaultTargetMachineFactory;
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform;
import org.gradle.nativeplatform.tasks.InstallExecutable;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;
import org.gradle.nativeplatform.test.cpp.internal.DefaultCppTestExecutable;
import org.gradle.nativeplatform.test.cpp.internal.DefaultCppTestSuite;
import org.gradle.nativeplatform.test.plugins.NativeTestingBasePlugin;
import org.gradle.nativeplatform.test.tasks.RunTestExecutable;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.gradle.language.cpp.CppBinary.DEBUGGABLE_ATTRIBUTE;
import static org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE;
import static org.gradle.language.nativeplatform.internal.Dimensions.createDimensionSuffix;

/**
 * A plugin that sets up the infrastructure for testing C++ binaries using a simple test executable.
 *
 * Gradle will create a {@link RunTestExecutable} task that relies on the exit code of the binary.
 *
 * @since 4.4
 */
@Incubating
public class CppUnitTestPlugin implements Plugin<ProjectInternal> {
    private final NativeComponentFactory componentFactory;
    private final ToolChainSelector toolChainSelector;
    private final ObjectFactory objectFactory;
    private final ImmutableAttributesFactory attributesFactory;
    private final TargetMachineFactory targetMachineFactory;

    @Inject
    public CppUnitTestPlugin(NativeComponentFactory componentFactory, ToolChainSelector toolChainSelector, ObjectFactory objectFactory, ImmutableAttributesFactory attributesFactory, TargetMachineFactory targetMachineFactory) {
        this.componentFactory = componentFactory;
        this.toolChainSelector = toolChainSelector;
        this.objectFactory = objectFactory;
        this.attributesFactory = attributesFactory;
        this.targetMachineFactory = targetMachineFactory;
    }

    @Override
    public void apply(final ProjectInternal project) {
        project.getPluginManager().apply(CppBasePlugin.class);
        project.getPluginManager().apply(NativeTestingBasePlugin.class);

        // Add the unit test and extension
        final DefaultCppTestSuite testComponent = componentFactory.newInstance(CppTestSuite.class, DefaultCppTestSuite.class, "test");
        project.getExtensions().add(CppTestSuite.class, "unitTest", testComponent);
        project.getComponents().add(testComponent);

        testComponent.getBaseName().set(project.getName() + "Test");

        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(final Project project) {
                final ProductionCppComponent mainComponent = project.getComponents().withType(ProductionCppComponent.class).findByName("main");

                Set<TargetMachine> targetMachines;
                if (mainComponent != null) {
                    testComponent.getTestedComponent().set(mainComponent);
                    targetMachines = setDefaultAndGetTargetMachineValues(testComponent.getTargetMachines(), mainComponent.getTargetMachines());
                } else {
                    targetMachines = setDefaultAndGetTargetMachineValues(testComponent.getTargetMachines(), null);
                }

                if (targetMachines.isEmpty()) {
                    throw new IllegalArgumentException("A target machine needs to be specified for the unit test.");
                }

                for (TargetMachine targetMachine : targetMachines) {
                    Usage runtimeUsage = objectFactory.named(Usage.class, Usage.NATIVE_RUNTIME);
                    Provider<String> group = project.provider(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return project.getGroup().toString();
                        }
                    });

                    Provider<String> version = project.provider(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return project.getVersion().toString();
                        }
                    });

                    AttributeContainer attributesDebug = attributesFactory.mutable();
                    attributesDebug.attribute(Usage.USAGE_ATTRIBUTE, runtimeUsage);
                    attributesDebug.attribute(DEBUGGABLE_ATTRIBUTE, true);
                    attributesDebug.attribute(OPTIMIZED_ATTRIBUTE, false);
                    attributesDebug.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, targetMachine.getArchitecture());
                    attributesDebug.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, targetMachine.getOperatingSystemFamily());

                    String operatingSystemSuffix = createDimensionSuffix(targetMachine.getOperatingSystemFamily(), targetMachines.stream().map(TargetMachine::getOperatingSystemFamily).collect(Collectors.toSet()));
                    String architectureSuffix = createDimensionSuffix(targetMachine.getArchitecture(), targetMachines.stream().map(TargetMachine::getArchitecture).collect(Collectors.toSet()));
                    String variantName = operatingSystemSuffix + architectureSuffix;

                    NativeVariantIdentity debugVariant = new NativeVariantIdentity("debug" + variantName, testComponent.getBaseName(), group, version, true, false, targetMachine,
                            null,
                            new DefaultUsageContext("debug" + variantName + "Runtime", runtimeUsage, attributesDebug));

                    if (DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName().equals(targetMachine.getOperatingSystemFamily().getName())) {
                        ToolChainSelector.Result<CppPlatform> result = toolChainSelector.select(CppPlatform.class, targetMachine);
                        CppTestExecutable testExecutable = testComponent.addExecutable(variantName, debugVariant, result.getTargetPlatform(), result.getToolChain(), result.getPlatformToolProvider());

                        // If we have a mainComponent set, we'll derive the main test binary from the main component development binary later,
                        // otherwise we set it to the executable that matches the current architecture.
                        if (mainComponent == null && DefaultNativePlatform.getCurrentArchitecture().equals(result.getTargetPlatform().getArchitecture())) {
                            testComponent.getTestBinary().set(testExecutable);
                        }

                        // TODO: Publishing for test executable?
                    }
                }

                final TaskContainer tasks = project.getTasks();
                testComponent.getBinaries().whenElementKnown(DefaultCppTestExecutable.class, new Action<DefaultCppTestExecutable>() {
                    @Override
                    public void execute(final DefaultCppTestExecutable testExecutable) {
                        if (mainComponent != null) {
                            mainComponent.getBinaries().whenElementFinalized(new Action<CppBinary>() {
                                @Override
                                public void execute(final CppBinary testedBinary) {
                                    if (!isTestedBinary(testExecutable, testedBinary)) {
                                        return;
                                    }

                                    // TODO - move this to a base plugin
                                    // Setup the dependency on the main binary
                                    // This should all be replaced by a single dependency that points at some "testable" variants of the main binary

                                    // Inherit implementation dependencies
                                    testExecutable.getImplementationDependencies().extendsFrom(((DefaultCppBinary) testedBinary).getImplementationDependencies());

                                    // Configure test binary to link against tested component compiled objects
                                    ConfigurableFileCollection testableObjects = project.files();
                                    if (mainComponent instanceof CppApplication) {
                                        TaskProvider<UnexportMainSymbol> unexportMainSymbol = tasks.register(testExecutable.getNames().getTaskName("relocateMainFor"), UnexportMainSymbol.class, new Action<UnexportMainSymbol>() {
                                            @Override
                                            public void execute(UnexportMainSymbol unexportMainSymbol) {
                                                unexportMainSymbol.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("obj/main/for-test"));
                                                unexportMainSymbol.getObjects().from(testedBinary.getObjects());
                                            }
                                        });
                                        // TODO: builtBy unnecessary?
                                        testableObjects.builtBy(unexportMainSymbol);
                                        testableObjects.from(unexportMainSymbol.map(new Transformer<FileCollection, UnexportMainSymbol>() {
                                            @Override
                                            public FileCollection transform(UnexportMainSymbol unexportMainSymbol) {
                                                return unexportMainSymbol.getRelocatedObjects();
                                            }
                                        }));
                                    } else {
                                        testableObjects.from(testedBinary.getObjects());
                                    }
                                    Dependency linkDependency = project.getDependencies().create(testableObjects);
                                    testExecutable.getLinkConfiguration().getDependencies().add(linkDependency);

                                    // Set this as the main test binary if it tests the main development binary
                                    if (testedBinary == mainComponent.getDevelopmentBinary().get()) {
                                        testComponent.getTestBinary().set(testExecutable);
                                    }
                                }
                            });
                        }

                        // TODO: Replace with native test task
                        final TaskProvider<RunTestExecutable> testTask = tasks.register(testExecutable.getNames().getTaskName("run"), RunTestExecutable.class, new Action<RunTestExecutable>() {
                            @Override
                            public void execute(RunTestExecutable testTask) {
                                testTask.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
                                testTask.setDescription("Executes C++ unit tests.");

                                final InstallExecutable installTask = testExecutable.getInstallTask().get();
                                testTask.onlyIf(new Spec<Task>() {
                                    @Override
                                    public boolean isSatisfiedBy(Task element) {
                                        return testExecutable.getInstallDirectory().get().getAsFile().exists();
                                    }
                                });
                                testTask.getInputs().dir(testExecutable.getInstallDirectory());
                                testTask.setExecutable(installTask.getRunScriptFile().get().getAsFile());
                                testTask.dependsOn(testExecutable.getInstallDirectory());
                                // TODO: Honor changes to build directory
                                testTask.setOutputDir(project.getLayout().getBuildDirectory().dir("test-results/" + testExecutable.getNames().getDirName()).get().getAsFile());
                            }
                        });
                        testExecutable.getRunTask().set(testTask);
                    }
                });

                testComponent.getBinaries().realizeNow();
            }
        });
    }

    private boolean isTestedBinary(DefaultCppTestExecutable testExecutable, CppBinary testedBinary) {
        // TODO: Make this more intelligent by matching the attributes of the runtime usage on the variant identities
        return testedBinary.getTargetPlatform().getOperatingSystemFamily().getName() == testExecutable.getTargetPlatform().getOperatingSystemFamily().getName()
                && testedBinary.getTargetPlatform().getArchitecture().getName() == testExecutable.getTargetPlatform().getArchitecture().getName()
                && !testedBinary.isOptimized()
                && !(isStaticLibrary(testedBinary));
    }

    private boolean isStaticLibrary(CppBinary testedBinary) {
        return testedBinary instanceof ConfigurableComponentWithLinkUsage && ((ConfigurableComponentWithLinkUsage)testedBinary).getLinkage() == Linkage.STATIC;
    }

    private Set<TargetMachine> setDefaultAndGetTargetMachineValues(SetProperty<TargetMachine> testTargetMachines, @Nullable SetProperty<TargetMachine> mainTargetMachines) {
        if (mainTargetMachines != null && mainTargetMachines.isPresent()) {
            if (!testTargetMachines.isPresent()) {
                testTargetMachines.set(mainTargetMachines);
            } else {
                Set<TargetMachine> mainComponentTargetMachines = mainTargetMachines.get();
                for (TargetMachine machine : testTargetMachines.get()) {
                    if (!mainComponentTargetMachines.contains(machine)) {
                        throw new IllegalArgumentException("The target machine " + machine.toString() + " was specified for the unit test, but this target machine was not specified on the main component.");
                    }
                }
            }
        } else {
            if (!testTargetMachines.isPresent()) {
                testTargetMachines.empty().add(((DefaultTargetMachineFactory)targetMachineFactory).host());
            }
        }

        testTargetMachines.finalizeValue();
        return testTargetMachines.get();
    }
}
