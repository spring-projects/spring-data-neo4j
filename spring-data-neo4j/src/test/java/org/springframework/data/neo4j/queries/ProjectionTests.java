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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.harness.ServerControls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.examples.movies.domain.CinemaAndBlockbuster;
import org.springframework.data.neo4j.examples.movies.domain.CinemaAndBlockbusterName;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@TestExecutionListeners(listeners = ProjectionTests.PrepareAndCleanDatabase.class, mergeMode = MERGE_WITH_DEFAULTS)
@RunWith(SpringRunner.class)
public class ProjectionTests {

	static class PrepareAndCleanDatabase implements TestExecutionListener {
		public void beforeTestClass(TestContext testContext) {

			GraphDatabaseService graphDatabaseService = testContext.getApplicationContext().getBean(ServerControls.class)
					.graph();

			graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
			graphDatabaseService.execute("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
					+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " + " CREATE (u:User {name:'Michal'}) "
					+ " CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" + " CREATE (m1:Movie {name:'San Andreas'}) "
					+ " CREATE (m2:Movie {name:'Pitch Perfect 2'})" + " CREATE (p)-[:BLOCKBUSTER]->(m1)"
					+ " CREATE (p)-[:SHOWS]->(m1)" + " CREATE (p)-[:SHOWS]->(m2)" + " CREATE (r)-[:BLOCKBUSTER]->(m2)"
					+ " CREATE (r)-[:SHOWS]->(m1)" + " CREATE (r)-[:SHOWS]->(m2)" + " CREATE (u)-[:RATED {stars :3}]->(m1)");
		}

		@Override
		public void afterTestClass(TestContext testContext) {
			GraphDatabaseService graphDatabaseService = testContext.getApplicationContext().getBean(ServerControls.class)
					.graph();
			graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
		}
	}

	@Autowired private CinemaRepository cinemaRepository;

	@Test
	@Transactional
	public void shouldFindCinemasWithoutRatings() {

		List<CinemaAndBlockbuster> theatres = cinemaRepository.findByNameLike("Picture*");

		assertThat(theatres).hasSize(1);
		CinemaAndBlockbuster cinema = theatres.get(0);
		assertThat(cinema.getName()).isEqualTo("Picturehouse");
		assertThat(cinema.getLocation()).isEqualTo("London");
		assertThat(cinema.getBlockbusterOfTheWeek().getName()).isEqualTo("San Andreas");
		assertThat(cinema.getBlockbusterOfTheWeek().getRatings()).isEmpty();
	}

	@Test
	@Transactional
	public void shouldFindCinemasOrderByWithoutRatings() {

		List<CinemaAndBlockbuster> theatres = cinemaRepository.findByNameLike("*", Sort.by(Sort.Direction.ASC, "name"));

		assertThat(theatres).hasSize(2);
		assertThat(theatres).extracting("name").containsExactly("Picturehouse", "Ritzy");
		CinemaAndBlockbuster cinema = theatres.get(0);
		assertThat(cinema.getLocation()).isEqualTo("London");
		assertThat(cinema.getBlockbusterOfTheWeek().getName()).isEqualTo("San Andreas");
		assertThat(cinema.getBlockbusterOfTheWeek().getRatings()).isEmpty();

		theatres = cinemaRepository.findByNameLike("*", Sort.by(Sort.Direction.DESC, "name"));

		assertThat(theatres).hasSize(2);
		assertThat(theatres).extracting("name").containsExactly("Ritzy", "Picturehouse");
	}

	@Test
	@Transactional
	public void shouldFindCinemasWithoutUsersAndCustomDepth() {

		List<CinemaAndBlockbuster> cinemas = cinemaRepository.findByNameLike("Picture*", 0);

		assertThat(cinemas).hasSize(1);
		CinemaAndBlockbuster cinema = cinemas.get(0);
		assertThat(cinema.getName()).isEqualTo("Picturehouse");
		assertThat(cinema.getBlockbusterOfTheWeek()).isNull();
	}

	@Test
	@Transactional
	public void shouldFindCinemaWithOpenProjection() {

		List<CinemaAndBlockbusterName> cinemas = cinemaRepository.findByNameStartingWith("P");

		assertThat(cinemas).hasSize(1);
		CinemaAndBlockbusterName c1 = cinemas.get(0);
		assertThat(c1.getName()).isEqualTo("Picturehouse");
		assertThat(c1.blockBusterOfTheWeekName()).isEqualTo("San Andreas");
	}
}
