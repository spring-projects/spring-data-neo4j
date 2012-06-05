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
package org.springframework.data.neo4j.annotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.PersonRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-via-test-context.xml"})
@Transactional
public class RelatedToViaTests {
    @Autowired
    private PersonRepository personRepository;

    @Before
    public void before() {
        personRepository.deleteAll();
    }

    @Test
    public void shouldMapRelationshipAsFirstClassEditableCitizen() throws Exception {
        Person ronald = personRepository.save(new Person("Ronald", 13));
        Person hermione = personRepository.save(new Person("Hermione", 12));
        Person harry = new Person("Harry", 14);
        harry.wentToSchoolWith(ronald);
        harry.wentToSchoolWith(hermione);

        personRepository.save(harry);

        harry = personRepository.findOne(harry.getId());
        assertThat(asList(ronald.getId(), hermione.getId()), hasItem(harry.getSchoolMates().iterator().next().getPerson2().getId()));
    }

    @Test
    public void shouldValidateEndNode() throws Exception {
        Person billy = new Person("Billy", 42);
        billy.wentToSchoolWith(null);

        try {
            personRepository.save(billy);

            fail();
        } catch (InvalidDataAccessApiUsageException e) {
            assertThat(e.getCause().getMessage(), is(equalTo("End node must not be null (org.springframework.data.neo4j.model.Friendship)")));
        }
    }
}
