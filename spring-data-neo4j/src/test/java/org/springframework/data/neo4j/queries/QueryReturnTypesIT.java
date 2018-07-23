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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.galaxy.context.GalaxyContext;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.repo.WorldRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { GalaxyContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class QueryReturnTypesIT extends MultiDriverTestClass {

	@Autowired WorldRepository worldRepository;

	@Autowired Session session;

	@Test
	public void shouldCallExecuteWhenPrimitiveVoidReturnTypeOnQuery() {

		World world = new World("Tatooine", 0);

		worldRepository.save(world);
		worldRepository.touchAllWorlds();

		session.clear();
		world = worldRepository.findOne(world.getId());
		assertNotNull(world.getUpdated());
	}

	/**
	 * @see DATAGRAPH-704
	 */
	@Test
	public void shouldCallExecuteWhenVoidReturnTypeOnQuery() {

		World tatooine = new World("Tatooine", 0);
		World dagobah = new World("Dagobah", 0);

		tatooine.addRocketRouteTo(dagobah);

		worldRepository.save(tatooine);
		worldRepository.touchAllWorlds();

		session.clear();
		tatooine = worldRepository.findOne(tatooine.getId());

		assertNotNull(tatooine.getUpdated());
		assertEquals(1, tatooine.getReachableByRocket().size());

		for (World world : tatooine.getReachableByRocket()) {
			assertNotNull(world.getUpdated());
		}
	}

	@Test
	public void shouldReturnStatisticsIfRequested() {

		World tatooine = new World("Tatooine", 0);

		worldRepository.save(tatooine);

		QueryStatistics stats = worldRepository.touchAllWorldsWithStatistics().queryStatistics();

		assertEquals(1, stats.getPropertiesSet());
	}
}
