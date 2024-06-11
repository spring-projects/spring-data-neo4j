/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2905;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class Gh2905IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	interface FromRepository extends Neo4jRepository<BugFromV1, String> {

	}

	interface ToRepository extends Neo4jRepository<BugTargetV1, String> {

	}

	@BeforeEach
	void clearDatabase(@Autowired Driver driver) {
		driver.executableQuery("MATCH (n) DETACH DELETE n").execute();
	}

	@Test
	void storeFromRootAggregate(@Autowired ToRepository toRepository, @Autowired Driver driver) {
		var to1 = BugTargetV1.builder().name("T1").type("BUG").build();

		var from1 = BugFromV1.builder()
				.name("F1")
				.reli(BugRelationshipV1.builder().target(to1).comment("F1<-T1").build())
				.build();
		var from2 = BugFromV1.builder()
				.name("F2")
				.reli(BugRelationshipV1.builder().target(to1).comment("F2<-T1").build())
				.build();
		var from3 = BugFromV1.builder()
				.name("F3")
				.reli(BugRelationshipV1.builder().target(to1).comment("F3<-T1").build())
				.build();

		to1.relatedBugs = Set.of(from1, from2, from3);
		toRepository.save(to1);

		assertGraph(driver);
	}

	@Test
	void saveSingleEntities(@Autowired FromRepository fromRepository, @Autowired ToRepository toRepository, @Autowired Driver driver) {
		var to1 = BugTargetV1.builder().name("T1").type("BUG").build();
		to1.relatedBugs = new HashSet<>();
		to1 = toRepository.save(to1);

		var from1 = BugFromV1.builder()
				.name("F1")
				.reli(BugRelationshipV1.builder().target(to1).comment("F1<-T1").build())
				.build();
		// This is the key to solve 2905 when you had the annotation previously, you must maintain both ends of the bidirectional relationship.
		// SDN does not do this for you.
		to1.relatedBugs.add(from1);
		from1 = fromRepository.save(from1);

		var from2 = BugFromV1.builder()
				.name("F2")
				.reli(BugRelationshipV1.builder().target(to1).comment("F2<-T1").build())
				.build();
		// See above
		to1.relatedBugs.add(from2);

		var from3 = BugFromV1.builder()
				.name("F3")
				.reli(BugRelationshipV1.builder().target(to1).comment("F3<-T1").build())
				.build();
		to1.relatedBugs.add(from3);
		// See above
		fromRepository.saveAll(List.of(from1, from2, from3));

		assertGraph(driver);
	}

	private static void assertGraph(Driver driver) {
		var result = driver.executableQuery("MATCH (t:BugTargetV1) -[:RELI] ->(f:BugFromV1) RETURN t, collect(f) AS f").execute().records();
		assertThat(result)
				.hasSize(1)
				.element(0).satisfies(r -> {
					assertThat(r.get("t")).matches(TypeSystem.getDefault().NODE()::isTypeOf);
					assertThat(r.get("f"))
							.matches(TypeSystem.getDefault().LIST()::isTypeOf)
							.extracting(Value::asList, as(InstanceOfAssertFactories.LIST))
							.hasSize(3);
				});
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}


		@Override
		public boolean isCypher5Compatible() {
			return false;
		}
	}
}
