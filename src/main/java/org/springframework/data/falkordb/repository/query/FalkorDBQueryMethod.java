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

}
