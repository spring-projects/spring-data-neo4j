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

import static org.junit.Assert.*;

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
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
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

		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		// FIXME : bug here - content is always null
		// assertNotNull(page.getContent().get(0).getName());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	@Test // DATAGRAPH-893
	@Transactional
	public void shouldFindSlicedQueryResults() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		Slice<CinemaQueryResult> page = cinemaRepository.getSlicedCinemaQueryResults(pageable);

		assertEquals(3, page.getNumberOfElements());
		// FIXME : bug here - content is always null
		// assertNotNull(page.getContent().get(0).getName());
		assertTrue(page.hasNext());

		page = cinemaRepository.getSlicedCinemaQueryResults(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());

		page = cinemaRepository.getSlicedCinemaQueryResults(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());

		page = cinemaRepository.getSlicedCinemaQueryResults(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
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

		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		// FIXME : bug here - content is always null
		// assertNotNull(page.getContent().get(0).getName());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	@Test
	@Transactional
	public void shouldUseQueryParametersInCountQuery() {
		setup();
		Pageable pageable = PageRequest.of(0, 5);
		Page<Cinema> page = cinemaRepository.getPagedCinemasByCityWithPageCount("London", pageable);
		assertEquals(5, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); // With a count query, the total elements should equal the number
																								// returned by the count query
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemasByCityWithPageCount("London", page.nextPageable());
		assertEquals(5, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); // With a count query, the total elements should equal the number
																								// returned by the count query
		assertFalse(page.hasNext()); // with a count query, the next page calculation is correct
	}

	@Test
	@Transactional
	public void shouldFindSlicedCinemas() {
		setup();
		Pageable pageable = PageRequest.of(0, 3);
		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertNotNull(slice.getContent().get(0).getName());
		assertEquals(3, slice.getNumberOfElements());
		assertTrue(slice.hasNext());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(3, slice.getNumberOfElements());
		assertTrue(slice.hasNext());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(3, slice.getNumberOfElements());
		assertTrue(slice.hasNext());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(1, slice.getNumberOfElements());
		assertFalse(slice.hasNext());
	}

	@Test
	@Transactional
	public void shouldCorrectlyCalculateWhetherNextSliceExists() {
		setup();
		Pageable pageable = PageRequest.of(0, 5);
		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertEquals(5, slice.getNumberOfElements());
		assertTrue(slice.hasNext());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(5, slice.getNumberOfElements());
		assertFalse(slice.hasNext());
	}

	@Test // DATAGRAPH-887
	@Transactional
	public void shouldFindPagedAndSortedCinemas() {
		setup();
		Pageable pageable = PageRequest.of(0, 4, Sort.by("name").ascending());

		Page<Cinema> page = cinemaRepository.findByLocation("London", pageable);
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements());
		assertEquals("Cineplex", page.getContent().get(0).getName());
		assertEquals("Inox", page.getContent().get(1).getName());
		assertEquals("Landmark", page.getContent().get(2).getName());
		assertEquals("Metro", page.getContent().get(3).getName());

		page = cinemaRepository.findByLocation("London", page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements());
		assertEquals("Movietime", page.getContent().get(0).getName());
		assertEquals("PVR", page.getContent().get(1).getName());
		assertEquals("Picturehouse", page.getContent().get(2).getName());
		assertEquals("Rainbow", page.getContent().get(3).getName());

		page = cinemaRepository.findByLocation("London", page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertFalse(page.hasNext());
		assertEquals(10, page.getTotalElements());
		assertEquals("Regal", page.getContent().get(0).getName());
		assertEquals("Ritzy", page.getContent().get(1).getName());
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
				assertEquals(1, page.getNumberOfElements());
				assertEquals("Ritzy", page.getContent().get(0).getName());

				page = cinemaRepository.findByLocationAndVisitedName("London", "Michal",
						PageRequest.of(1, 1, Sort.Direction.DESC, "name"));
				assertEquals(1, page.getNumberOfElements());
				assertEquals("Picturehouse", page.getContent().get(0).getName());
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
				assertEquals(1, page.getNumberOfElements());
				assertEquals("Regal", page.getContent().get(0).getName());

				page = cinemaRepository.findByVisitedName("Michal", PageRequest.of(1, 1, Sort.Direction.DESC, "location"));
				assertEquals(1, page.getNumberOfElements());
				assertEquals("Regal", page.getContent().get(0).getName());
			}
		});
	}

	@Test // DATAGRAPH-887
	@Transactional
	public void shouldFindSortedCinemas() {
		setup();
		Sort sort = Sort.by(Sort.Direction.ASC, "name");
		List<Cinema> cinemas = cinemaRepository.findByLocation("London", sort);
		assertEquals(10, cinemas.size());
		assertEquals("Cineplex", cinemas.get(0).getName());
		assertEquals("Inox", cinemas.get(1).getName());
		assertEquals("Landmark", cinemas.get(2).getName());
		assertEquals("Metro", cinemas.get(3).getName());
		assertEquals("Movietime", cinemas.get(4).getName());
		assertEquals("PVR", cinemas.get(5).getName());
		assertEquals("Picturehouse", cinemas.get(6).getName());
		assertEquals("Rainbow", cinemas.get(7).getName());
		assertEquals("Regal", cinemas.get(8).getName());
		assertEquals("Ritzy", cinemas.get(9).getName());
	}

	@Test // DATAGRAPH-887
	@Transactional
	public void shouldFindPagedAndSortedCinemasByCapacity() {
		setup();

		Pageable pageable = PageRequest.of(0, 4, Sort.by("name").ascending());

		Page<Cinema> page = cinemaRepository.findByCapacity(500, pageable);
		assertEquals(4, page.getNumberOfElements());
		assertEquals("Cineplex", page.getContent().get(0).getName());
		assertEquals("Inox", page.getContent().get(1).getName());
		assertEquals("Landmark", page.getContent().get(2).getName());
		assertEquals("Metro", page.getContent().get(3).getName());

		pageable = PageRequest.of(1, 4, Sort.Direction.ASC, "name");
		page = cinemaRepository.findByCapacity(500, pageable);
		assertEquals(4, page.getContent().size());
		assertEquals("Movietime", page.getContent().get(0).getName());
		assertEquals("PVR", page.getContent().get(1).getName());
		assertEquals("Picturehouse", page.getContent().get(2).getName());
		assertEquals("Rainbow", page.getContent().get(3).getName());

		pageable = PageRequest.of(2, 4, Sort.Direction.ASC, "name");
		page = cinemaRepository.findByCapacity(500, pageable);
		assertEquals(2, page.getContent().size());
		assertEquals("Regal", page.getContent().get(0).getName());
		assertEquals("Ritzy", page.getContent().get(1).getName());
	}

	@Test // DATAGRAPH-653
	@Transactional
	public void shouldFindPagedCinemasSortedWithCustomQuery() {
		setup();
		Pageable pageable = PageRequest.of(0, 4, Sort.by("n.name").ascending());

		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate
		assertEquals("Cineplex", page.getContent().get(0).getName());
		assertEquals("Inox", page.getContent().get(1).getName());
		assertEquals("Landmark", page.getContent().get(2).getName());
		assertEquals("Metro", page.getContent().get(3).getName());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate
		assertEquals("Movietime", page.getContent().get(0).getName());
		assertEquals("PVR", page.getContent().get(1).getName());
		assertEquals("Picturehouse", page.getContent().get(2).getName());
		assertEquals("Rainbow", page.getContent().get(3).getName());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertFalse(page.hasNext());
		assertEquals(10, page.getTotalElements()); // this should not be relied on as incorrect as the total elements is an
																								// estimate
		assertEquals("Regal", page.getContent().get(0).getName());
		assertEquals("Ritzy", page.getContent().get(1).getName());
	}

	@Test // DATAGRAPH-653
	@Transactional
	public void shouldFindSlicedCinemasSortedWithCustomQuery() {
		setup();
		Pageable pageable = PageRequest.of(0, 4, Sort.by("n.name").ascending());

		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertEquals(4, slice.getNumberOfElements());
		assertTrue(slice.hasNext());
		assertEquals("Cineplex", slice.getContent().get(0).getName());
		assertEquals("Inox", slice.getContent().get(1).getName());
		assertEquals("Landmark", slice.getContent().get(2).getName());
		assertEquals("Metro", slice.getContent().get(3).getName());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(4, slice.getNumberOfElements());
		assertTrue(slice.hasNext());
		assertEquals("Movietime", slice.getContent().get(0).getName());
		assertEquals("PVR", slice.getContent().get(1).getName());
		assertEquals("Picturehouse", slice.getContent().get(2).getName());
		assertEquals("Rainbow", slice.getContent().get(3).getName());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(2, slice.getNumberOfElements());
		assertFalse(slice.hasNext());
		assertEquals("Regal", slice.getContent().get(0).getName());
		assertEquals("Ritzy", slice.getContent().get(1).getName());
	}

	@Test // DATAGRAPH-653
	@Transactional
	public void shouldFindCinemasSortedByNameWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Direction.ASC, "n.name");
		List<Cinema> cinemas = cinemaRepository.getCinemasSortedByName(sort);
		assertEquals(10, cinemas.size());
		assertEquals("Cineplex", cinemas.get(0).getName());
		assertEquals("Inox", cinemas.get(1).getName());
		assertEquals("Landmark", cinemas.get(2).getName());
		assertEquals("Metro", cinemas.get(3).getName());
		assertEquals("Movietime", cinemas.get(4).getName());
		assertEquals("PVR", cinemas.get(5).getName());
		assertEquals("Picturehouse", cinemas.get(6).getName());
		assertEquals("Rainbow", cinemas.get(7).getName());
		assertEquals("Regal", cinemas.get(8).getName());
		assertEquals("Ritzy", cinemas.get(9).getName());
	}

	@Test // DATAGRAPH-1290
	@Transactional
	public void shouldFindPagedCinemasSortedCaseInsensitiveWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Order.asc("n.name").ignoreCase());
		Pageable pageable = PageRequest.of(0, 4, sort);

		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals("Cineplex", page.getContent().get(0).getName());
		assertEquals("Inox", page.getContent().get(1).getName());
		assertEquals("Landmark", page.getContent().get(2).getName());
		assertEquals("Metro", page.getContent().get(3).getName());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals("Movietime", page.getContent().get(0).getName());
		assertEquals("Picturehouse", page.getContent().get(1).getName());
		assertEquals("PVR", page.getContent().get(2).getName());
		assertEquals("Rainbow", page.getContent().get(3).getName());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertFalse(page.hasNext());
		assertEquals("Regal", page.getContent().get(0).getName());
		assertEquals("Ritzy", page.getContent().get(1).getName());
	}

	@Test // DATAGRAPH-1290
	@Transactional
	public void shouldFindSlicedCinemasSortedCaseInsensitiveWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Order.asc("n.name").ignoreCase());
		Pageable pageable = PageRequest.of(0, 4, sort);

		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertEquals(4, slice.getNumberOfElements());
		assertTrue(slice.hasNext());
		assertEquals("Cineplex", slice.getContent().get(0).getName());
		assertEquals("Inox", slice.getContent().get(1).getName());
		assertEquals("Landmark", slice.getContent().get(2).getName());
		assertEquals("Metro", slice.getContent().get(3).getName());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(4, slice.getNumberOfElements());
		assertTrue(slice.hasNext());
		assertEquals("Movietime", slice.getContent().get(0).getName());
		assertEquals("Picturehouse", slice.getContent().get(1).getName());
		assertEquals("PVR", slice.getContent().get(2).getName());
		assertEquals("Rainbow", slice.getContent().get(3).getName());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(2, slice.getNumberOfElements());
		assertFalse(slice.hasNext());
		assertEquals("Regal", slice.getContent().get(0).getName());
		assertEquals("Ritzy", slice.getContent().get(1).getName());
	}

	@Test // DATAGRAPH-1290
	@Transactional
	public void shouldFindCinemasSortedCaseInsensitiveByNameWithCustomQuery() {
		setup();
		Sort sort = Sort.by(Sort.Order.asc("n.name").ignoreCase());
		List<Cinema> cinemas = cinemaRepository.getCinemasSortedByName(sort);
		assertEquals(10, cinemas.size());
		assertEquals("Cineplex", cinemas.get(0).getName());
		assertEquals("Inox", cinemas.get(1).getName());
		assertEquals("Landmark", cinemas.get(2).getName());
		assertEquals("Metro", cinemas.get(3).getName());
		assertEquals("Movietime", cinemas.get(4).getName());
		assertEquals("Picturehouse", cinemas.get(5).getName());
		assertEquals("PVR", cinemas.get(6).getName());
		assertEquals("Rainbow", cinemas.get(7).getName());
		assertEquals("Regal", cinemas.get(8).getName());
		assertEquals("Ritzy", cinemas.get(9).getName());
	}
}
