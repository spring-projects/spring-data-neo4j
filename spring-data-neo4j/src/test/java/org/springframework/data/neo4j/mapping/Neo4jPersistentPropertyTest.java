/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.neo4j.ogm.metadata.MetaData;
import org.springframework.data.neo4j.domain.sample.User;

/**
 * @author Gerrit Meier
 */
public class Neo4jPersistentPropertyTest {

	@Test // DATAGRAPH-1081
	public void detectsOgmVersionAnnotation() {
		MetaData metaData = new MetaData("org.springframework.data.neo4j.domain.sample");
		Neo4jMappingContext mappingContext = new Neo4jMappingContext(metaData);
		Neo4jPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(User.class);
		Neo4jPersistentProperty version = persistentEntity.getRequiredPersistentProperty("version");
		assertThat(version.isVersionProperty()).isTrue();
	}

}
