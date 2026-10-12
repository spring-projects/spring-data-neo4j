/*
 * Copyright 2011-present the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.PropertyFilter.ProjectedPath;
import org.springframework.data.neo4j.core.mapping.PropertyFilter.RelaxedPropertyPath;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ResultProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author 안승현
 */
class PropertyFilterSupportTests {

	private final ProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

	private final Neo4jMappingContext mappingContext = new Neo4jMappingContext();

	@ParameterizedTest // GH-3125
	@ValueSource(strings = { "deleteByName", "deleteByNameReturningVoid", "findByName" })
	void shouldNotInspectProjectionInformationForNonProjectingReturnTypes(String methodName)
			throws NoSuchMethodException {

		ResultProcessor resultProcessor = resultProcessorFor(methodName);
		ProjectionFactory factory = mock(ProjectionFactory.class);

		var properties = PropertyFilterSupport.getInputProperties(resultProcessor, factory, this.mappingContext);

		assertThat(properties).isEmpty();
		verifyNoInteractions(factory);
	}

	@Test // GH-3125
	void shouldKeepInputPropertiesForClosedProjections() throws NoSuchMethodException {

		ResultProcessor resultProcessor = resultProcessorFor("findNameByName");

		var properties = PropertyFilterSupport.getInputProperties(resultProcessor, this.projectionFactory,
				this.mappingContext);

		assertThat(properties).singleElement()
			.usingRecursiveComparison()
			.isEqualTo(new ProjectedPath(RelaxedPropertyPath.withRootType(NameProjection.class).append("name"), false));
	}

	@Test // GH-3125
	void shouldNotFilterInputPropertiesForOpenProjections() throws NoSuchMethodException {

		ResultProcessor resultProcessor = resultProcessorFor("findOpenNameByName");

		var properties = PropertyFilterSupport.getInputProperties(resultProcessor, this.projectionFactory,
				this.mappingContext);

		assertThat(properties).isEmpty();
	}

	private ResultProcessor resultProcessorFor(String methodName) throws NoSuchMethodException {

		Method method = PersonRepository.class.getMethod(methodName, String.class);
		var metadata = new DefaultRepositoryMetadata(PersonRepository.class);
		return new QueryMethod(method, metadata, this.projectionFactory).getResultProcessor();
	}

	interface PersonRepository extends Repository<Person, Long> {

		void deleteByName(String name);

		Void deleteByNameReturningVoid(String name);

		Person findByName(String name);

		NameProjection findNameByName(String name);

		OpenNameProjection findOpenNameByName(String name);

	}

	@Node
	record Person(@Id Long id, String name) {
	}

	interface NameProjection {

		String getName();

	}

	interface OpenNameProjection {

		@Value("#{target.name}")
		String getName();

	}

}
