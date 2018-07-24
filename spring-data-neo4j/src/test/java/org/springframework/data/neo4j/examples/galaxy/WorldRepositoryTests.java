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

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.repo.WorldRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@ContextConfiguration(classes = { WorldRepositoryTests.GalaxyContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class WorldRepositoryTests extends MultiDriverTestClass {

	@Autowired WorldRepository worldRepository;

	@Autowired TransactionTemplate transactionTemplate;

	boolean failed = false;

	/**
	 * see https://jira.spring.io/browse/DATAGRAPH-951
	 *
	 * @throws Exception
	 */
	@Test
	public void multipleThreadsResultsGetMixedUp() throws Exception {

		World world1 = new World("world 1", 1);
		worldRepository.save(world1, 0);

		World world2 = new World("world 2", 2);
		worldRepository.save(world2, 0);

		int iterations = 10;

		ExecutorService service = Executors.newFixedThreadPool(2);
		final CountDownLatch countDownLatch = new CountDownLatch(iterations * 2);

		for (int i = 0; i < iterations; i++) {

			service.execute(() -> {
				World world = worldRepository.findByName("world 1");

				if (!"world 1".equals(world.getName())) {
					failed = true;
				}
				countDownLatch.countDown();
			});

			service.execute(() -> {

				World world = worldRepository.findByName("world 2");

				if (!"world 2".equals(world.getName())) {
					failed = true;
				}
				countDownLatch.countDown();
			});
		}
		countDownLatch.await();
		assertFalse(failed);
	}

	/**
	 * see https://jira.spring.io/browse/DATAGRAPH-948
	 *
	 * @throws Exception
	 */
	@Test(expected = IncorrectResultSizeDataAccessException.class)
	public void findByNameSingleResult() throws Exception {
		World world1 = new World("world 1", 1);
		worldRepository.save(world1, 0);

		World world2 = new World("world 1", 2);
		worldRepository.save(world2, 0);

		// there are 2 results, 1 is returned instead of IncorrectResultSizeDataAccessException thrown
		worldRepository.findByName("world 1");
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
