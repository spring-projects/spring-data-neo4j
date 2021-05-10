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
package org.springframework.data.neo4j.integration.multiple_ctx_imperative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.multiple_ctx_imperative.domain1.Domain1Config;
import org.springframework.data.neo4j.integration.multiple_ctx_imperative.domain1.Domain1Entity;
import org.springframework.data.neo4j.integration.multiple_ctx_imperative.domain1.Domain1Repository;
import org.springframework.data.neo4j.integration.multiple_ctx_imperative.domain2.Domain2Config;
import org.springframework.data.neo4j.integration.multiple_ctx_imperative.domain2.Domain2Entity;
import org.springframework.data.neo4j.integration.multiple_ctx_imperative.domain2.Domain2Repository;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests whether multiple context are truly separated.
 *
 * @author Michael J. Simons
 */
@SpringJUnitConfig(classes = { SharedConfig.class, Domain1Config.class, Domain2Config.class })
@Testcontainers(disabledWithoutDocker = true)
@Tag(Neo4jExtension.INCOMPATIBLE_WITH_CLUSTERS)
public class MultipleContextsIT {

	@Container
	private static Neo4jContainer container1 = new Neo4jContainer<>("neo4j:4.0")
			.withAdminPassword("secret1");

	@Container
	private static Neo4jContainer container2 = new Neo4jContainer<>("neo4j:4.0")
			.withAdminPassword("secret2");

	@DynamicPropertySource
	static void neo4jSettings(DynamicPropertyRegistry registry) {

		registry.add("database1.url", container1::getBoltUrl);
		registry.add("database1.password", () -> "secret1");

		registry.add("database2.url", container2::getBoltUrl);
		registry.add("database2.password", () -> "secret2");
	}

	@Test // DATAGRAPH-1441
	void repositoriesShouldTargetTheCorrectDatabase(
			@Autowired Domain1Repository repo1,
			@Autowired Domain2Repository repo2
	) {

		Domain1Entity newEntity1 = repo1.save(new Domain1Entity("For domain 1"));
		newEntity1.setAnAttribute(newEntity1.getAnAttribute() + " updated");
		newEntity1 = repo1.save(newEntity1);
		long id1 = newEntity1.getId();

		Domain2Entity newEntity2 = new Domain2Entity("For domain 2");
		newEntity2.setAnAttribute(newEntity2.getAnAttribute() + " updated");
		newEntity2 = repo2.save(newEntity2);
		long id2 = repo2.save(newEntity2).getId();

		try (Driver driver = newDriver(container1.getBoltUrl(), container1.getAdminPassword());
				Session session = driver.session()) {
			verifyExistenceAndVersion(id1, session);
		}

		try (Driver driver = newDriver(container2.getBoltUrl(), container2.getAdminPassword());
				Session session = driver.session()) {
			verifyExistenceAndVersion(id2, session);
		}
	}

	/**
	 * Create drivers independend from the setup under test.
	 *
	 * @param boltUrl  Where to connect to
	 * @param password Which password
	 * @return Minimal driver instance.
	 */
	private static Driver newDriver(String boltUrl, String password) {

		Config driverConfig = Config.builder()
				.withMaxConnectionPoolSize(1)
				.withLogging(Logging.none())
				.withEventLoopThreads(1)
				.build();
		return GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", password), driverConfig);
	}

	private static void verifyExistenceAndVersion(long id1, Session session) {
		Long version = session
				.readTransaction(tx -> tx.run("MATCH (n) WHERE id(n) = $id RETURN n.version", Collections
						.singletonMap("id", id1)).single().get(0).asLong());
		assertThat(version).isOne();
	}
}
