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
package org.springframework.data.neo4j.repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.NodeWithUUIDAsId;
import org.springframework.data.neo4j.domain.sample.SampleEntity;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactory;
import org.springframework.data.neo4j.repository.support.TransactionalRepositoryTests;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Michael J. Simons
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Neo4jRepositoryTests.Config.class)
@Transactional
public class Neo4jRepositoryTests extends MultiDriverTestClass {

	@Autowired
	SampleEntityRepository repository;

	@Autowired
	NodeWithUUIDAsIdRepository nodeWithUUIDAsIdRepository;


	@Test
	public void testCrudOperationsForCompoundKeyEntity() throws Exception {

		SampleEntity entity = new SampleEntity("foo", "bar");
		repository.save(entity);
		assertThat(repository.existsById(entity.getId()), is(true));
		assertThat(repository.count(), is(1L));

		Optional<SampleEntity> optional = repository.findById(entity.getId());
		assertTrue(optional.isPresent());
		optional.ifPresent(actual -> assertThat(actual, is(entity)));

		repository.deleteAll(Arrays.asList(entity));
		assertThat(repository.count(), is(0L));
	}

	@Test // DATAGRAPH-1144
	public void explicitIdsWithCustomTypesShouldWork() throws Exception {

		NodeWithUUIDAsId entity = new NodeWithUUIDAsId("someProperty");
		nodeWithUUIDAsIdRepository.save(entity);

		assertThat(nodeWithUUIDAsIdRepository.existsById(entity.getMyNiceId()), is(true));
		assertThat(nodeWithUUIDAsIdRepository.count(), is(1L));

		Optional<NodeWithUUIDAsId> retrievedEntity = nodeWithUUIDAsIdRepository.findById(entity.getMyNiceId());
		assertTrue(retrievedEntity.isPresent());
		assertThat(retrievedEntity.get(), is(entity));

		nodeWithUUIDAsIdRepository.deleteAll(Arrays.asList(entity));
		assertThat(nodeWithUUIDAsIdRepository.count(), is(0L));
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	public static class Config {

		@Bean
		public TransactionalRepositoryTests.DelegatingTransactionManager transactionManager() throws Exception {
			return new TransactionalRepositoryTests.DelegatingTransactionManager(
					new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.domain.sample");
		}
	}
}

interface SampleEntityRepository extends Neo4jRepository<SampleEntity, Long> {}

interface NodeWithUUIDAsIdRepository extends Neo4jRepository<NodeWithUUIDAsId, UUID> {}
