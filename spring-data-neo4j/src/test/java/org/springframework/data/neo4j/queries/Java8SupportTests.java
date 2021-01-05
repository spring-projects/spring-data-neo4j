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
		Collection<Cinema> allCinemas = cinemaRepository.getCinemasSortedByName(Sort.by("n.name"))
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
