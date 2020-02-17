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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.harness.ServerControls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResultInterface;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See DATAGRAPH-680
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Mark Angrish
 * @author Nicolas Mervaillie
 * @author Ihor Dziuba
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringRunner.class)
public class PagedQueryTests {

	@Autowired private ServerControls neo4jTestServer;

	@Autowired private CinemaRepository cinemaRepository;

	@Autowired private TransactionTemplate transactionTemplate;

	@Before
	public void init() {
		neo4jTestServer.graph().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	public void setup() {
		String[] names = new String[] { "Picturehouse", "Regal", "Ritzy", "Metro", "Inox", "PVR", "Cineplex", "Landmark",
				"Rainbow", "Movietime" };
		for (String name : names) {
			Cinema cinema = new Cinema(name);
			cinema.setLocation("London");
			cinema.setCapacity(500);
			cinemaRepository.save(cinema);
		}
	}

	private void executeUpdate(String cypher) {
		neo4jTestServer.graph().execute(cypher);
	}

	@Test
	@Transactional
	public void shouldFindPagedCinemas() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10);

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10);

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10);

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

	@Test(expected = IllegalArgumentException.class)
	@Transactional
	public void shouldThrowExceptionIfCountQueryAbsent() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		cinemaRepository.getPagedCinemasWithoutCountQuery(pageable);
	}

	/**
	 * Repeats shouldFindPagedCinemas for query results - concrete classes.
	 */
	@Test // DATAGRAPH-893
	@Transactional
	public void shouldFindPagedQueryResults() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		Page<CinemaQueryResult> page = cinemaRepository.getPagedCinemaQueryResults(pageable);

		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		// FIXME : bug here - content is always null
		// assertNotNull(page.getContent().get(0).getName());
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

	@Test // DATAGRAPH-893
	@Transactional
	public void shouldFindSlicedQueryResults() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		Slice<CinemaQueryResult> page = cinemaRepository.getSlicedCinemaQueryResults(pageable);

		assertThat(page.getNumberOfElements()).isEqualTo(3);
		// FIXME : bug here - content is always null
		// assertNotNull(page.getContent().get(0).getName());
		assertThat(page.hasNext()).isTrue();

		page = cinemaRepository.getSlicedCinemaQueryResults(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();

		page = cinemaRepository.getSlicedCinemaQueryResults(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();

		page = cinemaRepository.getSlicedCinemaQueryResults(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

	/**
	 * Repeats shouldFindPagedCinemas for query results - interfaces * @see DATAGRAPH-893
	 */
	@Test
	@Transactional
	public void shouldFindPagedQueryInterfaceResults() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		Page<CinemaQueryResultInterface> page = cinemaRepository.getPagedCinemaQueryResultInterfaces(pageable);

		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		// FIXME : bug here - content is always null
		// assertNotNull(page.getContent().get(0).getName());
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	@Transactional
	public void shouldUseQueryParametersInCountQuery() {
		setup();
		Pageable pageable = PageRequest.of(0, 5);
		Page<Cinema> page = cinemaRepository.getPagedCinemasByCityWithPageCount("London", pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(5);
		assertThat(page.getTotalElements()).isEqualTo(10); // With a count query, the total elements should equal the number
																								// returned by the count query
		assertThat(page.hasNext()).isTrue();

		page = cinemaRepository.getPagedCinemasByCityWithPageCount("London", page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(5);
		assertThat(page.getTotalElements()).isEqualTo(10); // With a count query, the total elements should equal the number
																								// returned by the count query
		assertThat(page.hasNext()).isFalse(); // with a count query, the next page calculation is correct
	}

	@Test
	@Transactional
	public void shouldFindSlicedCinemas() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertThat(slice.getContent().get(0).getName()).isNotNull();
		assertThat(slice.getNumberOfElements()).isEqualTo(3);
		assertThat(slice.hasNext()).isTrue();

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(3);
		assertThat(slice.hasNext()).isTrue();

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(3);
		assertThat(slice.hasNext()).isTrue();

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(1);
		assertThat(slice.hasNext()).isFalse();
	}

	@Test
	@Transactional
	public void shouldCorrectlyCalculateWhetherNextSliceExists() {
		setup();
		Pageable pageable = PageRequest.of(0, 5);
		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertThat(slice.getNumberOfElements()).isEqualTo(5);
		assertThat(slice.hasNext()).isTrue();

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(5);
		assertThat(slice.hasNext()).isFalse();
	}

	@Test // DATAGRAPH-887
	@Transactional
	public void shouldFindPagedAndSortedCinemas() {
		setup();
		Pageable pageable = PageRequest.of(0, 4, Sort.by("name").ascending());

		Page<Cinema> page = cinemaRepository.findByLocation("London", pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Cineplex");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Inox");
		assertThat(page.getContent().get(2).getName()).isEqualTo("Landmark");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Metro");

		page = cinemaRepository.findByLocation("London", page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Movietime");
		assertThat(page.getContent().get(1).getName()).isEqualTo("PVR");
		assertThat(page.getContent().get(2).getName()).isEqualTo("Picturehouse");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Rainbow");

		page = cinemaRepository.findByLocation("London", page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(2);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.getTotalElements()).isEqualTo(10);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Regal");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-887
	public void shouldSortPageWhenNestedPropertyIsInvolved() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ "CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) "
				+ "CREATE (m:Theatre {name:'Picturehouse', city:'London', capacity: 5000}) "
				+ "CREATE (u:User {name:'Michal'}) " + "CREATE (u)-[:VISITED]->(r)  " + "CREATE (u)-[:VISITED]->(m)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Page<Cinema> page = cinemaRepository.findByLocationAndVisitedName("London", "Michal",
						PageRequest.of(0, 1, Sort.Direction.DESC, "name"));
				assertThat(page.getNumberOfElements()).isEqualTo(1);
				assertThat(page.getContent().get(0).getName()).isEqualTo("Ritzy");

				page = cinemaRepository.findByLocationAndVisitedName("London", "Michal",
						PageRequest.of(1, 1, Sort.Direction.DESC, "name"));
				assertThat(page.getNumberOfElements()).isEqualTo(1);
				assertThat(page.getContent().get(0).getName()).isEqualTo("Picturehouse");
			}
		});
	}

	@Test // DATAGRAPH-887
	public void shouldSortPageByNestedPropertyIsInvolved() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ "CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) "
				+ "CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 5000}) " + "CREATE (u:User {name:'Michal'}) "
				+ "CREATE (u)-[:VISITED]->(r)  " + "CREATE (u)-[:VISITED]->(m)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Page<Cinema> page = cinemaRepository.findByVisitedName("Michal",
						PageRequest.of(0, 1, Sort.Direction.ASC, "location"));
				assertThat(page.getNumberOfElements()).isEqualTo(1);
				assertThat(page.getContent().get(0).getName()).isEqualTo("Regal");

				page = cinemaRepository.findByVisitedName("Michal", PageRequest.of(1, 1, Sort.Direction.DESC, "location"));
				assertThat(page.getNumberOfElements()).isEqualTo(1);
				assertThat(page.getContent().get(0).getName()).isEqualTo("Regal");
			}
		});
	}

	@Test // DATAGRAPH-887
	@Transactional
	public void shouldFindSortedCinemas() {
		setup();
		Sort sort = Sort.by(Sort.Direction.ASC, "name");
		List<Cinema> cinemas = cinemaRepository.findByLocation("London", sort);
		assertThat(cinemas.size()).isEqualTo(10);
		assertThat(cinemas.get(0).getName()).isEqualTo("Cineplex");
		assertThat(cinemas.get(1).getName()).isEqualTo("Inox");
		assertThat(cinemas.get(2).getName()).isEqualTo("Landmark");
		assertThat(cinemas.get(3).getName()).isEqualTo("Metro");
		assertThat(cinemas.get(4).getName()).isEqualTo("Movietime");
		assertThat(cinemas.get(5).getName()).isEqualTo("PVR");
		assertThat(cinemas.get(6).getName()).isEqualTo("Picturehouse");
		assertThat(cinemas.get(7).getName()).isEqualTo("Rainbow");
		assertThat(cinemas.get(8).getName()).isEqualTo("Regal");
		assertThat(cinemas.get(9).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-887
	@Transactional
	public void shouldFindPagedAndSortedCinemasByCapacity() {
		setup();

		Pageable pageable = PageRequest.of(0, 4, Sort.by("name").ascending());

		Page<Cinema> page = cinemaRepository.findByCapacity(500, pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Cineplex");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Inox");
		assertThat(page.getContent().get(2).getName()).isEqualTo("Landmark");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Metro");

		pageable = PageRequest.of(1, 4, Sort.Direction.ASC, "name");
		page = cinemaRepository.findByCapacity(500, pageable);
		assertThat(page.getContent().size()).isEqualTo(4);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Movietime");
		assertThat(page.getContent().get(1).getName()).isEqualTo("PVR");
		assertThat(page.getContent().get(2).getName()).isEqualTo("Picturehouse");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Rainbow");

		pageable = PageRequest.of(2, 4, Sort.Direction.ASC, "name");
		page = cinemaRepository.findByCapacity(500, pageable);
		assertThat(page.getContent().size()).isEqualTo(2);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Regal");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-653
	@Transactional
	public void shouldFindPagedCinemasSortedWithCustomQuery() {
		setup();
		Pageable pageable = PageRequest.of(0, 4, Sort.by("n.name").ascending());

		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate
		assertThat(page.getContent().get(0).getName()).isEqualTo("Cineplex");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Inox");
		assertThat(page.getContent().get(2).getName()).isEqualTo("Landmark");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Metro");

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate
		assertThat(page.getContent().get(0).getName()).isEqualTo("Movietime");
		assertThat(page.getContent().get(1).getName()).isEqualTo("PVR");
		assertThat(page.getContent().get(2).getName()).isEqualTo("Picturehouse");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Rainbow");

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(2);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.getTotalElements()).isEqualTo(10); // this should not be relied on as incorrect as the total elements is an
																								// estimate
		assertThat(page.getContent().get(0).getName()).isEqualTo("Regal");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-653
	@Transactional
	public void shouldFindSlicedCinemasSortedWithCustomQuery() {
		setup();
		Pageable pageable = PageRequest.of(0, 4, Sort.by("n.name").ascending());

		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertThat(slice.getNumberOfElements()).isEqualTo(4);
		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getContent().get(0).getName()).isEqualTo("Cineplex");
		assertThat(slice.getContent().get(1).getName()).isEqualTo("Inox");
		assertThat(slice.getContent().get(2).getName()).isEqualTo("Landmark");
		assertThat(slice.getContent().get(3).getName()).isEqualTo("Metro");

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(4);
		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getContent().get(0).getName()).isEqualTo("Movietime");
		assertThat(slice.getContent().get(1).getName()).isEqualTo("PVR");
		assertThat(slice.getContent().get(2).getName()).isEqualTo("Picturehouse");
		assertThat(slice.getContent().get(3).getName()).isEqualTo("Rainbow");

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(2);
		assertThat(slice.hasNext()).isFalse();
		assertThat(slice.getContent().get(0).getName()).isEqualTo("Regal");
		assertThat(slice.getContent().get(1).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-653
	@Transactional
	public void shouldFindCinemasSortedByNameWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Direction.ASC, "n.name");
		List<Cinema> cinemas = cinemaRepository.getCinemasSortedByName(sort);
		assertThat(cinemas.size()).isEqualTo(10);
		assertThat(cinemas.get(0).getName()).isEqualTo("Cineplex");
		assertThat(cinemas.get(1).getName()).isEqualTo("Inox");
		assertThat(cinemas.get(2).getName()).isEqualTo("Landmark");
		assertThat(cinemas.get(3).getName()).isEqualTo("Metro");
		assertThat(cinemas.get(4).getName()).isEqualTo("Movietime");
		assertThat(cinemas.get(5).getName()).isEqualTo("PVR");
		assertThat(cinemas.get(6).getName()).isEqualTo("Picturehouse");
		assertThat(cinemas.get(7).getName()).isEqualTo("Rainbow");
		assertThat(cinemas.get(8).getName()).isEqualTo("Regal");
		assertThat(cinemas.get(9).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-1290
	@Transactional
	public void shouldFindPagedCinemasSortedCaseInsensitiveWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Order.asc("n.name").ignoreCase());
		Pageable pageable = PageRequest.of(0, 4, sort);

		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getContent().get(0).getName()).isEqualTo("Cineplex");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Inox");
		assertThat(page.getContent().get(2).getName()).isEqualTo("Landmark");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Metro");

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.getContent().get(0).getName()).isEqualTo("Movietime");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Picturehouse");
		assertThat(page.getContent().get(2).getName()).isEqualTo("PVR");
		assertThat(page.getContent().get(3).getName()).isEqualTo("Rainbow");

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertThat(page.getNumberOfElements()).isEqualTo(2);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.getContent().get(0).getName()).isEqualTo("Regal");
		assertThat(page.getContent().get(1).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-1290
	@Transactional
	public void shouldFindSlicedCinemasSortedCaseInsensitiveWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Order.asc("n.name").ignoreCase());
		Pageable pageable = PageRequest.of(0, 4, sort);

		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertThat(slice.getNumberOfElements()).isEqualTo(4);
		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getContent().get(0).getName()).isEqualTo("Cineplex");
		assertThat(slice.getContent().get(1).getName()).isEqualTo("Inox");
		assertThat(slice.getContent().get(2).getName()).isEqualTo("Landmark");
		assertThat(slice.getContent().get(3).getName()).isEqualTo("Metro");

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(4);
		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getContent().get(0).getName()).isEqualTo("Movietime");
		assertThat(slice.getContent().get(1).getName()).isEqualTo("Picturehouse");
		assertThat(slice.getContent().get(2).getName()).isEqualTo("PVR");
		assertThat(slice.getContent().get(3).getName()).isEqualTo("Rainbow");

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertThat(slice.getNumberOfElements()).isEqualTo(2);
		assertThat(slice.hasNext()).isFalse();
		assertThat(slice.getContent().get(0).getName()).isEqualTo("Regal");
		assertThat(slice.getContent().get(1).getName()).isEqualTo("Ritzy");
	}

	@Test // DATAGRAPH-1290
	@Transactional
	public void shouldFindCinemasSortedCaseInsensitiveByNameWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Order.asc("n.name").ignoreCase());
		List<Cinema> cinemas = cinemaRepository.getCinemasSortedByName(sort);
		assertThat(cinemas.size()).isEqualTo(10);
		assertThat(cinemas.get(0).getName()).isEqualTo("Cineplex");
		assertThat(cinemas.get(1).getName()).isEqualTo("Inox");
		assertThat(cinemas.get(2).getName()).isEqualTo("Landmark");
		assertThat(cinemas.get(3).getName()).isEqualTo("Metro");
		assertThat(cinemas.get(4).getName()).isEqualTo("Movietime");
		assertThat(cinemas.get(5).getName()).isEqualTo("Picturehouse");
		assertThat(cinemas.get(6).getName()).isEqualTo("PVR");
		assertThat(cinemas.get(7).getName()).isEqualTo("Rainbow");
		assertThat(cinemas.get(8).getName()).isEqualTo("Regal");
		assertThat(cinemas.get(9).getName()).isEqualTo("Ritzy");
	}
}
