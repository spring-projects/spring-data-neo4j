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

package org.springframework.data.neo4j.repositories.support;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.neo4j.ogm.session.Session;
import org.springframework.aop.framework.Advised;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactory;
import org.springframework.data.neo4j.repository.support.SimpleNeo4jRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit tests for {@code GraphRepositoryFactory}.
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.class)
public class GraphRepositoryFactoryTests {

	Neo4jRepositoryFactory factory;

	@Mock org.neo4j.ogm.session.Session session;

	@Before
	public void setUp() {

		factory = new Neo4jRepositoryFactory(session) {

		};
	}

	/**
	 * Assert that the instance created for the standard configuration is a valid {@code UserRepository}.
	 *
	 * @throws Exception
	 */
	@Test
	public void setsUpBasicInstanceCorrectly() throws Exception {
		assertNotNull(factory.getRepository(ObjectRepository.class));
	}

	@Test
	public void allowsCallingOfObjectMethods() {

		ObjectRepository repository = factory.getRepository(ObjectRepository.class);

		repository.hashCode();
		repository.toString();
		repository.equals(repository);
	}

	@Test
	public void usesConfiguredRepositoryBaseClass() {
		factory.setRepositoryBaseClass(CustomNeo4jRepository.class);
		ObjectRepository repository = factory.getRepository(ObjectRepository.class);
		assertEquals(CustomNeo4jRepository.class, ((Advised) repository).getTargetClass());
	}

	private interface ObjectRepository extends Neo4jRepository<Object, Long> {

		@Override
		@Transactional
		Optional<Object> findById(Long id);
	}

	static class CustomNeo4jRepository<T, ID extends Serializable> extends SimpleNeo4jRepository<T, ID> {

		public CustomNeo4jRepository(Class<T> clazz, Session session) {
			super(clazz, session);
		}
	}
}
