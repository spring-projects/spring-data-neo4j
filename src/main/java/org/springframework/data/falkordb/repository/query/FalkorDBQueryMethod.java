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
package org.springframework.data.falkordb.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * FalkorDB-specific implementation of {@link QueryMethod}.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class FalkorDBQueryMethod extends QueryMethod {

	private final FalkorDBMappingContext mappingContext;

	/**
	 * Creates a new {@link FalkorDBQueryMethod}.
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param projectionFactory must not be {@literal null}.
	 * @param mappingContext must not be {@literal null}.
	 */
	public FalkorDBQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
			FalkorDBMappingContext mappingContext) {
		super(method, metadata, projectionFactory);
		this.mappingContext = mappingContext;
	}

	/**
	 * Returns the {@link FalkorDBMappingContext} used.
	 * @return the mapping context
	 */
	public FalkorDBMappingContext getMappingContext() {
		return this.mappingContext;
	}

	/**
	 * Returns whether the method has an annotated query.
	 * @return {@literal true} if the method has a {@link Query} annotation.
	 */
	public boolean hasAnnotatedQuery() {
		return getAnnotatedQuery() != null;
	}

	/**
	 * Returns the query string declared in a {@link Query} annotation or {@literal null} if neither the annotation found
	 * nor the attribute was specified.
	 * @return the query string or {@literal null}.
	 */
	@Nullable
	public String getAnnotatedQuery() {
		Query query = getMethod().getAnnotation(Query.class);
		if (query == null) {
			return null;
		}

		String queryString = query.value();
		if (!StringUtils.hasText(queryString)) {
			queryString = query.cypher();
		}

		return StringUtils.hasText(queryString) ? queryString : null;
	}

	/**
	 * Returns whether the query method is a count query.
	 * @return {@literal true} if the query is marked as count query.
	 */
	public boolean isCountQuery() {
		Query query = getMethod().getAnnotation(Query.class);
		return query != null && query.count();
	}

	/**
	 * Returns whether the query method is an exists query.
	 * @return {@literal true} if the query is marked as exists query.
	 */
	public boolean isExistsQuery() {
		Query query = getMethod().getAnnotation(Query.class);
		return query != null && query.exists();
	}

	/**
	 * Returns whether the query method is a write operation.
	 * @return {@literal true} if the query is marked as write operation.
	 */
	public boolean isWriteQuery() {
		Query query = getMethod().getAnnotation(Query.class);
		return query != null && query.write();
	}

}
