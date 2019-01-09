/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.SampleEntity;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactory;
import org.springframework.data.neo4j.repository.support.TransactionalRepositoryIT;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mark Angrish
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Neo4jRepositoryIT.Config.class)
@Transactional
public class Neo4jRepositoryIT extends MultiDriverTestClass {

	@Autowired Session session;

	Neo4jRepository<SampleEntity, Long> repository;

	@Before
	public void setUp() {

		repository = new Neo4jRepositoryFactory(session).getRepository(SampleEntityRepository.class);
	}

	@Test
	public void testCrudOperationsForCompoundKeyEntity() throws Exception {

		SampleEntity entity = new SampleEntity("foo", "bar");
		repository.save(entity);
		assertThat(repository.exists(entity.getId()), is(true));
		assertThat(repository.count(), is(1L));
		assertThat(repository.findOne(entity.getId()), is(entity));

		repository.delete(Arrays.asList(entity));
		assertThat(repository.count(), is(0L));
	}

	private interface SampleEntityRepository extends Neo4jRepository<SampleEntity, Long> {

	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	public static class Config {

		@Bean
		public TransactionalRepositoryIT.DelegatingTransactionManager transactionManager() throws Exception {
			return new TransactionalRepositoryIT.DelegatingTransactionManager(new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory("org.springframework.data.neo4j.domain.sample");
		}
	}
}
