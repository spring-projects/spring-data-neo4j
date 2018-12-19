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

package org.springframework.data.neo4j.nativetypes;

import org.neo4j.harness.ServerControls;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;

/**
 * Shared configuration for all tests involving native, spatial types.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Configuration
@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.nativetypes")
class NativeTypesContextConfiguration {

	@Bean
	org.neo4j.ogm.config.Configuration neo4jOGMConfiguration(ServerControls neo4jTestServer) {
		return new org.neo4j.ogm.config.Configuration.Builder() //
				.uri(neo4jTestServer.boltURI().toString()) //
				.useNativeTypes() //
				.build();
	}
}
