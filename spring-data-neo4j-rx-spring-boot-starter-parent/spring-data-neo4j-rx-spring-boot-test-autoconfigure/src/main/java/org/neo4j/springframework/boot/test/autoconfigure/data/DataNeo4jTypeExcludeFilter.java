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
package org.neo4j.springframework.boot.test.autoconfigure.data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataNeo4jTest @DataNeo4jTest}.
 *
 * @author Michael J. Simons
 * @soundtrack Iron Maiden - Rock In Rio
 * @since 1.0
 */
class DataNeo4jTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataNeo4jTest> {

	DataNeo4jTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

	private static final Set<Class<?>> DEFAULT_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		includes.add(Driver.class);
		includes.add(Neo4jClient.class);
		includes.add(ReactiveNeo4jClient.class);
		includes.add(Neo4jRepository.class);
		includes.add(ReactiveNeo4jRepository.class);
		DEFAULT_INCLUDES = Collections.unmodifiableSet(includes);
	}

	@Override
	protected Set<Class<?>> getDefaultIncludes() {
		return DEFAULT_INCLUDES;
	}
}
