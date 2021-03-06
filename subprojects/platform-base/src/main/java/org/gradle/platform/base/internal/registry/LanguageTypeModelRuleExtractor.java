/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.platform.base.internal.registry;

import com.google.common.collect.ImmutableList;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.LanguageSourceSetFactory;
import org.gradle.language.base.plugins.LanguageBasePlugin;
import org.gradle.language.base.sources.BaseLanguageSourceSet;
import org.gradle.model.internal.core.ModelReference;
import org.gradle.model.internal.manage.schema.ModelSchemaStore;
import org.gradle.platform.base.LanguageType;

public class LanguageTypeModelRuleExtractor extends TypeModelRuleExtractor<LanguageType, LanguageSourceSet, BaseLanguageSourceSet, LanguageSourceSetFactory> {
    public LanguageTypeModelRuleExtractor(ModelSchemaStore schemaStore) {
        super("language", LanguageSourceSet.class, BaseLanguageSourceSet.class, ModelReference.of(LanguageSourceSetFactory.class), ImmutableList.of(LanguageBasePlugin.class), schemaStore);
    }
}
