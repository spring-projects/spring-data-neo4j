/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.neo4j.ogm.metadata.MetaData;
import org.springframework.data.neo4j.domain.sample.NodeWithUUIDAsId;
import org.springframework.data.neo4j.domain.sample.User;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class Neo4jPersistentPropertyTests {

	@Test // DATAGRAPH-1081
	public void detectsOgmVersionAnnotation() {
		MetaData metaData = new MetaData("org.springframework.data.neo4j.domain.sample");
		Neo4jMappingContext mappingContext = new Neo4jMappingContext(metaData);
		Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(User.class);
		Neo4jPersistentProperty version = persistentEntity.getRequiredPersistentProperty("version");
		assertThat(version.isVersionProperty()).isTrue();
	}

	@Test // DATAGRAPH-1144
	public void shouldDetectExplicitIdFieldsAsIdProperties() {
		MetaData metaData = new MetaData("org.springframework.data.neo4j.domain.sample");
		Neo4jMappingContext mappingContext = new Neo4jMappingContext(metaData);
		Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(NodeWithUUIDAsId.class);
		Neo4jPersistentProperty idProperty = persistentEntity.getRequiredIdProperty();
		assertThat(idProperty.isIdProperty());
		assertThat(idProperty.getName()).isEqualTo("myNiceId");
	}
}
