/*
 * Copyright 2011-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.queries.ogmgh551.AnotherThing;
import org.springframework.data.neo4j.queries.ogmgh551.ThingRepository;
import org.springframework.data.neo4j.queries.ogmgh551.ThingResult;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Those tests might have also fitted in {@link QueryReturnTypesTests} and {@link QueryIntegrationTests}, but those use
 * a very example like domain model, so we rather use a generic issue model here.
 *
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { QueryResultIntegrationTests.ContextConfig.class })
@RunWith(SpringRunner.class)
@Transactional
public class QueryResultIntegrationTests {

	@Autowired
	private GraphDatabaseService graphDatabaseService;

	@Autowired
	private ThingRepository thingRepository;

	@Before
	public void prepareTestDa() {

		try (Transaction tx = graphDatabaseService.beginTx()) {
			graphDatabaseService
					.execute("unwind range(1,10) as x with x create (n:ThingEntity {name: 'Thing ' + x}) return n");
			tx.success();
		}
	}

	@Test
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

	@Configuration
	@Neo4jIntegrationTest(repositoryPackages = "org.springframework.data.neo4j.queries.ogmgh551", domainPackages = "org.springframework.data.neo4j.queries.ogmgh551")
	static class ContextConfig {
	}
}
