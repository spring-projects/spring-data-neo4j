/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.neo4j.repository.cdi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.model.Person;

/**
 * Integration tests for {@link Neo4jCdiRepositoryExtension}.
 * 
 * @see DATAGRAPH-340
 * @author Nicki Watt
 * @author Oliver Gierke
 */
@Ignore
public class CdiExtensionIntegrationTests {

	static CdiTestContainer container;

	@BeforeClass
	public static void setUp() throws Exception {
		container = CdiTestContainerLoader.getCdiContainer();
		container.bootContainer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		container.shutdownContainer();
	}

	@Test
	public void testRepositoryStyle1IsCreatedCorrectly() {

		GraphDatabase database = container.getInstance(GraphDatabase.class);
		RepositoryClient client = container.getInstance(RepositoryClient.class);
		CdiPersonRepository repository = client.repository;

		assertThat(repository, is(notNullValue()));

		Person person = null;
		Person result = null;

		try (Transaction tx = database.beginTx()) {
			repository.deleteAll();

			person = new Person("Simon", 28);
			result = repository.save(person);
			tx.success();
		}

        try (Transaction tx = database.beginTx()) {
            assertThat(result, is(notNullValue()));
            Long resultId = result.getId();
            Person lookedUpPerson = repository.findOne(person.getId());
            assertThat(lookedUpPerson.getId(), is(resultId));
            tx.success();
        }
	}

	@Test
	public void testRepositoryStyle2IsCreatedCorrectly() {

		GraphDatabase database = container.getInstance(GraphDatabase.class);
		RepositoryClient client = container.getInstance(RepositoryClient.class);
		CdiPersonRepository2 repository = client.repository2;

		assertThat(repository, is(notNullValue()));

		Person person = null;
		Person result = null;

		try (Transaction tx = database.beginTx()) {
			repository.deleteAll();

			person = new Person("Simon", 28);
			result = repository.save(person);
			tx.success();
		}

        try (Transaction tx = database.beginTx()) {
            assertThat(result, is(notNullValue()));
            Long resultId = result.getId();
            Person lookedUpPerson = repository.findOne(person.getId());
            assertThat(lookedUpPerson.getId(), is(resultId));
            tx.success();
        }
	}

	@Test
	public void neo4jCrudRepositorySubTypeWorks() {

		GraphDatabase database = container.getInstance(GraphDatabase.class);
		RepositoryClient client = container.getInstance(RepositoryClient.class);
		CdiPersonRepository3 repository = client.repository3;

		assertThat(repository, is(notNullValue()));

		Person person = null;
		Person result = null;

		try (Transaction tx = database.beginTx()) {
			repository.deleteAll();

			person = new Person("Simon", 28);
			result = repository.save(person);
			tx.success();
		}

        try (Transaction tx = database.beginTx()) {
            assertThat(result, is(notNullValue()));
            Long resultId = result.getId();
            Person lookedUpPerson = repository.findOne(person.getId());
            assertThat(lookedUpPerson.getId(), is(resultId));
            tx.success();
        }

	}

	/**
	 * @see DATAGRAPH-500
	 */
	@Test
	public void returnOneFromCustomImpl() {

		GraphDatabase database = container.getInstance(GraphDatabase.class);

		try (Transaction tx = database.beginTx()) {

			RepositoryClient client = container.getInstance(RepositoryClient.class);
			assertThat(client.samplePersonRepository.returnOne(), is(1));
		}
	}
}
