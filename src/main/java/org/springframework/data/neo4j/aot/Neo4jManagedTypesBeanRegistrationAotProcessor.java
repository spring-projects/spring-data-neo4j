/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.aot;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.aot.ManagedTypesBeanRegistrationAotProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Gerrit Meier
 * @since 7.0.0
 */
public class Neo4jManagedTypesBeanRegistrationAotProcessor extends ManagedTypesBeanRegistrationAotProcessor {

	public Neo4jManagedTypesBeanRegistrationAotProcessor() {
		setModuleIdentifier("neo4j");
	}

	@Override
	protected boolean isMatch(@Nullable Class<?> beanType, @Nullable String beanName) {
		return isNeo4jManagedTypes(beanType) || super.isMatch(beanType, beanName);
	}

	protected boolean isNeo4jManagedTypes(@Nullable Class<?> beanType) {
		return beanType != null && ClassUtils.isAssignable(Neo4jManagedTypes.class, beanType);
	}

	@Override
	protected void contributeType(ResolvableType type, GenerationContext generationContext) {

		if (Neo4jAotPredicates.IS_SIMPLE_TYPE.test(type.toClass())) {
			return;
		}

		super.contributeType(type, generationContext);
	}
}
