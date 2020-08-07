/*
 * Copyright 2011-2020 the original author or authors.
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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

/**
 * @author Michael J. Simons
 * @since 6.0
 */
public class Neo4jCdiRepositoryBean<T> extends CdiRepositoryBean<T> {

	private final BeanManager beanManager;
	private Bean<Neo4jOperations> neo4jOperationsBean;
	private Bean<Neo4jMappingContext> mappingContextBean;

	public Neo4jCdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType,
			BeanManager beanManager, Optional<CustomRepositoryImplementationDetector> detector) {
		super(qualifiers, repositoryType, beanManager, detector);

		this.beanManager = beanManager;

		this.neo4jOperationsBean = (Bean<Neo4jOperations>) beanManager.getBeans(Neo4jOperations.class).stream()
				.findFirst().orElseThrow(
						() -> new NoSuchBeanDefinitionException(Neo4jOperations.class));

		this.mappingContextBean = (Bean<Neo4jMappingContext>) beanManager.getBeans(Neo4jMappingContext.class).stream()
				.findFirst().orElseThrow(
						() -> new NoSuchBeanDefinitionException(Neo4jMappingContext.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.cdi.CdiRepositoryBean#create(javax.enterprise.context.spi.CreationalContext, java.lang.Class)
	 */
	@Override
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

		Neo4jOperations neo4jOperations = (Neo4jOperations) beanManager
				.getReference(neo4jOperationsBean, Neo4jOperations.class, creationalContext);
		Neo4jMappingContext mappingContext = (Neo4jMappingContext) beanManager
				.getReference(mappingContextBean, Neo4jMappingContext.class, creationalContext);
		return create(() -> new Neo4jRepositoryFactory(neo4jOperations, mappingContext), repositoryType);
	}
}
