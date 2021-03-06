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

package org.gradle.tooling.internal.provider;

import org.gradle.initialization.BuildRequestContext;
import org.gradle.internal.invocation.BuildAction;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.launcher.exec.BuildActionExecuter;
import org.gradle.launcher.exec.CompositeBuildActionParameters;
import org.gradle.launcher.exec.CompositeBuildActionRunner;

public class CompositeBuildActionExecuter implements BuildActionExecuter<CompositeBuildActionParameters> {
    private final CompositeBuildActionRunner compositeBuildActionRunner;

    public CompositeBuildActionExecuter(CompositeBuildActionRunner compositeBuildActionRunner) {
        this.compositeBuildActionRunner = compositeBuildActionRunner;
    }

    @Override
    public Object execute(BuildAction action, BuildRequestContext requestContext, CompositeBuildActionParameters actionParameters, ServiceRegistry contextServices) {
        DefaultCompositeBuildController buildController = new DefaultCompositeBuildController(contextServices);
        compositeBuildActionRunner.run(action, requestContext, actionParameters, buildController);
        return buildController.getResult();
    }

}
