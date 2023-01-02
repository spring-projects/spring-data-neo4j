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
package org.springframework.data.neo4j.documentation.repositories;

// tag::java-config-imperative[]

import java.util.Arrays;
import java.util.Collection;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Context configuration for documentation reference.
 */
// tag::java-config-imperative-short[]
@Configuration // <1>
@EnableNeo4jRepositories // <2>
@EnableTransactionManagement // <3>
public class Config extends AbstractNeo4jConfig { // <4>

	@Bean
	public Driver driver() { // <5>
		return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "secret"));
	}
	// end::java-config-imperative-short[]

	@Override
	protected Collection<String> getMappingBasePackages() { // <6>
		return Arrays.asList("your.domain.package");
	}

	// tag::java-config-imperative-short[]
}
// end::java-config-imperative[]
// end::java-config-imperative-short[]
