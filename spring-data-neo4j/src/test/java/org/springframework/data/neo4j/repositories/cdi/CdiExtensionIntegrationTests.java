/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.data.neo4j.repositories.cdi;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.data.neo4j.examples.friends.domain.Person;

/**
 * Integration tests for {@link org.springframework.data.neo4j.repository.cdi.Neo4jCdiRepositoryExtension}.
 *
 * @author Mark Paluch
 * @see DATAGRAPH-879
 */
public class CdiExtensionIntegrationTests extends MultiDriverTestClass {

    static CdiTestContainer container;

    @BeforeClass
    public static void setUp() throws Exception {

        // Prevent the Jersey extension to interact with the InitialContext
        System.setProperty("com.sun.jersey.server.impl.cdi.lookupExtensionInBeanManager", "true");

        setupMultiDriverTestEnvironment();

        container = CdiTestContainerLoader.getCdiContainer();
        container.bootContainer();
    }

    @AfterClass
    public static void tearDown() throws Exception {

        container.shutdownContainer();

        tearDownMultiDriverTestEnvironment();
    }

    /**
     * @see DATAGRAPH-879
     */
    @Test
    @SuppressWarnings("null")
    public void regularRepositoryShouldWork() {

        RepositoryClient client = container.getInstance(RepositoryClient.class);
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
        Person lookedUpPerson = repository.findOne(person.getId());
        assertThat(lookedUpPerson.getId(), is(resultId));
    }

    /**
     * @see DATAGRAPH-879
     */
    @Test
    @SuppressWarnings("null")
    public void repositoryWithQualifiersShouldWork() {

        RepositoryClient client = container.getInstance(RepositoryClient.class);
        client.qualifiedPersonRepository.deleteAll();

        assertEquals(0, client.qualifiedPersonRepository.count());
    }

    /**
     * @see DATAGRAPH-879
     */
    @Test
    @SuppressWarnings("null")
    public void repositoryWithCustomImplementationShouldWork() {

        RepositoryClient client = container.getInstance(RepositoryClient.class);

        assertEquals(1, client.samplePersonRepository.returnOne());
    }
}
