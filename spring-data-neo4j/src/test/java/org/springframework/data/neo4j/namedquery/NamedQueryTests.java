/*
 * Copyright 2011-2020 the original author or authors.
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
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { NamedQueryTests.NamedQueryContext.class })
@RunWith(SpringRunner.class)
@Transactional
public class NamedQueryTests {

	private static final String SAMPLE_ENTITY_NAME = "test";

	@Autowired private SampleEntityForNamedQueryRepository repository;

	@Test
	public void findElementByQueryAnnotation() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.getTitleEntity();
		assertThat(titleEntity).isNotNull();
	}

	@Test
	public void findElementByDerivedFunction() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByName(SAMPLE_ENTITY_NAME);
		assertThat(titleEntity).isNotNull();
	}

	@Test
	public void findElementByNamedQuery() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByQueryWithoutParameter();
		assertThat(titleEntity).isNotNull();
	}

	@Test
	public void findElementByNamedQueryWithParameter() {
		createAndSaveSampleEntity();

		SampleEntityForNamedQuery titleEntity = repository.findByQueryWithParameter(SAMPLE_ENTITY_NAME);
		assertThat(titleEntity).isNotNull();
	}

	private void createAndSaveSampleEntity() {
		SampleEntityForNamedQuery entity = new SampleEntityForNamedQuery();
		entity.setName(SAMPLE_ENTITY_NAME);
		repository.save(entity);
	}

	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.namedquery.domain",
			repositoryPackages = "org.springframework.data.neo4j.namedquery.repo")
	@ComponentScan({ "org.springframework.data.neo4j.namedquery" })
	static class NamedQueryContext {
	}
}
