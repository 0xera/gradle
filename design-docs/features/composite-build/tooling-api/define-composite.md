## Tooling client provides model for "composite" with one multi-project participant

### Overview

- This story provides an API for retrieving an aggregate model (single type from multiple projects) through the TAPI.
- With the existing `ProjectConnection` API, aggregation must be done on the client side or a model type must be a `HierarchicalElement` (only works for multi-project builds).
- It will only support retrieving models for `EclipseProject`.  Later stories add support for `ProjectPublications`, and eventually any model type should be able to be aggregated from a composite build.

### API

To support Eclipse import, only a constrained composite connection API is required.

    abstract class GradleConnector { // existing class
        static GradleConnection.Builder newGradleConnectionBuilder()
    }

    // See code in 'composite-build/src'

    // Usage:
    GradleBuild build = GradleConnector.newParticipant(new File ("path/to/root"))
        .useGradleDistribution("2.11").create(); //or URI or File or don't specify to use the wrapper
    GradleConnection connection = GradleConnector.newGradleConnectionBuilder()
        .addBuild(build)
        .build()

    // Using blocking call
    Set<EclipseProject> projects = connection.getModels(EclipseProject.class)
    for (EclipseProject project : projects) {
        // do something with EclipseProject model
    }

    // Using CompositeModelBuilder
    CompositeModelBuilder<Set<EclipseProject>> modelBuilder = connection.models(EclipseProject.class)
        //can set participant-specific arguments
        .setJavaHome(build, new File(...));
        .setJvmArguments(build, "-Xmx512m", ...)
        .withArguments(build, "-PmySpecialFeature", ...)
    Set<EclipseProject> projects = modelBuilder.get()

    // using result handler
    // or connection.getModels(EclipseProject.class, ...)
    modelBuilder.get(new ResultHandler<Set<EclipseProject>>() {
        @Override
        public void onComplete(Set<EclipseProject> result) {
            // handle complete result set
        }

        @Override
        public void onFailure(GradleConnectionException failure) {
            // handle failures
        }
    })

### Implementation notes

- Implement `GradleConnection` on top of existing Tooling API client
- Create `ProjectConnection` instance for the participant
- Delegate calls to the participant's `ProjectConnection`
    - Optimize for `EclipseProject`: open a single connection and traverse hierarchy
    - Fail for any other model type
- Gather all `EclipseProject`s into result Set
- After closing a `GradleConnection`, `GradleConnection` methods throw IllegalStateException (like `ProjectConnection.getModel`)
- `CompositeModelBuilder` is an extension of `ModelBuilder`, allowing to set per-participant arguments on top of arguments for the coordinator.
- All `CompositeModelBuilder` methods are delegates to the underlying `ProjectConnection`
- Validate participant projects are a "valid" composite before retrieving model
    - >1 participants
    - All using >= Gradle 1.0
    - Participants are not "overlapping" (subprojects of one another)
    - Participants are actually Gradle builds

### Test coverage

- Fail with `IllegalStateException` if no participants are added to the composite when connecting.
- Fail with `UnsupportedOperationException` if composite build is created with >1 participant when connecting.
- Fail with `IllegalStateException` after connecting to a `GradleConnection`, closing the connection and trying to retrieve a model.
- Errors from trying to retrieve models (getModels, et al) is propagated to caller.
- When retrieving anything other than `EclipseProject`, an `UnsupportedOperationException` is thrown.
- When retrieving `EclipseProject`:
    - a single ProjectConnection is used.
    - a single project returns a single `EclipseProject`
    - a multi-project build returns a `EclipseProject` for each Gradle project in a Set
- Fail if participant is not a Gradle build (what does this look like for existing integration tests?)
- After making a successful model request, on a subsequent model request:
    - Changing the set of sub-projects changes the number of `EclipseProject`s that are returned
    - Removing the project directory is causes a failure
    - Changing a single build into a multi-project build changes the number of `EclipseProject`s that are returned
- Errors from closing underlying ProjectConnection propagate to caller.
- The participants Gradle distribution is reflected in the `ProjectConnection`
- Participant project directory is used as the project directory for the `ProjectConnection`
- The java home, jvm arguments and build arguments for the participant are passed to the `ModelBuilder` of the participant
- Cross-version tests:
    - Fail if participants are <Gradle 1.0
    - Test retrieving `EclipseProject` from all supported Gradle versions

### Documentation

- Need to rework sample or add composite sample using new API.
- Add toolingApi sample with a single multi-project build. Demonstrate retrieving models from all projects.

### Open issues

- Provide way of detecting feature set of composite build?
- Enable validation of composite build -- better way than querying model multiple times?

## Tooling client provides model for composite containing multiple participants

### Overview

- Projects using < Gradle 1.8 do not support `ProjectPublications`
- Projects using < Gradle 2.5 do not support dependency substitution

### API

    class GradleCompositeException extends GradleConnectionException {
    }

    // Usage:
    GradleBuild participant1 = GradleConnector.newParticipant(new File ("root1")).create();
    GradleBuild participant2 = GradleConnector.newParticipant(new File ("root2")).useGradleDistribution("2.11").create();
    GradleConnection connection = GradleConnector.newGradleConnectionBuilder()
        .addBuild(participant1)
        .addBuild(participant2)
        .build()

### Implementation notes

- Client will provide connection information for multiple builds (root project)
- Methods will delegate to each `ProjectConnection` and aggregate results.
- Only a aggregate result will be returned (no partial results).
- Each participant in the composite will be used sequentially
- Overall operation fails on the first failure (no subsequent participants are queried).
- Implement "composite" ModelBuilder<Set<T>> implementation.
    - `models()` returns a composite `ModelBuilder<Set<T>>`
    - When `get()` is used, each participant's `ModelBuilder<T>` is configured and called.
    - `ResultHandler<Set<T>>.onComplete()` gets aggregated result.
    - `ResultHandler<Set<T>>.onFailure()` gets the first failure
- Order of participants added to a composite does not guarantee an order when operating on participants or returning results.  A composite with [A,B,C] will return the same results as a composite with [C,B,A], but results are not guaranteed to be in any particular order.

### Test coverage

- Including 'single-build' tests, except relaxing allowed # of participants.
- When composite build connection is closed, all `ProjectConnection`s are closed.
- When retrieving an `EclipseProject` with getModels(modelType), getModels(modelType, resultHandler), models(modelType)
    - with two 'single' projects, two `EclipseProject`s are returned.
    - with two multi-project builds, a `EclipseProject` is returned for every project in both builds.
- Fail if participant is a subproject of another participant.
- Check that a consumer can cancel an operation
- Check that retrieving a model fails on the first `ProjectConnection` failure
- Check that a handler receives a single completion or failure call when retrieving a model.
- After a successful model request, on a subsequent request:
    - Making one participant a subproject of another causes the request to fail
- if any participant throws an error, the overall operation fails with a `GradleCompositeException`

### Documentation

- Add toolingApi sample with multiple independent builds. Demonstrate retrieving models for all projects.

### Open issues

- Deferred most of `ModelBuilder` API by creating simpler `CompositeModelBuilder`
- Check that all `ModelBuilder` methods are forwarded to each underlying build's `ModelBuilder` when configuring a build specific `ModelBuilder`
- Make `GradleCompositeException` more useful?
