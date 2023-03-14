/*
 * Copyright 2011-2023 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Neo4j specific implementation of {@link QueryMethod}. It contains a custom implementation of {@link Parameter} which
 * supports Neo4js specific placeholder as well as a convenient method to return either the parameters index or name
 * without placeholder.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Mark Paluch
 * @since 6.0
 */
class Neo4jQueryMethod extends QueryMethod {

	/**
	 * Optional query annotation of the method.
	 */
	private @Nullable final Query queryAnnotation;

	private final String repositoryName;

	private final boolean cypherBasedProjection;

	/**
	 * Creates a new {@link Neo4jQueryMethod} from the given parameters. Looks up the correct query to use for following
	 * invocations of the method given.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	Neo4jQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		this(method, metadata, factory, ClassUtils.hasMethod(CypherdslStatementExecutor.class, method));
	}

	/**
	 * Allows configuring {@link #cypherBasedProjection} from inheriting classes. Not meant to be called outside the
	 * inheritance tree.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 * @param cypherBasedProjection True if this points to a Cypher-DSL based projection.
	 */
	Neo4jQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			boolean cypherBasedProjection) {
		super(method, metadata, factory);

		Class<?> declaringClass = method.getDeclaringClass();
		this.repositoryName = declaringClass.getName();
		this.cypherBasedProjection = cypherBasedProjection;
		this.queryAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
	}

	String getRepositoryName() {
		return repositoryName;
	}

	boolean isCollectionLikeQuery() {
		return isCollectionQuery() || isStreamQuery();
	}

	boolean isCypherBasedProjection() {
		return cypherBasedProjection;
	}

	/**
	 * @return True if the underlying method has been annotated with {@code @Query}.
	 */
	boolean hasQueryAnnotation() {
		return this.queryAnnotation != null;
	}

	/**
	 * @return the {@link Query} annotation that is applied to the method or an empty {@link Optional} if none available.
	 */
	Optional<Query> getQueryAnnotation() {
		return Optional.ofNullable(this.queryAnnotation);
	}

	@Override
	protected Parameters<Neo4jParameters, Neo4jParameter> createParameters(Method method, TypeInformation<?> domainType) {
		return new Neo4jParameters(method, domainType);
	}

	static class Neo4jParameters extends Parameters<Neo4jParameters, Neo4jParameter> {

		Neo4jParameters(Method method, TypeInformation<?> domainType) {
			super(method, it -> new Neo4jParameter(it, domainType));
		}

		private Neo4jParameters(List<Neo4jParameter> originals) {
			super(originals);
		}

		@Override
		protected Neo4jParameters createFrom(List<Neo4jParameter> parameters) {
			return new Neo4jParameters(parameters);
		}
	}

	static class Neo4jParameter extends Parameter {

		private static final String NAMED_PARAMETER_TEMPLATE = "$%s";
		private static final String POSITION_PARAMETER_TEMPLATE = "$%d";

		/**
		 * Creates a new {@link Parameter} for the given {@link MethodParameter} and {@link TypeInformation}.
		 *
		 * @param parameter must not be {@literal null}.
		 * @param domainType must not be {@literal null}.
		 */
		Neo4jParameter(MethodParameter parameter, TypeInformation<?> domainType) {
			super(parameter, domainType);
		}

		public String getPlaceholder() {

			if (isNamedParameter()) {
				return String.format(NAMED_PARAMETER_TEMPLATE, getName().get());
			} else {
				return String.format(POSITION_PARAMETER_TEMPLATE, getIndex());
			}
		}

		public String getNameOrIndex() {
			return this.getName().orElseGet(() -> Integer.toString(this.getIndex()));
		}
	}

	boolean incrementLimit() {
		return (this.isSliceQuery() && this.getQueryAnnotation().map(Query::countQuery).filter(StringUtils::hasText).isEmpty()) || this.isScrollQuery();
	}

	boolean asCollectionQuery() {
		return this.isCollectionLikeQuery() || this.isPageQuery() || this.isSliceQuery() || this.isScrollQuery();
	}
}
