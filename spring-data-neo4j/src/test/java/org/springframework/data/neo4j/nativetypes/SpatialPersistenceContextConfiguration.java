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
package org.springframework.data.neo4j.nativetypes;

import org.neo4j.harness.ServerControls;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;

/**
 * Shared configuration for all tests involving native, spatial types.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Configuration
@Neo4jIntegrationTest(
		domainPackages = { "org.springframework.data.neo4j.nativetypes",
				"org.springframework.data.neo4j.examples.restaurants.domain" },
		repositoryPackages = { "org.springframework.data.neo4j.nativetypes",
				"org.springframework.data.neo4j.examples.restaurants.repo" })
@ComponentScan("org.springframework.data.neo4j.examples.restaurants")
class SpatialPersistenceContextConfiguration {

	@Bean
	org.neo4j.ogm.config.Configuration neo4jOGMConfiguration(ServerControls serverControls) {
		return new org.neo4j.ogm.config.Configuration.Builder() //
				.uri(serverControls.boltURI().toString()) //
				.useNativeTypes() //
				.verifyConnection(true)
				.build();
	}

}
