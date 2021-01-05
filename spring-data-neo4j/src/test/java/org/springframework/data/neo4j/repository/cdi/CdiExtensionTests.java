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
package org.springframework.data.neo4j.repository.cdi;

import java.util.Optional;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.springframework.data.neo4j.examples.friends.domain.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link org.springframework.data.neo4j.repository.cdi.Neo4jCdiRepositoryExtension}.
 *
 * @author Mark Paluch
 * @author Michael J. Simons
 */
public class CdiExtensionTests {

	static ServerControls neo4jTestServer;
	static SeContainer container;

	@BeforeClass
	public static void setUp() {

		neo4jTestServer = TestServerBuilders.newInProcessBuilder().newServer();

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
		neo4jTestServer.close();
	}

	@Test // DATAGRAPH-879, DATAGRAPH-1028
	public void regularRepositoryShouldWork() {

		RepositoryClient client = container.select(RepositoryClient.class).get();
		CdiPersonRepository repository = client.repository;

		assertThat(repository).isNotNull();

		Person person = null;
		Person result = null;

		repository.deleteAll();

		person = new Person();
		person.setFirstName("Homer");
		person.setLastName("Simpson");

		result = repository.save(person);

		assertThat(result).isNotNull();
		Long resultId = result.getId();
		Optional<Person> lookedUpPerson = repository.findByLastName(person.getLastName());
		assertThat(lookedUpPerson.isPresent()).isTrue();
		lookedUpPerson.ifPresent(actual -> assertThat(actual.getId()).isEqualTo(resultId));
	}

	@Test // DATAGRAPH-879, DATAGRAPH-1028
	public void repositoryWithQualifiersShouldWork() {

		RepositoryClient client = container.select(RepositoryClient.class).get();
		client.qualifiedPersonRepository.deleteAll();

		assertThat(client.qualifiedPersonRepository.count()).isEqualTo(0);
	}

	@Test // DATAGRAPH-879, DATAGRAPH-1028
	public void repositoryWithCustomImplementationShouldWork() {

		RepositoryClient client = container.select(RepositoryClient.class).get();

		assertThat(client.samplePersonRepository.returnOne()).isEqualTo(1);
	}
}
