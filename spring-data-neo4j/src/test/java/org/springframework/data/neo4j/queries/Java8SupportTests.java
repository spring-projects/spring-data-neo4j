/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringRunner.class)
@Transactional
public class Java8SupportTests {

	@Autowired private ServerControls neo4jTestServer;

	@Autowired private CinemaStreamingRepository cinemaRepository;

	@Before
	public void setup() {
		neo4jTestServer.graph().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");

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
	public void shouldFindOptionalCinema() {
		Optional<Cinema> cinema = cinemaRepository.findByName("Picturehouse", 1);
		assertTrue(cinema.isPresent());
		assertEquals("Picturehouse", cinema.get().getName());

		cinema = cinemaRepository.findByName("ZZZ", 1);
		assertFalse(cinema.isPresent());
	}

	@Test
	public void shouldStreamCinemas() {
		Stream<Cinema> allCinemas = cinemaRepository.getAllCinemas();
		assertEquals(10, allCinemas.count());
	}

	@Test
	public void shouldStreamCinemasWithSort() {
		Collection<Cinema> allCinemas = cinemaRepository.getCinemasSortedByName(new Sort("n.name"))
				.collect(Collectors.toList());

		assertEquals(10, allCinemas.size());
		assertEquals("Cineplex", allCinemas.iterator().next().getName());
	}

	@Test
	public void shouldGetCinemasAsync() {
		cinemaRepository.getAllCinemasAsync().thenAccept(cinemas -> {
			assertEquals(10, cinemas.size());
			cinemas.forEach(cinema -> assertNotNull(cinema.getName()));
		});
	}
}
