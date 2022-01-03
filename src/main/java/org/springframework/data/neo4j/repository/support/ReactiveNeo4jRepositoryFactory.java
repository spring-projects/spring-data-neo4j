/*
 * Copyright 2011-2022 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.ReactiveNeo4jQueryLookupStrategy;
import org.springframework.data.neo4j.repository.query.ReactiveQuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.SimpleReactiveQueryByExampleExecutor;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

/**
 * Factory to create {@link ReactiveNeo4jRepository} instances.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class ReactiveNeo4jRepositoryFactory extends ReactiveRepositoryFactorySupport {

	private final ReactiveNeo4jOperations neo4jOperations;

	private final Neo4jMappingContext mappingContext;

	ReactiveNeo4jRepositoryFactory(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext) {

		this.neo4jOperations = neo4jOperations;
		this.mappingContext = mappingContext;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, ID> Neo4jEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		Neo4jPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(domainClass);
		return new DefaultNeo4jEntityInformation<>((Neo4jPersistentEntity<T>) entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {

		Neo4jEntityInformation<?, Object> entityInformation = getEntityInformation(metadata.getDomainType());
		Neo4jRepositoryFactorySupport.assertIdentifierType(metadata.getIdType(), entityInformation.getIdType());
		return getTargetRepositoryViaReflection(metadata, neo4jOperations, entityInformation);
	}

	@Override
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

		RepositoryFragments fragments = RepositoryFragments.empty();

		SimpleReactiveQueryByExampleExecutor<?> byExampleExecutor = instantiateClass(
				SimpleReactiveQueryByExampleExecutor.class, neo4jOperations, mappingContext);

		fragments = fragments.append(RepositoryFragment.implemented(byExampleExecutor));

		boolean isQueryDslRepository = QuerydslUtils.QUERY_DSL_PRESENT
									   && ReactiveQuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		if (isQueryDslRepository) {

			fragments = fragments.append(createDSLExecutorFragment(metadata, ReactiveQuerydslNeo4jPredicateExecutor.class));
		}

		return fragments;
	}

	private RepositoryFragment<Object> createDSLExecutorFragment(RepositoryMetadata metadata, Class<?> implementor) {

		Neo4jEntityInformation<?, Object> entityInformation = getEntityInformation(metadata.getDomainType());
		Object querydslFragment = instantiateClass(implementor, entityInformation, neo4jOperations);

		return RepositoryFragment.implemented(querydslFragment);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleReactiveNeo4jRepository.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key, org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		return Optional
				.of(new ReactiveNeo4jQueryLookupStrategy(neo4jOperations, mappingContext, evaluationContextProvider));
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
	}
}
