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

package org.gradle.integtests.tooling.fixture

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.test.fixtures.file.TestFile
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.eclipse.EclipseProject
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

@ToolingApiVersion('>=1.2')
@TargetGradleVersion('>=1.0-milestone-8')
abstract class ToolingApiSpecification extends AbstractToolingApiSpecification {

    public <T> T withConnection(@DelegatesTo(ProjectConnection) @ClosureParams(value = SimpleType, options = ["org.gradle.tooling.ProjectConnection"]) Closure<T> cl) {
        toolingApi.withConnection(cl)
    }

    public <T> T withConnection(GradleConnector connector, @DelegatesTo(ProjectConnection) @ClosureParams(value = SimpleType, options = ["org.gradle.tooling.ProjectConnection"]) Closure<T> cl) {
        toolingApi.withConnection(connector, cl)
    }

    public ConfigurableOperation withModel(Class modelType, Closure cl = {}) {
        withConnection {
            def model = it.model(modelType)
            cl(model)
            new ConfigurableOperation(model).buildModel()
        }
    }

    public ConfigurableOperation withBuild(Closure cl = {}) {
        withConnection {
            def build = it.newBuild()
            cl(build)
            def out = new ConfigurableOperation(build)
            build.run()
            out
        }
    }

    TestFile getProjectDir() {
        temporaryFolder.testDirectory
    }

    TestFile getBuildFile() {
        file("build.gradle")
    }

    TestFile getSettingsFile() {
        file("settings.gradle")
    }

    TestFile file(Object... path) {
        projectDir.file(path)
    }

    /**
     * Returns the set of implicit task names expected for a non-root project for the target Gradle version.
     */
    Set<String> getImplicitTasks() {
        if (GradleVersion.version(targetDist.version.baseVersion.version) >= GradleVersion.version("2.10")) {
            return ['buildEnvironment', 'components', 'dependencies', 'dependencyInsight', 'help', 'projects', 'properties', 'tasks', 'model']
        } else if (GradleVersion.version(targetDist.version.baseVersion.version) >= GradleVersion.version("2.4")) {
            return ['components', 'dependencies', 'dependencyInsight', 'help', 'projects', 'properties', 'tasks', 'model']
        } else if (GradleVersion.version(targetDist.version.baseVersion.version) >= GradleVersion.version("2.1")) {
            return ['components', 'dependencies', 'dependencyInsight', 'help', 'projects', 'properties', 'tasks']
        } else {
            return ['dependencies', 'dependencyInsight', 'help', 'projects', 'properties', 'tasks']
        }
    }

    /**
     * Returns the set of implicit selector names expected for a non-root project for the target Gradle version.
     *
     * <p>Note that in some versions the handling of implicit selectors was broken, so this method may return a different value
     * to {@link #getImplicitTasks()}.
     */
    Set<String> getImplicitSelectors() {
        if (GradleVersion.version(targetDist.version.baseVersion.version) <= GradleVersion.version("2.0")) {
            // Implicit tasks were ignored
            return []
        }
        return getImplicitTasks()
    }

    /**
     * Returns the set of implicit task names expected for a root project for the target Gradle version.
     */
    Set<String> getRootProjectImplicitTasks() {
        def targetVersion = GradleVersion.version(targetDist.version.baseVersion.version)
        if (targetVersion == GradleVersion.version("1.6")) {
            return implicitTasks + ['setupBuild']
        }
        return implicitTasks + ['init', 'wrapper']
    }

    /**
     * Returns the set of implicit selector names expected for a root project for the target Gradle version.
     *
     * <p>Note that in some versions the handling of implicit selectors was broken, so this method may return a different value
     * to {@link #getRootProjectImplicitTasks()}.
     */
    Set<String> getRootProjectImplicitSelectors() {
        def targetVersion = GradleVersion.version(targetDist.version.baseVersion.version)
        if (targetVersion == GradleVersion.version("1.6")) {
            // Implicit tasks were ignored, and setupBuild was added as a regular task
            return ['setupBuild']
        }
        if (targetVersion <= GradleVersion.version("2.0")) {
            // Implicit tasks were ignored
            return []
        }
        return rootProjectImplicitTasks
    }

    /**
     * Returns the set of implicit tasks returned by GradleProject.getTasks()
     *
     * <p>Note that in some versions the handling of implicit tasks was broken, so this method may return a different value
     * to {@link #getRootProjectImplicitTasks()}.
     */
    Set<String> getRootProjectImplicitTasksForGradleProjectModel() {
        def targetVersion = GradleVersion.version(targetDist.version.baseVersion.version)
        if (targetVersion == GradleVersion.version("1.6")) {
            // Implicit tasks were ignored, and setupBuild was added as a regular task
            return ['setupBuild']
        }

        targetVersion < GradleVersion.version("2.3") ? [] : rootProjectImplicitTasks
    }


    EclipseProject loadEclipseProjectModel() {
        loadToolingModel(EclipseProject)
    }

    public <T> T loadToolingModel(Class<T> modelClass) {
        def model = withConnection { connection -> connection.getModel(modelClass) }
        assertToolingModelIsSerializable(model)
        model
    }

    void assertToolingModelIsSerializable(object) {
        // Only check serialization with 2.12+ since Tooling Model proxy serialization is broken in previous TAPI clients
        if (toolingApiVersion.getBaseVersion() >= VersionNumber.parse("2.12")) {
            // only check that no exceptions are thrown during serialization
            // cross-version deserialization would require using PayloadSerializer
            ObjectOutputStream oos = new ObjectOutputStream(new NullOutputStream());
            try {
                oos.writeObject(object);
            } finally {
                oos.flush();
            }
        }
    }

    private static class NullOutputStream extends OutputStream {
        @Override
        void write(int b) throws IOException {
            // ignore
        }
    }
}
