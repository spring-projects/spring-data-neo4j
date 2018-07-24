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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.model.QueryStatistics;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.repo.WorldRepository;
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
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 */
@ContextConfiguration(classes = { QueryReturnTypesTests.GalaxyContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class QueryReturnTypesTests extends MultiDriverTestClass {

	@Autowired PlatformTransactionManager transactionManager;

	@Autowired TransactionTemplate transactionTemplate;

	@Autowired WorldRepository worldRepository;

	@Autowired Session session;

	@Before
	public void clearDatabase() {
		transactionTemplate = new TransactionTemplate(transactionManager);
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldCallExecuteWhenPrimitiveVoidReturnTypeOnQuery() {

		World world = new World("Tatooine", 0);

		worldRepository.save(world);
		worldRepository.touchAllWorlds();

		session.clear();
		world = worldRepository.findById(world.getId()).get();
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
		tatooine = worldRepository.findById(tatooine.getId()).get();

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
