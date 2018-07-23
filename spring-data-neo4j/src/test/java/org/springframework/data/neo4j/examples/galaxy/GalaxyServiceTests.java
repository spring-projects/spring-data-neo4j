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
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.service.GalaxyService;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Vince Bickers
 * @author Mark Paluch
 */
@ContextConfiguration(classes = { GalaxyServiceTests.GalaxyContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class GalaxyServiceTests extends MultiDriverTestClass {

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

		Pageable pageable = new PageRequest(2, 3);
		Page<World> worlds = galaxyService.findAllWorlds(pageable);

		assertTrue(worlds.hasNext());
	}

	@Test
	public void shouldDetectLastPage() {

		int count = galaxyService.makeAllWorldsAtOnce().size();

		assertEquals(count, 13);

		Pageable pageable = new PageRequest(4, 3);
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

		Pageable pageable = new PageRequest(0, 3);

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

		Pageable pageable = new PageRequest(0, 3, Sort.Direction.ASC, "name");

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

		Sort sort = new Sort(Sort.Direction.ASC, "name");
		int i = 0;
		for (World world : galaxyService.findAllWorlds(sort)) {
			assertEquals(sortedNames[i], world.getName());
			count--;
			i++;
		}

		assertEquals(0, count);
	}

	/**
	 * @see DATAGRAPH-783
	 */
	@Test
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

	@Configuration
	@ComponentScan({ "org.springframework.data.neo4j.examples.galaxy.service" })
	@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.galaxy.repo")
	@EnableTransactionManagement
	static class GalaxyContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.galaxy.domain");
		}

		@Bean
		public TransactionTemplate transactionTemplate() {
			return new TransactionTemplate(transactionManager());
		}

	}
}
