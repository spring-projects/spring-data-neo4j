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
package org.springframework.data.neo4j.examples.galaxy;

import static org.hamcrest.core.AnyOf.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.StringContains.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.cypher.query.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.service.GalaxyService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vince Bickers
 * @author Mark Paluch
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = GalaxyContextConfiguration.class)
@RunWith(SpringRunner.class)
@Transactional
public class GalaxyServiceTests {

	@Autowired private GalaxyService galaxyService;

	@Before
	public void setUp() {
		galaxyService.deleteAll();
		assertEquals(0, galaxyService.getNumberOfWorlds());
	}

	@Test
	public void shouldAllowDirectWorldCreation() {

		World myWorld = galaxyService.createWorld("mine", 0);
		Collection<World> foundWorlds = (Collection<World>) galaxyService.getAllWorlds();

		assertEquals(1, foundWorlds.size());
		World mine = foundWorlds.iterator().next();

		assertEquals(myWorld.getName(), mine.getName());
	}

	@Test
	public void shouldHaveCorrectNumberOfWorlds() {
		galaxyService.makeSomeWorlds();
		assertEquals(13, galaxyService.getNumberOfWorlds());
	}

	@Test
	public void createAllWorldsAtOnce() {
		galaxyService.makeAllWorldsAtOnce();
		assertEquals(13, galaxyService.getNumberOfWorlds());

		World earth = galaxyService.findWorldByName("Earth");
		World mars = galaxyService.findWorldByName("Mars");

		assertTrue(mars.canBeReachedFrom(earth));
		assertTrue(earth.canBeReachedFrom(mars));
	}

	@Test
	public void shouldFindWorldsById() {
		galaxyService.makeSomeWorlds();

		for (World world : galaxyService.getAllWorlds()) {
			Optional<World> foundWorld = galaxyService.findWorldById(world.getId());
			assertTrue(foundWorld.isPresent());
		}
	}

	@Test
	public void shouldFindWorldsByName() {
		galaxyService.makeSomeWorlds();
		for (World world : galaxyService.getAllWorlds()) {
			World foundWorld = galaxyService.findWorldByName(world.getName());
			assertNotNull(foundWorld);
		}
	}

	@Test
	public void shouldReachMarsFromEarth() {
		galaxyService.makeSomeWorlds();

		World earth = galaxyService.findWorldByName("Earth");
		World mars = galaxyService.findWorldByName("Mars");

		assertTrue(mars.canBeReachedFrom(earth));
		assertTrue(earth.canBeReachedFrom(mars));
	}

	@Test
	public void shouldFindAllWorlds() {

		Collection<World> madeWorlds = galaxyService.makeSomeWorlds();
		Iterable<World> foundWorlds = galaxyService.getAllWorlds();

		int countOfFoundWorlds = 0;
		for (World foundWorld : foundWorlds) {
			assertTrue(madeWorlds.contains(foundWorld));
			countOfFoundWorlds++;
		}

		assertEquals(madeWorlds.size(), countOfFoundWorlds);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldFindWorldsWith1Moon() {
		galaxyService.makeSomeWorlds();

		for (World worldWithOneMoon : galaxyService.findAllByNumberOfMoons(1)) {
			assertThat(worldWithOneMoon.getName(), is(anyOf(containsString("Earth"), containsString("Midgard"))));
		}
	}

	@Test
	public void shouldNotFindKrypton() {
		galaxyService.makeSomeWorlds();
		World krypton = galaxyService.findWorldByName("Krypton");
		assertNull(krypton);
	}

	@Test
	public void shouldSupportPaging() {

		List<World> worlds = (List<World>) galaxyService.makeAllWorldsAtOnce();

		int count = worlds.size();
		int PAGE_SIZE = 3;
		int pages = count / PAGE_SIZE + 1;
		long n = 0;
		for (World world : worlds) {
			n += world.getId();
		}

		for (int page = 0; page < pages; page++) {
			Iterable<World> paged = galaxyService.findAllWorlds(new Pagination(page, PAGE_SIZE));
			for (World world : paged) {
				n -= world.getId();
			}
		}

		assertEquals(0L, n);
	}

	@Test
	public void shouldDetectNotOnLastPage() {

		int count = galaxyService.makeAllWorldsAtOnce().size();

		assertEquals(count, 13);

		Pageable pageable = PageRequest.of(2, 3);
		Page<World> worlds = galaxyService.findAllWorlds(pageable);

		assertTrue(worlds.hasNext());
	}

	@Test
	public void shouldDetectLastPage() {

		int count = galaxyService.makeAllWorldsAtOnce().size();

		assertEquals(count, 13);

		Pageable pageable = PageRequest.of(4, 3);
		Page<World> worlds = galaxyService.findAllWorlds(pageable);

		assertFalse(worlds.hasNext());
	}

	@Test
	public void shouldPageAllWorlds() {

		long sum = 0;
		List<World> worlds = (List<World>) galaxyService.makeAllWorldsAtOnce();
		for (World world : worlds) {
			sum += world.getId();
		}
		// note: this doesn't work, because deleted node ids are not reclaimed
		// long sum = (size * size - size) / 2; // 0-based node ids

		Pageable pageable = PageRequest.of(0, 3);

		for (;;) {
			Page<World> page = galaxyService.findAllWorlds(pageable);
			for (World world : page) {
				sum -= world.getId();
			}
			if (!page.hasNext()) {
				break;
			}
			pageable = pageable.next();
		}

		assertEquals(0, sum);
	}

	@Test
	public void shouldPageAllWorldsSorted() {

		List<World> worlds = (List<World>) galaxyService.makeAllWorldsAtOnce();
		int count = worlds.size();
		assertEquals(count, 13);

		String[] sortedNames = getNamesSorted(worlds);

		Pageable pageable = PageRequest.of(0, 3, Sort.Direction.ASC, "name");

		int i = 0;
		for (;;) {
			Page<World> page = galaxyService.findAllWorlds(pageable);
			for (World world : page) {
				assertEquals(sortedNames[i], world.getName());
				count--;
				i++;
			}
			if (!page.hasNext()) {
				break;
			}
			pageable = pageable.next();
		}

		assertEquals(0, count);
	}

	@Test
	public void shouldIterateAllWorldsSorted() {

		List<World> worlds = (List<World>) galaxyService.makeAllWorldsAtOnce();
		int count = worlds.size();
		assertEquals(count, 13);

		String[] sortedNames = getNamesSorted(worlds);

		Sort sort = Sort.by(Sort.Direction.ASC, "name");
		int i = 0;
		for (World world : galaxyService.findAllWorlds(sort)) {
			assertEquals(sortedNames[i], world.getName());
			count--;
			i++;
		}

		assertEquals(0, count);
	}

	@Test // DATAGRAPH-783
	public void shouldFindWorldWithRadius() {
		galaxyService.deleteAll();
		World earth = new World("Earth", 1);
		earth.setRadius(6371.0f);
		galaxyService.create(earth);

		earth = galaxyService.findByName("Earth");
		assertEquals(Float.valueOf(6371.0f), earth.getRadius());
	}

	private String[] getNamesSorted(List<World> worlds) {
		List<String> names = new ArrayList();

		for (World world : worlds) {
			names.add(world.getName());
		}

		String[] sortedNames = names.toArray(new String[] {});
		Arrays.sort(sortedNames);
		return sortedNames;
	}
}
