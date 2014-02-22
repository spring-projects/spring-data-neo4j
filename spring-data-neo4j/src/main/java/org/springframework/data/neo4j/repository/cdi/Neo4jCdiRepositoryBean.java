/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.neo4j.repository.cdi;

import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.repository.GraphRepositoryFactory;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.util.Assert;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * {@link org.springframework.data.repository.cdi.CdiRepositoryBean} to create Neo4j repository instances via CDI.
 * 
 * @author Nicki Watt
 */
public class Neo4jCdiRepositoryBean<T> extends CdiRepositoryBean<T> {

	private final Bean<GraphDatabase> graphDatabase;

	/**
	 * Creates a new {@link Neo4jCdiRepositoryBean}.
	 * 
	 * @param graphDatabase must not be {@literal null}.
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType must not be {@literal null}.
	 * @param beanManager must not be {@literal null}.
	 */
	public Neo4jCdiRepositoryBean(Bean<GraphDatabase> graphDatabase,
			Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {

		super(qualifiers, repositoryType, beanManager);

		this.graphDatabase = graphDatabase;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.cdi.CdiRepositoryBean#create(javax.enterprise.context.spi.CreationalContext, java.lang.Class)
	 */
	@Override
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

		Neo4jMappingContext neo4jMapCtx = new Neo4jMappingContext();
		Neo4jTemplate neo4jTemplate = new Neo4jTemplate(getDependencyInstance(graphDatabase, GraphDatabase.class));

		GraphRepositoryFactory factory = new GraphRepositoryFactory(neo4jTemplate, neo4jMapCtx);
		return factory.getRepository(repositoryType);
	}
}
