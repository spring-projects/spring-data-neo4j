/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.StringUtils;

/**
 * FalkorDB-specific implementation of {@link QueryMethod}.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class FalkorDBQueryMethod extends QueryMethod {

	/**
	 * The FalkorDB mapping context.
	 */
	private final FalkorDBMappingContext mappingContext;

	/**
	 * The repository method.
	 */
	private final Method repositoryMethod;

	/**
	 * Creates a new {@link FalkorDBQueryMethod}.
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param projectionFactory must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 */
	public FalkorDBQueryMethod(final Method method, final RepositoryMetadata metadata,
			final ProjectionFactory projectionFactory, final FalkorDBMappingContext context) {
		super(method, metadata, projectionFactory);
		this.mappingContext = context;
		this.repositoryMethod = method;
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
	 * Returns the query string declared in a {@link Query} annotation or {@literal null}
	 * if neither the annotation found nor the attribute was specified.
	 * @return the query string or {@literal null}.
	 */
	public String getAnnotatedQuery() {
		Query query = this.repositoryMethod.getAnnotation(Query.class);
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
		Query query = this.repositoryMethod.getAnnotation(Query.class);
		return query != null && query.count();
	}

	/**
	 * Returns whether the query method is an exists query.
	 * @return {@literal true} if the query is marked as exists query.
	 */
	public boolean isExistsQuery() {
		Query query = this.repositoryMethod.getAnnotation(Query.class);
		return query != null && query.exists();
	}

	/**
	 * Returns whether the query method is a write operation.
	 * @return {@literal true} if the query is marked as write operation.
	 */
	public boolean isWriteQuery() {
		Query query = this.repositoryMethod.getAnnotation(Query.class);
		return query != null && query.write();
	}

}
