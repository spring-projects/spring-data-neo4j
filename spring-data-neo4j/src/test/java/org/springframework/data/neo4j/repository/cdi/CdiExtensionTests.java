/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.data.neo4j.repository.cdi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Optional;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.data.neo4j.examples.friends.domain.Person;

/**
 * Integration tests for {@link org.springframework.data.neo4j.repository.cdi.Neo4jCdiRepositoryExtension}.
 *
 * @author Mark Paluch
 * @see DATAGRAPH-879, DATAGRAPH-1028
 */
public class CdiExtensionTests extends MultiDriverTestClass {

	static SeContainer container;

	@BeforeClass
	public static void setUp() throws Exception {

		setupMultiDriverTestEnvironment();

		// Prevent the Jersey extension to interact with the InitialContext
		System.setProperty("com.sun.jersey.server.impl.cdi.lookupExtensionInBeanManager", "true");

		container = SeContainerInitializer.newInstance() //
				.disableDiscovery() //
				.addPackages(RepositoryClient.class) //
				.initialize();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		container.close();
	}

	/**
	 * @see DATAGRAPH-879, DATAGRAPH-1028
	 */
	@Test
	public void regularRepositoryShouldWork() {

		RepositoryClient client = container.select(RepositoryClient.class).get();
		CdiPersonRepository repository = client.repository;

		assertThat(repository, is(notNullValue()));

		Person person = null;
		Person result = null;

		repository.deleteAll();

		person = new Person();
		person.setFirstName("Homer");
		person.setLastName("Simpson");

		result = repository.save(person);

		assertThat(result, is(notNullValue()));
		Long resultId = result.getId();
		Optional<Person> lookedUpPerson = repository.findByLastName(person.getLastName());
		assertTrue(lookedUpPerson.isPresent());
		lookedUpPerson.ifPresent(actual -> assertThat(actual.getId(), is(resultId)));
	}

	/**
	 * @see DATAGRAPH-879, DATAGRAPH-1028
	 */
	@Test
	public void repositoryWithQualifiersShouldWork() {

		RepositoryClient client = container.select(RepositoryClient.class).get();
		client.qualifiedPersonRepository.deleteAll();

		assertEquals(0, client.qualifiedPersonRepository.count());
	}

	/**
	 * @see DATAGRAPH-879, DATAGRAPH-1028
	 */
	@Test
	public void repositoryWithCustomImplementationShouldWork() {

		RepositoryClient client = container.select(RepositoryClient.class).get();

		assertEquals(1, client.samplePersonRepository.returnOne());
	}
}
