/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.core.convert;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - S&M2
 * @since 6.0
 */
final class DefaultNeo4jPersistentPropertyConverterFactory implements Neo4jPersistentPropertyConverterFactory {

	@Nullable
	private final BeanFactory beanFactory;

	DefaultNeo4jPersistentPropertyConverterFactory(@Nullable BeanFactory beanFactory) {

		this.beanFactory = beanFactory;
	}

	@Override
	public Neo4jPersistentPropertyConverter<?> getPropertyConverterFor(Neo4jPersistentProperty persistentProperty) {

		// At this point we already checked for the annotation.
		ConvertWith config = persistentProperty.getRequiredAnnotation(ConvertWith.class);

		if (StringUtils.hasText(config.converterRef())) {
			if (beanFactory == null) {
				throw new IllegalStateException(
						"The default converter factory has been configured without a bean factory and cannot use a converter from the application context.");
			}

			return beanFactory.getBean(config.converterRef(), Neo4jPersistentPropertyConverter.class);
		}

		if (config.converter() == ConvertWith.UnsetConverter.class) {
			throw new IllegalArgumentException(
					"The default custom conversion factory cannot be used with a placeholder");
		}

		return BeanUtils.instantiateClass(config.converter());
	}
}
