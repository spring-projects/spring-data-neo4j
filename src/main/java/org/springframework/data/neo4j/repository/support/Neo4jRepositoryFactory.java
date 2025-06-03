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
package org.springframework.data.neo4j.repository.support;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.CypherdslConditionExecutorImpl;
import org.springframework.data.neo4j.repository.query.Neo4jQueryLookupStrategy;
import org.springframework.data.neo4j.repository.query.QuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.SimpleQueryByExampleExecutor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Factory to create {@link Neo4jRepository} instances.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class Neo4jRepositoryFactory extends RepositoryFactorySupport {

	private final Neo4jOperations neo4jOperations;

	private final Neo4jMappingContext mappingContext;

	private Configuration cypherDSLConfiguration = Configuration.defaultConfig();

	Neo4jRepositoryFactory(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext) {

		this.neo4jOperations = neo4jOperations;
		this.mappingContext = mappingContext;
	}

	@Override
	public Neo4jEntityInformation<?, ?> getEntityInformation(RepositoryMetadata metadata) {

		Neo4jPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(metadata.getDomainType());
		return new DefaultNeo4jEntityInformation<>(entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {

		Neo4jEntityInformation<?, ?> entityInformation = getEntityInformation(metadata);
		Neo4jRepositoryFactorySupport.assertIdentifierType(metadata.getIdType(), entityInformation.getIdType());
		return getTargetRepositoryViaReflection(metadata, neo4jOperations, entityInformation);
	}

	@Override
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

		RepositoryFragments fragments = RepositoryFragments.empty();

		Object byExampleExecutor = instantiateClass(SimpleQueryByExampleExecutor.class, neo4jOperations,
				mappingContext);

		fragments = fragments.append(RepositoryFragment.implemented(byExampleExecutor));

		boolean isQueryDslRepository = QuerydslUtils.QUERY_DSL_PRESENT
									   && QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		if (isQueryDslRepository) {

			fragments = fragments.append(createDSLPredicateExecutorFragment(metadata, QuerydslNeo4jPredicateExecutor.class));
		}

		if (CypherdslConditionExecutor.class.isAssignableFrom(metadata.getRepositoryInterface())) {

			fragments = fragments.append(createDSLExecutorFragment(metadata, CypherdslConditionExecutorImpl.class));
		}

		return fragments;
	}

	private RepositoryFragment<Object> createDSLPredicateExecutorFragment(RepositoryMetadata metadata, Class<?> implementor) {

		Neo4jEntityInformation<?, ?> entityInformation = getEntityInformation(metadata);
		Object querydslFragment = instantiateClass(implementor, mappingContext, entityInformation, neo4jOperations);

		return RepositoryFragment.implemented(querydslFragment);
	}

	private RepositoryFragment<Object> createDSLExecutorFragment(RepositoryMetadata metadata, Class<?> implementor) {

		Neo4jEntityInformation<?, ?> entityInformation = getEntityInformation(metadata);
		Object querydslFragment = instantiateClass(implementor, entityInformation, neo4jOperations);

		return RepositoryFragment.implemented(querydslFragment);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleNeo4jRepository.class;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		this.cypherDSLConfiguration = beanFactory
				.getBeanProvider(Configuration.class)
				.getIfAvailable(Configuration::defaultConfig);
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable Key key,
			ValueExpressionDelegate valueExpressionDelegate) {
		return Optional.of(new Neo4jQueryLookupStrategy(neo4jOperations, mappingContext, valueExpressionDelegate, cypherDSLConfiguration));
	}

	@Override
	protected ProjectionFactory getProjectionFactory() {

		ProjectionFactory projectionFactory = super.getProjectionFactory();
		if (projectionFactory instanceof SpelAwareProxyProjectionFactory) {
			((SpelAwareProxyProjectionFactory) projectionFactory).registerMethodInvokerFactory(
					EntityAndGraphPropertyAccessingMethodInterceptor.createMethodInterceptorFactory(mappingContext));
		}
		return projectionFactory;
	}
}
