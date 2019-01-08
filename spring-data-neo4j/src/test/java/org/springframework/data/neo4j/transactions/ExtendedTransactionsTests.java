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

package org.springframework.data.neo4j.transactions;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.transactions.service.ServiceA;
import org.springframework.data.neo4j.transactions.service.ServiceB;
import org.springframework.data.neo4j.transactions.service.WrapperService;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * See <a href=
 * "http://stackoverflow.com/questions/17224887/java-spring-transactional-method-not-rolling-back-as-expected">StackOverFlow</a>
 * and DATAGRAPH-602
 *
 * @author: Vince Bickers
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = ExtendedTransactionsTests.ApplicationConfig.class)
@RunWith(SpringRunner.class)
public class ExtendedTransactionsTests {

	@Autowired GraphDatabaseService graphDatabaseService;

	@Autowired ServiceA serviceA;

	@Autowired ServiceB serviceB;

	@Autowired WrapperService wrapperService;

	@Before
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldRollbackSuccessThenFail() {

		try {
			wrapperService.composeSuccessThenFail();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	@Transactional
	@Rollback
	public void shouldCommitSuccessSuccess() {

		try {
			wrapperService.composeSuccessThenSuccess();
			assertEquals(2, countNodes());
		} catch (Exception e) {
			fail("should not have thrown exception");
		}
	}

	@Test
	public void shouldRollbackFailThenSuccess() {
		try {
			wrapperService.composeFailThenSuccess();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	public void shouldRollbackFailThenFail() {
		try {
			wrapperService.composeFailThenFail();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	public void shouldRollbackWithCheckedException() {
		try {
			wrapperService.rollbackWithCheckedException();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	public void shouldRollbackRepositoryMethodOnCheckedException() {
		try {
			serviceA.run();
		} catch (Exception e) {
			assertNull(serviceB.getBilbo());
		}
	}

	private int countNodes() {
		Iterator iterator = wrapperService.fetch().iterator();
		int i = 0;
		while (iterator.hasNext()) {
			iterator.next();
			i++;
		}
		return i;
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.transactions.domain",
			repositoryPackages = "org.springframework.data.neo4j.transactions.repo")
	@ComponentScan("org.springframework.data.neo4j.transactions.service")
	static class ApplicationConfig {}
}
