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
package org.springframework.data.neo4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ReactiveFluentFindOperation}.
 *
 * @author Michael J. Simons
 * @soundtrack Ozzy Osbourne - Ordinary Man
 * @since 6.1
 */
final class ReactiveFluentOperationSupport implements ReactiveFluentFindOperation, ReactiveFluentSaveOperation {

	private final ReactiveNeo4jTemplate template;

	ReactiveFluentOperationSupport(ReactiveNeo4jTemplate template) {
		this.template = template;
	}

	@Override
	public <T> ExecutableFind<T> find(Class<T> domainType) {

		Assert.notNull(domainType, "DomainType must not be null!");

		return new ExecutableFindSupport<>(template, domainType, domainType, null, Collections.emptyMap());
	}

	private static class ExecutableFindSupport<T>
			implements ExecutableFind<T>, FindWithProjection<T>, FindWithQuery<T>, TerminatingFind<T> {

		private final ReactiveNeo4jTemplate template;
		private final Class<?> domainType;
		private final Class<T> returnType;
		private final String query;
		private final Map<String, Object> parameters;
		private final QueryFragmentsAndParameters queryFragmentsAndParameters;

		ExecutableFindSupport(ReactiveNeo4jTemplate template, Class<?> domainType, Class<T> returnType, String query,
				Map<String, Object> parameters) {
			this.template = template;
			this.domainType = domainType;
			this.returnType = returnType;
			this.query = query;
			this.parameters = parameters;
			this.queryFragmentsAndParameters = null;
		}

		ExecutableFindSupport(ReactiveNeo4jTemplate template, Class<?> domainType, Class<T> returnType, QueryFragmentsAndParameters queryFragmentsAndParameters) {
			this.template = template;
			this.domainType = domainType;
			this.returnType = returnType;
			this.query = null;
			this.parameters = null;
			this.queryFragmentsAndParameters = queryFragmentsAndParameters;
		}

		@Override
		@SuppressWarnings("HiddenField")
		public <T1> FindWithQuery<T1> as(Class<T1> returnType) {

			Assert.notNull(returnType, "ReturnType must not be null!");

			return new ExecutableFindSupport<>(template, domainType, returnType, query, parameters);
		}

		@Override
		@SuppressWarnings("HiddenField")
		public TerminatingFind<T> matching(String query, Map<String, Object> parameters) {

			Assert.notNull(query, "Query must not be null!");

			return new ExecutableFindSupport<>(template, domainType, returnType, query, parameters);
		}

		@Override
		@SuppressWarnings("HiddenField")
		public TerminatingFind<T> matching(QueryFragmentsAndParameters queryFragmentsAndParameters) {

			return new ExecutableFindSupport<>(template, domainType, returnType, queryFragmentsAndParameters);
		}

		@Override
		public Mono<T> one() {
			return doFind(TemplateSupport.FetchType.ONE).single();
		}

		@Override
		public Flux<T> all() {
			return doFind(TemplateSupport.FetchType.ALL);
		}

		private Flux<T> doFind(TemplateSupport.FetchType fetchType) {
			return template.doFind(query, parameters, domainType, returnType, fetchType, queryFragmentsAndParameters);
		}
	}

	@Override
	public <T> ExecutableSave<T> save(Class<T> domainType) {
		Assert.notNull(domainType, "DomainType must not be null!");

		return new ExecutableSaveSupport<>(this.template, domainType);
	}

	private static class ExecutableSaveSupport<DT> implements ReactiveFluentSaveOperation.ExecutableSave<DT> {

		private final ReactiveNeo4jTemplate template;
		private final Class<DT> domainType;

		ExecutableSaveSupport(ReactiveNeo4jTemplate template, Class<DT> domainType) {
			this.template = template;
			this.domainType = domainType;
		}

		@Override
		public <T> Mono<T> one(T instance) {

			return doSave(Collections.singleton(instance)).single();
		}

		@Override
		public <T> Flux<T> all(Iterable<T> instances) {

			return doSave(instances);
		}

		private <T> Flux<T> doSave(Iterable<T> instances) {
			return template.doSave(instances, domainType);
		}
	}
}
