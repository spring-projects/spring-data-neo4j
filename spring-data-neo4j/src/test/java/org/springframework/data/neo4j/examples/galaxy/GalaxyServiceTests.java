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

import static org.assertj.core.api.Assertions.*;

/**
 * @author Vince Bickers
 * @author Mark Paluch
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = GalaxyContextConfiguration.class)
@RunWith(SpringRunner.class)
@Transactional
public class GalaxyServiceTests {

	@Autowired
	private GalaxyService galaxyService;

	@Before
	public void setUp() {
		galaxyService.deleteAll();
		assertThat(galaxyService.getNumberOfWorlds()).isEqualTo(0);
	}

	@Test
	public void shouldAllowDirectWorldCreation() {

		World myWorld = galaxyService.createWorld("mine", 0);
		Collection<World> foundWorlds = (Collection<World>) galaxyService.getAllWorlds();

		assertThat(foundWorlds.size()).isEqualTo(1);
		World mine = foundWorlds.iterator().next();

		assertThat(mine.getName()).isEqualTo(myWorld.getName());
	}

	@Test
	public void shouldHaveCorrectNumberOfWorlds() {
		galaxyService.makeSomeWorlds();
		assertThat(galaxyService.getNumberOfWorlds()).isEqualTo(13);
	}

	@Test
	public void createAllWorldsAtOnce() {
		galaxyService.makeAllWorldsAtOnce();
		assertThat(galaxyService.getNumberOfWorlds()).isEqualTo(13);

		World earth = galaxyService.findWorldByName("Earth");
		World mars = galaxyService.findWorldByName("Mars");

		assertThat(mars.canBeReachedFrom(earth)).isTrue();
		assertThat(earth.canBeReachedFrom(mars)).isTrue();
	}

	@Test
	public void shouldFindWorldsById() {
		galaxyService.makeSomeWorlds();

		for (World world : galaxyService.getAllWorlds()) {
			Optional<World> foundWorld = galaxyService.findWorldById(world.getId());
			assertThat(foundWorld.isPresent()).isTrue();
		}
	}

	@Test
	public void shouldFindWorldsByName() {
		galaxyService.makeSomeWorlds();
		for (World world : galaxyService.getAllWorlds()) {
			World foundWorld = galaxyService.findWorldByName(world.getName());
			assertThat(foundWorld).isNotNull();
		}
	}

	@Test
	public void shouldReachMarsFromEarth() {
		galaxyService.makeSomeWorlds();

		World earth = galaxyService.findWorldByName("Earth");
		World mars = galaxyService.findWorldByName("Mars");

		assertThat(mars.canBeReachedFrom(earth)).isTrue();
		assertThat(earth.canBeReachedFrom(mars)).isTrue();
	}

	@Test
	public void shouldFindAllWorlds() {

		Collection<World> madeWorlds = galaxyService.makeSomeWorlds();
		Iterable<World> foundWorlds = galaxyService.getAllWorlds();

		int countOfFoundWorlds = 0;
		for (World foundWorld : foundWorlds) {
			assertThat(madeWorlds.contains(foundWorld)).isTrue();
			countOfFoundWorlds++;
		}

		assertThat(countOfFoundWorlds).isEqualTo(madeWorlds.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldFindWorldsWith1Moon() {
		galaxyService.makeSomeWorlds();

		for (World worldWithOneMoon : galaxyService.findAllByNumberOfMoons(1)) {
			assertThat(worldWithOneMoon.getName()).satisfiesAnyOf(s -> {
				assertThat(s).contains("Earth");
			}, s -> {
				assertThat(s).contains("Midgard");
			});
		}
	}

	@Test
	public void shouldNotFindKrypton() {
		galaxyService.makeSomeWorlds();
		World krypton = galaxyService.findWorldByName("Krypton");
		assertThat(krypton).isNull();
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
			Iterable<World> paged = galaxyService
					.findAllWorlds(new Pagination(page, PAGE_SIZE));
			for (World world : paged) {
				n -= world.getId();
			}
		}

		assertThat(n).isEqualTo(0L);
	}

	@Test
	public void shouldDetectNotOnLastPage() {

		int count = galaxyService.makeAllWorldsAtOnce().size();

		assertThat(13).isEqualTo(count);

		Pageable pageable = PageRequest.of(2, 3);
		Page<World> worlds = galaxyService.findAllWorlds(pageable);

		assertThat(worlds.hasNext()).isTrue();
	}

	@Test
	public void shouldDetectLastPage() {

		int count = galaxyService.makeAllWorldsAtOnce().size();

		assertThat(13).isEqualTo(count);

		Pageable pageable = PageRequest.of(4, 3);
		Page<World> worlds = galaxyService.findAllWorlds(pageable);

		assertThat(worlds.hasNext()).isFalse();
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

		for (; ; ) {
			Page<World> page = galaxyService.findAllWorlds(pageable);
			for (World world : page) {
				sum -= world.getId();
			}
			if (!page.hasNext()) {
				break;
			}
			pageable = pageable.next();
		}

		assertThat(sum).isEqualTo(0);
	}

	@Test
	public void shouldPageAllWorldsSorted() {

		List<World> worlds = (List<World>) galaxyService.makeAllWorldsAtOnce();
		int count = worlds.size();
		assertThat(13).isEqualTo(count);

		String[] sortedNames = getNamesSorted(worlds);

		Pageable pageable = PageRequest.of(0, 3, Sort.Direction.ASC, "name");

		int i = 0;
		for (; ; ) {
			Page<World> page = galaxyService.findAllWorlds(pageable);
			for (World world : page) {
				assertThat(world.getName()).isEqualTo(sortedNames[i]);
				count--;
				i++;
			}
			if (!page.hasNext()) {
				break;
			}
			pageable = pageable.next();
		}

		assertThat(count).isEqualTo(0);
	}

	@Test
	public void shouldIterateAllWorldsSorted() {

		List<World> worlds = (List<World>) galaxyService.makeAllWorldsAtOnce();
		int count = worlds.size();
		assertThat(13).isEqualTo(count);

		String[] sortedNames = getNamesSorted(worlds);

		Sort sort = Sort.by(Sort.Direction.ASC, "name");
		int i = 0;
		for (World world : galaxyService.findAllWorlds(sort)) {
			assertThat(world.getName()).isEqualTo(sortedNames[i]);
			count--;
			i++;
		}

		assertThat(count).isEqualTo(0);
	}

	@Test // DATAGRAPH-783
	public void shouldFindWorldWithRadius() {
		galaxyService.deleteAll();
		World earth = new World("Earth", 1);
		earth.setRadius(6371.0f);
		galaxyService.create(earth);

		earth = galaxyService.findByName("Earth");
		assertThat(earth.getRadius()).isEqualTo(Float.valueOf(6371.0f));
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
