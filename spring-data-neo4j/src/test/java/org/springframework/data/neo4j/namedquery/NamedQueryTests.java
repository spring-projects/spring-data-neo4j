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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.namedquery.domain.SampleEntityForNamedQuery;
import org.springframework.data.neo4j.namedquery.repo.SampleEntityForNamedQueryRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Ihor Dziuba
 */
@ContextConfiguration(classes = { NamedQueryTests.NamedQueryContext.class })
@RunWith(SpringRunner.class)
@Transactional
public class NamedQueryTests {

	private static final String SAMPLE_ENTITY_NAME = "test";

	@Autowired private SampleEntityForNamedQueryRepository repository;

	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();

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

	@Test  //DATAGRAPH-1241
	public void findElementsByNamedPagedQueryWithParameter() {
		createAndSaveSampleEntity();
		createAndSaveSampleEntity();

		Page<SampleEntityForNamedQuery> page = repository.findByPagedQueryWithParameter(SAMPLE_ENTITY_NAME, PageRequest.of(0,1));
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.get().count()).isEqualTo(1);
	}

	@Test  //DATAGRAPH-1241
	public void findElementsByNamedPagedQueryFailedWithoutCountQuery() {
		createAndSaveSampleEntity();
		createAndSaveSampleEntity();

		exceptionRule.expect(IllegalArgumentException.class);
		exceptionRule.expectMessage("Must specify a count query to get pagination info.");
		Page<SampleEntityForNamedQuery> page = repository.findByPagedQueryWithoutCountQuery(SAMPLE_ENTITY_NAME, PageRequest.of(0,1));
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
