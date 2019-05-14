/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core;

import static java.util.stream.Collectors.*;

import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.springframework.data.neo4j.core.Neo4jClient.MappingSpec;
import org.springframework.data.neo4j.core.Neo4jClient.RecordFetchSpec;
import org.springframework.data.neo4j.core.context.DefaultPersistenceContext;
import org.springframework.data.neo4j.core.context.PersistenceContext;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.Schema;
import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
class DefaultNodeManager implements NodeManager {

	private final Schema schema;

	private final Neo4jClient neo4jClient;

	private final Transaction transaction;

	private final PersistenceContext persistenceContext;

	DefaultNodeManager(Schema schema, Neo4jClient neo4jClient, @Nullable Transaction transaction) {

		this.schema = schema;
		this.neo4jClient = neo4jClient;
		this.transaction = transaction;

		this.persistenceContext = new DefaultPersistenceContext();
	}

	@Override
	@Nullable
	public Transaction getTransaction() {
		return transaction;
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		Class<T> resultType = preparedQuery.getResultType();
		MappingSpec<Optional<T>, Collection<T>, T> mappingSpec = neo4jClient.newQuery(preparedQuery.getCypherQuery())
			.bindAll(preparedQuery.getParameters())
			.fetchAs(resultType);
		RecordFetchSpec<Optional<T>, Collection<T>, T> fetchSpec = preparedQuery.getOptionalMappingFunction()
			.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction))
			.orElse(mappingSpec);

		return new DefaultExecutableQuery(preparedQuery, schema.getNodeDescription(resultType), fetchSpec);
	}

	@Override
	public <T> T save(T entityWithUnknownState) {

		// TODO if already registered, here or in the context?
		this.persistenceContext
			.register(entityWithUnknownState, schema.getRequiredNodeDescription(entityWithUnknownState.getClass()));

		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	public void delete(Object managedEntity) {

		this.persistenceContext.deregister(managedEntity);

		throw new UnsupportedOperationException("Not there yet.");
	}

	@RequiredArgsConstructor
	class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;
		private final Optional<NodeDescription<?>> optionalNodeDescription;
		private final RecordFetchSpec<Optional<T>, Collection<T>, T> fetchSpec;

		@Override
		public List<T> getResults() {
			return fetchSpec.all().stream().map(this::register).collect(toList());
		}

		@Override
		public Optional<T> getSingleResult() {
			try {
				return fetchSpec.one().map(this::register);
			} catch (NoSuchRecordException e) {
				return Optional.empty();
			}
		}

		@Override
		public T getRequiredSingleResult() {
			return fetchSpec.one().map(this::register)
				.orElseThrow(() -> new NoResultException(1L, preparedQuery.getCypherQuery()));
		}

		private T register(T entity) {
			this.optionalNodeDescription.ifPresent(
				nodeDescription -> DefaultNodeManager.this.persistenceContext.register(entity, nodeDescription));
			return entity;
		}
	}
}
