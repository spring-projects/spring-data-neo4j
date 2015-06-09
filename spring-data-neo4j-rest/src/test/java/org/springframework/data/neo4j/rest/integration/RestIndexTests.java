/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.rest.integration;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.support.IndexTests;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

/**
 * @author mh
 * @since 28.03.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
        "classpath:RestTests-context-index.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RestIndexTests extends IndexTests {

    @BeforeClass
    public static void startDb() throws Exception {
        RestTestBase.startDb();
    }

    @Before
    public void cleanDb() {
        RestTestBase.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        RestTestBase.shutdownDb();

    }

    @Test
    public void testOutsideRangeQueryPersonByIndexOnAnnotatedField() {
        persistedPerson(NAME_VALUE, 35);
        Iterable<Person> emptyResult = this.personRepository.findAllByRange("age", 0, 34);
        assertFalse("nothing found outside range", emptyResult.iterator().hasNext());
    }


    @Test
    public void testRangeQueryPersonByIndexOnAnnotatedField() {
        Person person = persistedPerson(NAME_VALUE, 35);
        final Person found = this.personRepository.findAllByRange("age", 10, 40).iterator().next();
        assertEquals("person found inside range", person, found);
    }
}
