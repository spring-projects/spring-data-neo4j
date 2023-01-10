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
package org.springframework.data.neo4j.core.mapping;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.callback.EventSupport;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.util.TypeInformation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
class DefaultNeo4jEntityConverterTest {

	private final DefaultNeo4jEntityConverter entityConverter;

	DefaultNeo4jEntityConverterTest() {
		EntityInstantiators entityInstantiators = new EntityInstantiators();
		NodeDescriptionStore nodeDescriptionStore = new NodeDescriptionStore();
		DefaultNeo4jConversionService conversionService = new DefaultNeo4jConversionService(new Neo4jConversions());
		Neo4jMappingContext context = new Neo4jMappingContext();
		context.addPersistentEntity(TypeInformation.of(EntityWithDefaultValues.class));
		nodeDescriptionStore.put("User", (DefaultNeo4jPersistentEntity<?>) context.getNodeDescription(EntityWithDefaultValues.class));
		EventSupport eventSupport = EventSupport.useExistingCallbacks(context, EntityCallbacks.create());
		TypeSystem typeSystem = InternalTypeSystem.TYPE_SYSTEM;
		this.entityConverter = new DefaultNeo4jEntityConverter(entityInstantiators, nodeDescriptionStore, conversionService, eventSupport, typeSystem);
	}

	@Test
	void readEntityWithDefaultValuesWithEmptyPropertiesFromDatabase() {
		Map<String, Value> properties = new HashMap<>();
		NodeValue mapAccessor = new NodeValue(
				new InternalNode(1L, Collections.singleton("EntityWithDefaultValues"), properties)
		);

		EntityWithDefaultValues readNode = entityConverter.read(EntityWithDefaultValues.class, mapAccessor);
		assertThat(readNode).isNotNull();
		assertThat(readNode.noDefaultValue).isNull();
		assertThat(readNode.defaultValue).isEqualTo("Test");
	}

	@Test
	void readEntityWithDefaultValuesWithPropertiesFromDatabase() {
		Map<String, Value> properties = new HashMap<>();
		properties.put("noDefaultValue", Values.value("valueFromDatabase1"));
		properties.put("defaultValue", Values.value("valueFromDatabase2"));
		NodeValue mapAccessor = new NodeValue(
				new InternalNode(1L, Collections.singleton("EntityWithDefaultValues"), properties)
		);

		EntityWithDefaultValues readNode = entityConverter.read(EntityWithDefaultValues.class, mapAccessor);
		assertThat(readNode).isNotNull();
		assertThat(readNode.noDefaultValue).isEqualTo("valueFromDatabase1");
		assertThat(readNode.defaultValue).isEqualTo("valueFromDatabase2");
	}

	@Node
	static class EntityWithDefaultValues {
		@Id @GeneratedValue Long id;
		public String noDefaultValue;
		public String defaultValue = "Test";
	}
}
