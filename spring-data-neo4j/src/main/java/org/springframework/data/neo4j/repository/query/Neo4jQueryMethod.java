/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.query;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;

/**
 * Neo4j specific implementation of {@link QueryMethod}.
 *
 * @author Gerrit Meier
 **/
public class Neo4jQueryMethod extends QueryMethod {

	private final Method method;

	private Neo4jQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);

		this.method = method;
	}

	/**
	 * Creates a new {@link Neo4jQueryMethod} from the given parameters. Looks up the correct query to use for following
	 * invocations of the method given.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public static Neo4jQueryMethod of(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		return new Neo4jQueryMethod(method, metadata, factory);
	}

	@Nullable
	String getAnnotatedQuery() {
		return getQueryAnnotation().map(Query::value).orElse(null);
	}

	private Optional<Query> getQueryAnnotation() {
		return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, Query.class));
	}

}
