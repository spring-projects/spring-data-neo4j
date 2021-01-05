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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.galaxy.GalaxyContextConfiguration;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.repo.WorldRepository;
import org.springframework.data.neo4j.queries.ogmgh552.Thing;
import org.springframework.data.neo4j.queries.ogmgh552.ThingRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = GalaxyContextConfiguration.class)
@RunWith(SpringRunner.class)
@Transactional
public class QueryReturnTypesTests {

	@Autowired GraphDatabaseService graphDatabaseService;

	@Autowired TransactionTemplate transactionTemplate;

	@Autowired WorldRepository worldRepository;

	@Autowired Session session;

	@Autowired ThingRepository thingRepository;

	@Before
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldCallExecuteWhenPrimitiveVoidReturnTypeOnQuery() {

		World world = new World("Tatooine", 0);

		worldRepository.save(world);
		worldRepository.touchAllWorlds();

		session.clear();
		world = worldRepository.findById(world.getId()).get();
		assertThat(world.getUpdated()).isNotNull();
	}

	@Test
	public void queryResultsAndEntitiesMappedToTheSameSimpleTypeShouldNotBeMixedUp() {

		List<Thing> result = this.thingRepository.findAllTheThings();
		assertThat(result).hasSize(1).extracting(Thing::getNotAName).containsExactly("NOT A NAME!!!");
	}

	@Test // DATAGRAPH-704
	public void shouldCallExecuteWhenVoidReturnTypeOnQuery() {

		World tatooine = new World("Tatooine", 0);
		World dagobah = new World("Dagobah", 0);

		tatooine.addRocketRouteTo(dagobah);

		worldRepository.save(tatooine);
		worldRepository.touchAllWorlds();

		session.clear();
		tatooine = worldRepository.findById(tatooine.getId()).get();

		assertThat(tatooine.getUpdated()).isNotNull();
		assertThat(tatooine.getReachableByRocket().size()).isEqualTo(1);

		for (World world : tatooine.getReachableByRocket()) {
			assertThat(world.getUpdated()).isNotNull();
		}
	}

	@Test
	public void shouldReturnStatisticsIfRequested() {

		World tatooine = new World("Tatooine", 0);

		worldRepository.save(tatooine);

		QueryStatistics stats = worldRepository.touchAllWorldsWithStatistics().queryStatistics();

		assertThat(stats.getPropertiesSet()).isEqualTo(1);
	}
}
