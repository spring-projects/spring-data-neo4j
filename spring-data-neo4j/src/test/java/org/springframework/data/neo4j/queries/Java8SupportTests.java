/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.harness.ServerControls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.repo.CinemaStreamingRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nicolas Mervaillie
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class Java8SupportTests {

	@Autowired private ServerControls neo4jTestServer;

	@Autowired private CinemaStreamingRepository cinemaRepository;

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

	@Test
	@Transactional
	public void shouldFindOptionalCinema() {
		setup();

		Optional<Cinema> cinema = cinemaRepository.findByName("Picturehouse", 1);
		assertTrue(cinema.isPresent());
		assertEquals("Picturehouse", cinema.get().getName());

		cinema = cinemaRepository.findByName("ZZZ", 1);
		assertFalse(cinema.isPresent());
	}

	@Test
	@Transactional
	public void shouldStreamCinemas() {
		setup();

		Stream<Cinema> allCinemas = cinemaRepository.getAllCinemas();
		assertEquals(10, allCinemas.count());
	}

	@Test
	@Transactional
	public void shouldStreamCinemasWithSort() {
		setup();

		Collection<Cinema> allCinemas = cinemaRepository.getCinemasSortedByName(new Sort("n.name"))
				.collect(Collectors.toList());

		assertEquals(10, allCinemas.size());
		assertEquals("Cineplex", allCinemas.iterator().next().getName());
	}

	@Test
	@Transactional
	public void shouldGetCinemasAsync() {
		setup();

		cinemaRepository.getAllCinemasAsync().thenAccept(cinemas -> {
			assertEquals(10, cinemas.size());
			cinemas.forEach(cinema -> assertNotNull(cinema.getName()));
		});
	}
}
