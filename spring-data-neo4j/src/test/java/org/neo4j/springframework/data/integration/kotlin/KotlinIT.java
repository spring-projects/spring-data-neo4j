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
package org.neo4j.springframework.data.integration.kotlin;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.KotlinPerson;
import org.neo4j.springframework.data.integration.shared.KotlinRepository;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension.Neo4jConnectionSupport;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class KotlinIT {

	private final static String PERSON_NAME = "test";

	private static Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	private KotlinRepository repository;

	@Autowired
	private Driver driver;

	@BeforeEach
	void setup() {
		Session session = driver.session();
		Transaction transaction = session.beginTransaction();
		transaction.run("MATCH (n) detach delete n");
		transaction.run("CREATE (n:KotlinPerson) SET n.name = $personName", Values.parameters("personName", PERSON_NAME));
		transaction.commit();
		transaction.close();
		session.close();
	}

	@Test
	void findAllKotlinPersons() {
		Iterable<KotlinPerson> person = repository.findAll();
		assertThat(person.iterator().next().getName()).isEqualTo(PERSON_NAME);
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = KotlinPerson.class)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		public Collection<String> getMappingBasePackages() {
			return Collections.singletonList(KotlinPerson.class.getPackage().getName());
		}
	}
}
