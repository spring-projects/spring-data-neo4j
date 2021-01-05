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
package org.springframework.data.neo4j.namedquery;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.namedquery.domain.SampleEntityForNamedQuery;
import org.springframework.data.neo4j.namedquery.repo.SampleEntityForNamedQueryRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { NamedQueryTests.NamedQueryContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class NamedQueryTests {

	private static final String SAMPLE_ENTITY_NAME = "test";

	@Autowired private SampleEntityForNamedQueryRepository repository;

	@Test
	public void findElementByQueryAnnotation() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.getTitleEntity();
		assertNotNull(titleEntity);
	}

	@Test
	public void findElementByDerivedFunction() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByName(SAMPLE_ENTITY_NAME);
		assertNotNull(titleEntity);
	}

	@Test
	public void findElementByNamedQuery() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByQueryWithoutParameter();
		assertNotNull(titleEntity);
	}

	@Test
	public void findElementByNamedQueryWithParameter() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByQueryWithParameter(SAMPLE_ENTITY_NAME);
		assertNotNull(titleEntity);
	}

	private void createAndSaveSampleEntity() {
		SampleEntityForNamedQuery entity = new SampleEntityForNamedQuery();
		entity.setName(SAMPLE_ENTITY_NAME);
		repository.save(entity);
	}

	@Configuration
	@ComponentScan({ "org.springframework.data.neo4j.namedquery" })
	@EnableNeo4jRepositories(value = "org.springframework.data.neo4j.namedquery.repo")
	@EnableTransactionManagement
	static class NamedQueryContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory("org.springframework.data.neo4j.namedquery.domain");
		}
	}
}
