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
package org.springframework.data.neo4j.integration.issues.gh2906;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
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
public class Gh2906IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	interface FromRepository extends Neo4jRepository<BugFrom, String> {
	}

	interface ToRepository extends Neo4jRepository<BugTargetBase, String> {
	}

	@BeforeEach
	void clearDatabase(@Autowired Driver driver) {
		driver.executableQuery("MATCH (n) DETACH DELETE n").execute();
	}

	@Test
	void storeFromRootAggregateToLeaf(@Autowired ToRepository toRepository, @Autowired Driver driver) {
		var to1 = new BugTarget("T1", "BUG");

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		var from2 = new BugFrom("F2", "F2<-T1", to1);
		var from3 = new BugFrom("F3", "F3<-T1", to1);

		to1.relatedBugs = Set.of(
				new OutgoingBugRelationship(from1.reli.comment, from1),
				new OutgoingBugRelationship(from2.reli.comment, from2),
				new OutgoingBugRelationship(from3.reli.comment, from3)
		);
		toRepository.save(to1);

		assertGraph(driver);
	}


	@Test
	void storeFromRootAggregateToContainer(@Autowired ToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		var from2 = new BugFrom("F2", "F2<-T1", to1);
		var from3 = new BugFrom("F3", "F3<-T1", to1);

		to1.relatedBugs = Set.of(
				new OutgoingBugRelationship(from1.reli.comment, from1),
				new OutgoingBugRelationship(from2.reli.comment, from2),
				new OutgoingBugRelationship(from3.reli.comment, from3)
		);
		toRepository.save(to1);

		assertGraph(driver);
	}

	@Test
	void saveSingleEntitiesToLeaf(@Autowired FromRepository fromRepository, @Autowired ToRepository toRepository, @Autowired Driver driver) {

		var to1 = new BugTarget("T1", "BUG");
		to1 = toRepository.save(to1);

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from1.reli.comment, from1));
		from1 = fromRepository.save(from1);

		assertThat(from1.reli.id).isNotNull();
		assertThat(from1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		var from2 = new BugFrom("F2", "F2<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from2.reli.comment, from2));

		var from3 = new BugFrom("F3", "F3<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from3.reli.comment, from3));

		// See above
		var bugs = fromRepository.saveAll(List.of(from1, from2, from3));
		for (BugFrom from : bugs) {
			assertThat(from.reli.id).isNotNull();
			assertThat(from.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();
		}

		assertGraph(driver);
	}


	@Test
	void saveSingleEntitiesToContainer(@Autowired FromRepository fromRepository, @Autowired ToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		to1 = toRepository.save(to1);

		var from1 = new BugFrom("F1", "F1<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from1.reli.comment, from1));

		var from2 = new BugFrom("F2", "F2<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from2.reli.comment, from2));

		var from3 = new BugFrom("F3", "F3<-T1", to1);
		to1.relatedBugs.add(new OutgoingBugRelationship(from3.reli.comment, from3));

		// See above
		fromRepository.saveAll(List.of(from1, from2, from3));

		assertGraph(driver);
	}

	@Test
	void saveSingleEntitiesViaServiceToContainer(@Autowired FromRepository fromRepository, @Autowired ToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		to1 = toRepository.save(to1);
		var uuid = to1.uuid;
		to1 = null;

		var from1 = new BugFrom("F1", "F1<-T1", null);
		from1 = saveEntity(from1, uuid, fromRepository, toRepository);

		var from2 = new BugFrom("F2", "F2<-T1", null);
		from2 = saveEntity(from2, uuid, fromRepository, toRepository);

		var from3 = new BugFrom("F3", "F3<-T1", null);
		from3 = saveEntity(from3, uuid, fromRepository, toRepository);

		assertGraph(driver);
	}

	@Test
	void saveTwoSingleEntitiesViaServiceToContainer(@Autowired FromRepository fromRepository, @Autowired ToRepository toRepository, @Autowired Driver driver) {

		var t1 = new BugTarget("T1", "BUG");
		var t2 = new BugTarget("T2", "BUG");

		var to1 = new BugTargetContainer("C1");
		to1.items.add(t1);
		to1.items.add(t2);

		to1 = toRepository.save(to1);
		var uuid = to1.uuid;
		to1 = null;

		var from1 = new BugFrom("F1", "F1<-T1", null);
		from1 = saveEntity(from1, uuid, fromRepository, toRepository);

		var from2 = new BugFrom("F2", "F2<-T1", null);
		from2 = saveEntity(from2, uuid, fromRepository, toRepository);

		assertGraph(driver, 2);
	}

	@Test
	void saveSingleEntitiesViaServiceToLeaf(@Autowired FromRepository fromRepository, @Autowired ToRepository toRepository, @Autowired Driver driver) {

		var uuid = toRepository.save(new BugTarget("T1", "BUG")).uuid;

		var e1 = saveEntity(new BugFrom("F1", "F1<-T1", null), uuid, fromRepository, toRepository);

		assertThat(e1.reli.id).isNotNull();
		assertThat(e1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		e1 = saveEntity(new BugFrom("F2", "F2<-T1", null), uuid, fromRepository, toRepository);
		assertThat(e1.reli.id).isNotNull();
		assertThat(e1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		e1 = saveEntity(new BugFrom("F3", "F3<-T1", null), uuid, fromRepository, toRepository);
		assertThat(e1.reli.id).isNotNull();
		assertThat(e1.reli.target.relatedBugs).first().extracting(r -> r.id).isNotNull();

		assertGraph(driver);
	}

	@Test
	void saveTwoSingleEntitiesViaServiceToLeaf(@Autowired FromRepository fromRepository, @Autowired ToRepository toRepository, @Autowired Driver driver) {

		var to1 = new BugTarget("T1", "BUG");
		to1 = toRepository.save(to1);
		var uuid = to1.uuid;
		to1 = null;

		var from1 = new BugFrom("F1", "F1<-T1", null);
		from1 = saveEntity(from1, uuid, fromRepository, toRepository);

		var from2 = new BugFrom("F2", "F2<-T1", null);
		from2 = saveEntity(from2, uuid, fromRepository, toRepository);

		assertGraph(driver, 2);
	}

	/**
	 * This is the actual core use-case; a new from is attached to the target node that
	 * is referenced via its uuid and the stored from node is returned to the caller.
	 */
	BugFrom saveEntity(BugFrom from, String uuid, FromRepository fromRepository, ToRepository toRepository) {
		var to = toRepository.findById(uuid).orElseThrow();

		from.reli.target = to;
		to.relatedBugs.add(new OutgoingBugRelationship(from.reli.comment, from));

		return fromRepository.save(from);
	}

	private static void assertGraph(Driver driver) {
		assertGraph(driver, 3);
	}

	private static void assertGraph(Driver driver, int cnt) {

		var expectedNodes = IntStream.rangeClosed(1, cnt).mapToObj(i -> String.format("F%d", i)).toArray(String[]::new);
		var expectedRelationships = IntStream.rangeClosed(1, cnt).mapToObj(i -> String.format("F%d<-T1", i)).toArray(String[]::new);

		var result = driver.executableQuery("MATCH (t:BugTargetBase) -[r:RELI] ->(f:BugFrom) RETURN t, collect(f) AS f, collect(r) AS r").execute().records();
		assertThat(result)
				.hasSize(1)
				.element(0).satisfies(r -> {
					assertThat(r.get("t")).matches(TypeSystem.getDefault().NODE()::isTypeOf);
					assertThat(r.get("f"))
							.matches(TypeSystem.getDefault().LIST()::isTypeOf)
							.extracting(Value::asList, as(InstanceOfAssertFactories.LIST))
							.map(node -> ((Node) node).get("name").asString())
							.containsExactlyInAnyOrder(expectedNodes);
					assertThat(r.get("r"))
							.matches(TypeSystem.getDefault().LIST()::isTypeOf)
							.extracting(Value::asList, as(InstanceOfAssertFactories.LIST))
							.map(rel -> ((Relationship) rel).get("comment").asString())
							.containsExactlyInAnyOrder(expectedRelationships);
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
