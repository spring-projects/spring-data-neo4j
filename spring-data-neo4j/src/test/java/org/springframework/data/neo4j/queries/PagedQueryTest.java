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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @see DATAGRAPH-680
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class PagedQueryTest extends MultiDriverTestClass {

	private static GraphDatabaseService graphDatabaseService = getGraphDatabaseService();

	@Autowired
	private CinemaRepository cinemaRepository;

	@Before
	public void init() {
		clearDatabase();
		String[] names = new String[] {"Picturehouse", "Regal", "Ritzy", "Metro", "Inox", "PVR", "Cineplex", "Landmark", "Rainbow", "Movietime"};
		for (String name : names) {
			Cinema cinema = new Cinema(name);
			cinema.setLocation("London");
			cinemaRepository.save(cinema);
		}
	}

	@After
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldFindPagedCinemas() {
		Pageable pageable = new PageRequest(0,3);
		Page<Cinema> page = cinemaRepository.getPagedCinemasByName(pageable);
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(6, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemasByName(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(9, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemasByName(page.nextPageable());
		assertEquals(3, page.getNumberOfElements());
		assertTrue(page.hasNext());
		assertEquals(12, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate

		page = cinemaRepository.getPagedCinemasByName(page.nextPageable());
		assertEquals(1, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	@Test
	public void shouldNotRelyOnTotalElementsToFindPagedCinemas() {
		Pageable pageable = new PageRequest(0,5);
		Page<Cinema> page = cinemaRepository.getPagedCinemasByName(pageable);
		assertEquals(5, page.getNumberOfElements());
		assertTrue(page.hasNext());

		page = cinemaRepository.getPagedCinemasByName(page.nextPageable());
		assertEquals(5, page.getNumberOfElements());
		assertTrue(page.hasNext()); //this is an estimate and so for the last page, page.hasNext() is true when the total number of elements / total number of pages=0

		page = cinemaRepository.getPagedCinemasByName(page.nextPageable());
		assertEquals(0, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	@Test
	public void shouldFindPagedCinemasWithAccurateTotalCount() {
		Pageable pageable = new PageRequest(0,3);
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
		Pageable pageable = new PageRequest(0,5);
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
		Pageable pageable = new PageRequest(0,5);
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
		Pageable pageable = new PageRequest(0,3);
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
		Pageable pageable = new PageRequest(0,5);
		Slice<Cinema> slice = cinemaRepository.getSlicedCinemasByName(pageable);
		assertEquals(5, slice.getNumberOfElements());
		assertTrue(slice.hasNext());

		slice = cinemaRepository.getSlicedCinemasByName(slice.nextPageable());
		assertEquals(5, slice.getNumberOfElements());
		assertFalse(slice.hasNext());
	}

}
