/*
 * Copyright 2013 the original author or authors.
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

import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link Neo4jCdiRepositoryExtension}.
 * 
 * @author Nicki Watt
 */
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

        Neo4jTemplate template = container.getInstance(Neo4jTemplate.class);
		RepositoryClient client = container.getInstance(RepositoryClient.class);
		CdiPersonRepository repository = client.getRepository();

		assertThat(repository, is(notNullValue()));

        Person person = null;
        Person result = null;

        Transaction tx = template.getGraphDatabaseService().beginTx();
        try {
            repository.deleteAll();

            person = new Person("Simon", 28);
            result = repository.save(person);
            tx.success();
        } catch (Exception e) {
            tx.failure();
        } finally {
            tx.finish();
        }

        assertThat(result, is(notNullValue()));
		Long resultId = result.getId();
        Person lookedUpPerson = repository.findOne(person.getId());
        assertThat(lookedUpPerson.getId(), is(resultId));
	}

    @Test
    public void testRepositoryStyle2IsCreatedCorrectly() {

        Neo4jTemplate template = container.getInstance(Neo4jTemplate.class);
        RepositoryClient client = container.getInstance(RepositoryClient.class);
        CdiPersonRepository2 repository = client.getRepository2();

        assertThat(repository, is(notNullValue()));

        Person person = null;
        Person result = null;

        Transaction tx = template.getGraphDatabaseService().beginTx();
        try {
            repository.deleteAll();

            person = new Person("Simon", 28);
            result = repository.save(person);
            tx.success();
        } catch (Exception e) {
            tx.failure();
        } finally {
            tx.finish();
        }

        assertThat(result, is(notNullValue()));
        Long resultId = result.getId();
        Person lookedUpPerson = repository.findOne(person.getId());
        assertThat(lookedUpPerson.getId(), is(resultId));
    }

    @Test
    @Ignore // uncomment me to see issue (Note also need to uncomment
            // @Inject in RepositoryClient
    public void demoNonWorkingRepository() {

        Neo4jTemplate template = container.getInstance(Neo4jTemplate.class);
        RepositoryClient client = container.getInstance(RepositoryClient.class);
        NonWorkingCdiPersonRepository repository = client.getNonWorkingRepository();

        assertThat(repository, is(notNullValue()));

        Person person = null;
        Person result = null;

        Transaction tx = template.getGraphDatabaseService().beginTx();
        try {
            repository.deleteAll();

            person = new Person("Simon", 28);
            result = repository.save(person);
            tx.success();
        } catch (Exception e) {
            tx.failure();
        } finally {
            tx.finish();
        }

        assertThat(result, is(notNullValue()));
        Long resultId = result.getId();
        Person lookedUpPerson = repository.findOne(person.getId());
        assertThat(lookedUpPerson.getId(), is(resultId));

    }
}
