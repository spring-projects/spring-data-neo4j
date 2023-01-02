/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.repository.support;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.apiguardian.api.API;
import org.springframework.data.neo4j.config.Neo4jCdiExtension;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

/**
 * The CDI pendant to the {@link Neo4jRepositoryFactoryBean}. It creates instances of {@link Neo4jRepositoryFactory}.
 *
 * @author Michael J. Simons
 * @param <T> The type of the repository being created
 * @soundtrack Various - TRON Legacy R3conf1gur3d
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class Neo4jRepositoryFactoryCdiBean<T> extends CdiRepositoryBean<T> {

	private final BeanManager beanManager;

	public Neo4jRepositoryFactoryCdiBean(Set<Annotation> qualifiers, Class<T> repositoryType,
			BeanManager beanManager, CustomRepositoryImplementationDetector detector) {
		super(qualifiers, repositoryType, beanManager, Optional.of(detector));

		this.beanManager = beanManager;
	}

	@Override
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

		Neo4jOperations neo4jOperations = getReference(Neo4jOperations.class, creationalContext);
		Neo4jMappingContext mappingContext = getReference(Neo4jMappingContext.class, creationalContext);

		return create(() -> new Neo4jRepositoryFactory(neo4jOperations, mappingContext), repositoryType);
	}

	private <RT> RT getReference(Class<RT> clazz, CreationalContext<?> creationalContext) {

		Set<Bean<?>> beans = beanManager.getBeans(clazz, Neo4jCdiExtension.ANY_BEAN);
		if (beans.size() > 1) {
			beans = beans.stream()
					.filter(b -> b.getQualifiers().contains(Neo4jCdiExtension.DEFAULT_BEAN))
					.collect(Collectors.toSet());
		}

		@SuppressWarnings("unchecked")
		RT beanReference = (RT) beanManager.getReference(beanManager.resolve(beans), clazz, creationalContext);
		return beanReference;
	}
}
