/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResultInterface;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Mark Angrish
 *
 * @see DATAGRAPH-680
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class PagedQueryIT extends MultiDriverTestClass {

	private static GraphDatabaseService graphDatabaseService;

	@Autowired
	private CinemaRepository cinemaRepository;

	@Autowired Session session;

	@BeforeClass
	public static void beforeClass() {
		graphDatabaseService = getGraphDatabaseService();
	}

	@Before
	public void init() {
		clearDatabase();
		String[] names = new String[]{"Picturehouse", "Regal", "Ritzy", "Metro", "Inox", "PVR", "Cineplex", "Landmark", "Rainbow", "Movietime"};
		for (String name : names) {
			Cinema cinema = new Cinema(name);
			cinema.setLocation("London");
			cinema.setCapacity(500);
			cinemaRepository.save(cinema);
		}
	}

	@After
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
		session.clear();
	}

	private void executeUpdate(String cypher) {
		graphDatabaseService.execute(cypher);
	}


	@Test
	public void shouldFindPagedCinemas() {
		Pageable pageable = new PageRequest(0, 3);
		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(6, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(9, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(12, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	/**
	 * Repeats shouldFindPagedCinemas for query results - concrete classes.
	 *
	 * @see DATAGRAPH-893
	 */
	@Test
	public void shouldFindPagedQueryResults() {
		Pageable pageable = new PageRequest(0, 3);
		Page<CinemaQueryResult> page = cinemaRepository.getPagedCinemaQueryResults(pageable);
		System.out.println(page);

		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(6, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(9, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(12, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemaQueryResults(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	/**
	 * @see DATAGRAPH-893
	 */
	@Test
	public void shouldFindSlicedQueryResults() {
		Pageable pageable = new PageRequest(0, 3);
		Slice<CinemaQueryResult> page = cinemaRepository.getSlicedCinemaQueryResults(pageable);

		assertEquals(3, page.getNumberOfElements());
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
	 * Repeats shouldFindPagedCinemas for query results - interfaces
	 * * @see DATAGRAPH-893
	 */
	@Test
	public void shouldFindPagedQueryInterfaceResults() {
		Pageable pageable = new PageRequest(0, 3);
		Page<CinemaQueryResultInterface> page = cinemaRepository.getPagedCinemaQueryResultInterfaces(pageable);
		System.out.println(page);

		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(6, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(9, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(12, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemaQueryResultInterfaces(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	@Test
	public void shouldNotRelyOnTotalElementsToFindPagedCinemas() {
		Pageable pageable = new PageRequest(0, 5);
		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertEquals(5, page.getNumberOfElements());
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(5, page.getNumberOfElements());
		assertTrue(page.hasNext()); //this is an estimate and so for the last page, page.hasNext() is true when the total number of elements / total number of pages=0

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(0, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	@Test
	public void shouldFindPagedCinemasWithAccurateTotalCount() {
		Pageable pageable = new PageRequest(0, 3);
		Page<Cinema> page = cinemaRepository.getPagedCinemasWithPageCount(pageable);
		assertEquals(3, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemasWithPageCount(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemasWithPageCount(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemasWithPageCount(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertFalse(page.hasNext());
	}

	@Test
	public void shouldRelyOnTotalElementsToFindPagedCinemasWithCountQuery() {
		Pageable pageable = new PageRequest(0, 5);
		Page<Cinema> page = cinemaRepository.getPagedCinemasWithPageCount(pageable);
		assertEquals(5, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemasWithPageCount(page.nextPageable());
		assertEquals(5, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertFalse(page.hasNext()); //with a count query, the next page calculation is correct
	}

	@Test
	public void shouldUseQueryParametersInCountQuery() {
		Pageable pageable = new PageRequest(0, 5);
		Page<Cinema> page = cinemaRepository.getPagedCinemasByCityWithPageCount("London", pageable);
		assertEquals(5, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemasByCityWithPageCount("London", page.nextPageable());
		assertEquals(5, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //With a count query, the total elements should equal the number returned by the count query
		assertFalse(page.hasNext()); //with a count query, the next page calculation is correct
	}

	@Test
	public void shouldFindSlicedCinemas() {
		Pageable pageable = new PageRequest(0, 3);
		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
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
	public void shouldCorrectlyCalculateWhetherNextSliceExists() {
		Pageable pageable = new PageRequest(0, 5);
		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertEquals(5, slice.getNumberOfElements());
		assertTrue(slice.hasNext());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(5, slice.getNumberOfElements());
		assertFalse(slice.hasNext());
	}


	/**
	 * @see DATAGRAPH-887
	 */
	@Test
	public void shouldFindPagedAndSortedCinemas() {
		Pageable pageable = new PageRequest(0, 4, Sort.Direction.ASC, "name");

		Page<Cinema> page = cinemaRepository.findByLocation("London", pageable);
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(8, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertEquals("Cineplex", page.getContent().get(0).getName());
		assertEquals("Inox", page.getContent().get(1).getName());
		assertEquals("Landmark", page.getContent().get(2).getName());
		assertEquals("Metro", page.getContent().get(3).getName());

		page = cinemaRepository.findByLocation("London", page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(12, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertEquals("Movietime", page.getContent().get(0).getName());
		assertEquals("PVR", page.getContent().get(1).getName());
		assertEquals("Picturehouse", page.getContent().get(2).getName());
		assertEquals("Rainbow", page.getContent().get(3).getName());

		page = cinemaRepository.findByLocation("London", page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertFalse(page.hasNext());
		assertEquals(10, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertEquals("Regal", page.getContent().get(0).getName());
		assertEquals("Ritzy", page.getContent().get(1).getName());
	}

	/**
	 * @see DATAGRAPH-887
	 */
	@Test
	public void shouldSortPageWhenNestedPropertyIsInvolved() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) " +
				"CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " +
				"CREATE (m:Theatre {name:'Picturehouse', city:'London', capacity: 5000}) " +
				"CREATE (u:User {name:'Michal'}) " +
				"CREATE (u)-[:VISITED]->(r)  " +
				"CREATE (u)-[:VISITED]->(m)");

		Page<Cinema> page = cinemaRepository.findByLocationAndVisitedName("London", "Michal", new PageRequest(0, 1, Sort.Direction.DESC, "name"));
		assertEquals(1, page.getNumberOfElements());
		assertEquals("Ritzy", page.getContent().get(0).getName());

		page = cinemaRepository.findByLocationAndVisitedName("London", "Michal", new PageRequest(1, 1, Sort.Direction.DESC, "name"));
		assertEquals(1, page.getNumberOfElements());
		assertEquals("Picturehouse", page.getContent().get(0).getName());
	}

	/**
	 * @see DATAGRAPH-887
	 */
	@Test
	public void shouldSortPageByNestedPropertyIsInvolved() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) " +
				"CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " +
				"CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 5000}) " +
				"CREATE (u:User {name:'Michal'}) " +
				"CREATE (u)-[:VISITED]->(r)  " +
				"CREATE (u)-[:VISITED]->(m)");

		Page<Cinema> page = cinemaRepository.findByVisitedName("Michal", new PageRequest(0, 1, Sort.Direction.ASC, "location"));
		assertEquals(1, page.getNumberOfElements());
		assertEquals("Regal", page.getContent().get(0).getName());

		page = cinemaRepository.findByVisitedName("Michal", new PageRequest(1, 1, Sort.Direction.DESC, "location"));
		assertEquals(1, page.getNumberOfElements());
		assertEquals("Regal", page.getContent().get(0).getName());
	}

	/**
	 * @see DATAGRAPH-887
	 */
	@Test
	public void shouldFindSortedCinemas() {
		Sort sort = new Sort(Sort.Direction.ASC, "name");
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

	/**
	 * @see DATAGRAPH-887
	 */
	@Test
	public void shouldFindPagedAndSortedCinemasByCapacity() {
		Pageable pageable = new PageRequest(0, 4, Sort.Direction.ASC, "name");

		List<Cinema> page = cinemaRepository.findByCapacity(500, pageable);
		assertEquals(4, page.size());
		assertEquals("Cineplex", page.get(0).getName());
		assertEquals("Inox", page.get(1).getName());
		assertEquals("Landmark", page.get(2).getName());
		assertEquals("Metro", page.get(3).getName());

		pageable = new PageRequest(1, 4, Sort.Direction.ASC, "name");
		page = cinemaRepository.findByCapacity(500, pageable);
		assertEquals(4, page.size());
		assertEquals("Movietime", page.get(0).getName());
		assertEquals("PVR", page.get(1).getName());
		assertEquals("Picturehouse", page.get(2).getName());
		assertEquals("Rainbow", page.get(3).getName());

		pageable = new PageRequest(2, 4, Sort.Direction.ASC, "name");
		page = cinemaRepository.findByCapacity(500, pageable);
		assertEquals(2, page.size());
		assertEquals("Regal", page.get(0).getName());
		assertEquals("Ritzy", page.get(1).getName());
	}

	/**
	 * @see DATAGRAPH-653
	 */
	@Test
	public void shouldFindPagedCinemasSortedWithCustomQuery() {
		Pageable pageable = new PageRequest(0, 4, Sort.Direction.ASC, "n.name");

		Page<Cinema> page = cinemaRepository.getPagedCinemas(pageable);
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(8, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertEquals("Cineplex", page.getContent().get(0).getName());
		assertEquals("Inox", page.getContent().get(1).getName());
		assertEquals("Landmark", page.getContent().get(2).getName());
		assertEquals("Metro", page.getContent().get(3).getName());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(12, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertEquals("Movietime", page.getContent().get(0).getName());
		assertEquals("PVR", page.getContent().get(1).getName());
		assertEquals("Picturehouse", page.getContent().get(2).getName());
		assertEquals("Rainbow", page.getContent().get(3).getName());

		page = cinemaRepository.getPagedCinemas(page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertFalse(page.hasNext());
		assertEquals(10, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertEquals("Regal", page.getContent().get(0).getName());
		assertEquals("Ritzy", page.getContent().get(1).getName());
	}

	/**
	 * @see DATAGRAPH-653
	 */
	@Test
	public void shouldFindSlicedCinemasSortedWithCustomQuery() {
		Pageable pageable = new PageRequest(0, 4, Sort.Direction.ASC, "n.name");

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

	/**
	 * @see DATAGRAPH-653
	 */
	@Test
	public void shouldFindCinemasSortedByNameWithCustomQuery() {
		Sort sort = new Sort(Sort.Direction.ASC, "n.name");
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
}
