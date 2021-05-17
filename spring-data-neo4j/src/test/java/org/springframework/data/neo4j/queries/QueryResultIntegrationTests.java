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
package org.springframework.data.neo4j.queries;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.queries.gh1849.Cat;
import org.springframework.data.neo4j.queries.gh1849.Kennel;
import org.springframework.data.neo4j.queries.gh1849.KennelRepository;
import org.springframework.data.neo4j.queries.gh1849.Person;
import org.springframework.data.neo4j.queries.gh1849.Pet;
import org.springframework.data.neo4j.queries.ogmgh551.AnotherThing;
import org.springframework.data.neo4j.queries.ogmgh551.ThingRepository;
import org.springframework.data.neo4j.queries.ogmgh551.ThingResult;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Those tests might have also fitted in {@link QueryReturnTypesTests} and {@link QueryIntegrationTests}, but those use
 * a very example like domain model, so we rather use a generic issue model here.
 *
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { QueryResultIntegrationTests.ContextConfig.class })
@RunWith(SpringRunner.class)

public class QueryResultIntegrationTests {

	@Autowired
	private GraphDatabaseService graphDatabaseService;

	@Autowired
	private ThingRepository thingRepository;

	@Autowired
	private KennelRepository kennelRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Before
	public void prepareTestDa() {

		try (Transaction tx = graphDatabaseService.beginTx()) {
			graphDatabaseService.execute("MATCH (n) DETACH DELETE n");
			graphDatabaseService.execute("unwind range(1,10) as x with x create (n:ThingEntity {name: 'Thing ' + x}) return n");
			graphDatabaseService.execute("MERGE (p:Person {name:'Billy'})<-[:OWNED_BY]-(d:Dog {name: 'Ralph', breed: 'Muppet'})<-[:HOUSES]-(k:Kennel)");
			graphDatabaseService.execute("MERGE (p:Person {name:'Sally'})<-[:LIVES_WITH]-(c:Cat {name: 'Mittens', food: 'Birds'})<-[:HOUSES]-(k:Kennel)");
			tx.success();
		}
	}

	@Test
	@Transactional
	public void queryResultsShouldWorkWithNestedObjectsFromMap() {

		List<ThingResult> thingResults = this.thingRepository.findResults();
		assertThat(thingResults).hasSize(1);

		ThingResult thingResult = thingResults.get(0);
		assertThat(thingResult.getSomething()).isEqualTo("das ist ein Test");
		assertThat(thingResult.getThings())
				.hasSize(10)
				.extracting(AnotherThing::getName)
				.allSatisfy(s -> s.startsWith("Thing"));
	}

	@Test // GH-1849
	public void polymorphicQueryShouldIncludeAllRelTypes() {


		for (int depth : new int[] { -1, 10 }) {
			Iterable<Kennel> kennels = kennelRepository.findAll(depth);
			assertThat(kennels).hasSize(2);
			assertThat(kennels).allSatisfy(kennel -> {
				Pet pet = kennel.getPet();
				String expectedName = pet instanceof Cat ? "Mittens" : "Ralph";
				String expectedPerson = pet instanceof Cat ? "Sally" : "Billy";
				assertThat(pet).extracting(Pet::getName).isEqualTo(expectedName);
				assertThat(pet.getPerson()).isNotNull().extracting(Person::getName).isEqualTo(expectedPerson);
				System.out.println("what?");
			});
		}
	}

	@Configuration
	@Neo4jIntegrationTest(
			repositoryPackages = { "org.springframework.data.neo4j.queries.ogmgh551",
					"org.springframework.data.neo4j.queries.gh1849" },
			domainPackages = { "org.springframework.data.neo4j.queries.ogmgh551",
					"org.springframework.data.neo4j.queries.gh1849" }
	)
	static class ContextConfig {
	}
}
