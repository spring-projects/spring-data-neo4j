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
package org.springframework.data.neo4j.transaction;

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
import org.springframework.data.neo4j.transaction.service.ServiceA;
import org.springframework.data.neo4j.transaction.service.ServiceB;
import org.springframework.data.neo4j.transaction.service.WrapperService;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * See <a href=
 * "https://stackoverflow.com/questions/17224887/java-spring-transactional-method-not-rolling-back-as-expected">StackOverFlow</a>
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
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.transaction.domain",
			repositoryPackages = "org.springframework.data.neo4j.transaction.repo")
	@ComponentScan("org.springframework.data.neo4j.transaction.service")
	static class ApplicationConfig {}
}
