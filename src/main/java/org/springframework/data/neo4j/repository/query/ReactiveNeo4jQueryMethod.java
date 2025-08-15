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
package org.springframework.data.neo4j.repository.query;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.repository.support.ReactiveCypherdslStatementExecutor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.ReactiveWrappers;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;

/**
 * This is unfortunately a little bit of a hack to provide the information that the
 * returned types by this query are always considered as stream. We try to either separate
 * imperative and reactive concerns but due to type compatibility we extend the
 * {@link Neo4jQueryMethod} here instead of creating a complete new reactive focused
 * logical branch. It would only contain duplications of several classes.
 *
 * @author Gerrit Meier
 * @author Mark Paluch
 * @since 6.0
 */
final class ReactiveNeo4jQueryMethod extends Neo4jQueryMethod {

	static final List<Class<? extends Serializable>> VECTOR_SEARCH_RESULTS = List.of(SearchResult.class);

	@SuppressWarnings("rawtypes")
	private static final TypeInformation<Page> PAGE_TYPE = TypeInformation.of(Page.class);

	@SuppressWarnings("rawtypes")
	private static final TypeInformation<Slice> SLICE_TYPE = TypeInformation.of(Slice.class);

	private final Lazy<Boolean> isCollectionQuery;

	/**
	 * Creates a new {@link ReactiveNeo4jQueryMethod} from the given parameters.
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	ReactiveNeo4jQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory, ClassUtils.hasMethod(ReactiveCypherdslStatementExecutor.class, method));

		if (ReflectionUtils.hasParameterOfType(method, Pageable.class)) {

			TypeInformation<?> returnType = TypeInformation.fromReturnTypeOf(method);

			boolean multiWrapper = ReactiveWrappers.isMultiValueType(returnType.getType());
			boolean singleWrapperWithWrappedPageableResult = ReactiveWrappers.isSingleValueType(returnType.getType())
					&& (PAGE_TYPE.isAssignableFrom(returnType.getRequiredComponentType())
							|| SLICE_TYPE.isAssignableFrom(returnType.getRequiredComponentType()));

			if (singleWrapperWithWrappedPageableResult) {
				throw new InvalidDataAccessApiUsageException(String.format(
						"'%s.%s' must not use sliced or paged execution, please use Flux.buffer(size, skip)",
						ClassUtils.getShortName(method.getDeclaringClass()), method.getName()));
			}

			if (!multiWrapper) {
				throw new IllegalStateException(String.format(
						"Method has to use a multi-item reactive wrapper return type. Offending method: %s",
						method.toString()));
			}
		}

		this.isCollectionQuery = Lazy.of(() -> (!(isPageQuery() || isSliceQuery())
				&& ReactiveWrappers.isMultiValueType(metadata.getReturnType(method).getType()))
				|| super.isCollectionQuery());
	}

	@Override
	public boolean isCollectionQuery() {
		return this.isCollectionQuery.get();
	}

	/**
	 * Always return {@literal true} to skip {@link Pageable} validation in
	 * {@link org.springframework.data.repository.query.QueryMethod#QueryMethod(Method, RepositoryMetadata, ProjectionFactory)}.
	 * @return always {@literal true}.
	 */
	@Override
	public boolean isStreamQuery() {
		return true;
	}

	/**
	 * Consider only {@link #isCollectionQuery()} as {@link java.util.stream.Stream} query
	 * isn't applicable here.
	 */
	@Override
	boolean isCollectionLikeQuery() {
		return isCollectionQuery();
	}

}
