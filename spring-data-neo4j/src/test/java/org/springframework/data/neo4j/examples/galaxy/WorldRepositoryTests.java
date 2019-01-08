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

package org.springframework.data.neo4j.examples.galaxy;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.repo.WorldRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Nicolas Mervaillie
 * @author Mark Angrish
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = GalaxyContextConfiguration.class)
@RunWith(SpringRunner.class)
public class WorldRepositoryTests {

	@Autowired WorldRepository worldRepository;

	@Autowired TransactionTemplate transactionTemplate;

	boolean failed = false;

	@Test // DATAGRAPH-951
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

	@Test(expected = IncorrectResultSizeDataAccessException.class) // DATAGRAPH-948
	public void findByNameSingleResult() {
		World world1 = new World("world 1", 1);
		worldRepository.save(world1, 0);

		World world2 = new World("world 1", 2);
		worldRepository.save(world2, 0);

		// there are 2 results, 1 is returned instead of IncorrectResultSizeDataAccessException thrown
		worldRepository.findByName("world 1");
	}
}
