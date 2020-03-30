/*
 * Copyright (c) 2019-2020 "Neo4j,"
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

import static org.assertj.core.api.Assertions.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.springframework.data.core.ReactiveNeo4jTemplate;
import org.neo4j.springframework.data.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Integration tests for the reactive SDN/RX Neo4j test slice.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@ContextConfiguration(initializers = TestContainerInitializer.class)
@ReactiveDataNeo4jTest(excludeAutoConfiguration = Neo4jTestHarnessAutoConfiguration.class)
class ReactiveDataNeo4jTestIT {

	@Autowired
	private Driver driver;

	@Autowired
	private ReactiveNeo4jTemplate neo4jTemplate;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testTemplate() {

		Mono
			.just(new ExampleEntity("Look, new @ReactiveDataNeo4jTest!"))
			.flatMap(neo4jTemplate::save)
			.as(StepVerifier::create)
			.expectNextCount(1)
			.verifyComplete();

		try (Session session = driver
			.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.READ).build())) {
			long cnt = session.run("MATCH (n:ExampleEntity) RETURN count(n) as cnt").single().get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Test
	void didProvideOnlyReactiveTransactionManager() {

		assertThat(this.applicationContext.getBean(ReactiveTransactionManager.class)).isInstanceOf(
			ReactiveNeo4jTransactionManager.class);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(PlatformTransactionManager.class));
	}
}
