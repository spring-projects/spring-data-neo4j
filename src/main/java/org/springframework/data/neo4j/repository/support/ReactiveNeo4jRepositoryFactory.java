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
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.ReactiveCypherdslConditionExecutorImpl;
import org.springframework.data.neo4j.repository.query.ReactiveNeo4jQueryLookupStrategy;
import org.springframework.data.neo4j.repository.query.ReactiveQuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.SimpleReactiveQueryByExampleExecutor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Factory to create {@link ReactiveNeo4jRepository} instances.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Niklas Krieger
 * @since 6.0
 */
final class ReactiveNeo4jRepositoryFactory extends ReactiveRepositoryFactorySupport {

	private final ReactiveNeo4jOperations neo4jOperations;

	private final Neo4jMappingContext mappingContext;

	private Configuration cypherDSLConfiguration = Configuration.defaultConfig();

	ReactiveNeo4jRepositoryFactory(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext) {

		this.neo4jOperations = neo4jOperations;
		this.mappingContext = mappingContext;
	}

	@Override
	public Neo4jEntityInformation<?, ?> getEntityInformation(RepositoryMetadata metadata) {

		Neo4jPersistentEntity<?> entity = this.mappingContext.getRequiredPersistentEntity(metadata.getDomainType());
		return new DefaultNeo4jEntityInformation<>(entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {

		Neo4jEntityInformation<?, ?> entityInformation = getEntityInformation(metadata);
		Neo4jRepositoryFactorySupport.assertIdentifierType(metadata.getIdType(), entityInformation.getIdType());
		return getTargetRepositoryViaReflection(metadata, this.neo4jOperations, entityInformation);
	}

	@Override
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

		RepositoryFragments fragments = RepositoryFragments.empty();

		SimpleReactiveQueryByExampleExecutor<?> byExampleExecutor = instantiateClass(
				SimpleReactiveQueryByExampleExecutor.class, this.neo4jOperations, this.mappingContext);

		fragments = fragments.append(RepositoryFragment.implemented(byExampleExecutor));

		boolean isQueryDslRepository = QuerydslUtils.QUERY_DSL_PRESENT
				&& ReactiveQuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		if (isQueryDslRepository) {

			fragments = fragments
				.append(createDSLPredicateExecutorFragment(metadata, ReactiveQuerydslNeo4jPredicateExecutor.class));
		}

		if (ReactiveCypherdslConditionExecutor.class.isAssignableFrom(metadata.getRepositoryInterface())) {

			fragments = fragments
				.append(createDSLExecutorFragment(metadata, ReactiveCypherdslConditionExecutorImpl.class));
		}

		return fragments;
	}

	private RepositoryFragment<Object> createDSLPredicateExecutorFragment(RepositoryMetadata metadata,
			Class<?> implementor) {

		Neo4jEntityInformation<?, ?> entityInformation = getEntityInformation(metadata);
		Object querydslFragment = instantiateClass(implementor, this.mappingContext, entityInformation,
				this.neo4jOperations);

		return RepositoryFragment.implemented(querydslFragment);
	}

	private RepositoryFragment<Object> createDSLExecutorFragment(RepositoryMetadata metadata, Class<?> implementor) {

		Neo4jEntityInformation<?, ?> entityInformation = getEntityInformation(metadata);
		Object querydslFragment = instantiateClass(implementor, entityInformation, this.neo4jOperations);

		return RepositoryFragment.implemented(querydslFragment);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleReactiveNeo4jRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable Key key,
			ValueExpressionDelegate valueExpressionDelegate) {
		return Optional.of(new ReactiveNeo4jQueryLookupStrategy(this.neo4jOperations, this.mappingContext,
				valueExpressionDelegate, this.cypherDSLConfiguration));
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		super.setBeanFactory(beanFactory);

		if (beanFactory instanceof ListableBeanFactory) {
			addRepositoryProxyPostProcessor((factory, repositoryInformation) -> {
				ReactivePersistenceExceptionTranslationInterceptor advice = new ReactivePersistenceExceptionTranslationInterceptor(
						(ListableBeanFactory) beanFactory);
				factory.addAdvice(advice);
			});
		}

		this.cypherDSLConfiguration = beanFactory.getBeanProvider(Configuration.class)
			.getIfAvailable(Configuration::defaultConfig);
	}

	@Override
	protected ProjectionFactory getProjectionFactory() {

		ProjectionFactory projectionFactory = super.getProjectionFactory();
		if (projectionFactory instanceof SpelAwareProxyProjectionFactory) {
			((SpelAwareProxyProjectionFactory) projectionFactory)
				.registerMethodInvokerFactory(EntityAndGraphPropertyAccessingMethodInterceptor
					.createMethodInterceptorFactory(this.mappingContext));
		}
		return projectionFactory;
	}

}
