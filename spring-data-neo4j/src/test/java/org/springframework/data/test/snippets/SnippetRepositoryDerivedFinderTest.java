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
package org.springframework.data.test.snippets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.test.DocumentingTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:DocumentingTest-context.xml"})
public class SnippetRepositoryDerivedFinderTest extends DocumentingTestBase {
    @Autowired
    Neo4jTemplate template;
    // SNIPPET derived
    @NodeEntity
    public static class Person {
        @GraphId Long id;
        private String name;
        private Group group;

        private Person(){}
        public Person(String name) {
            this.name = name;
        }
    }
    @NodeEntity
    public static class Group {
        @GraphId Long id;
        private String title;
        // incoming relationship for the person -> group
        @RelatedTo(type = "group", direction = Direction.INCOMING)
        private Set<Person> members=new HashSet<Person>();

        private Group(){}
        public Group(String title, Person...people) {
            this.title = title;
            members.addAll(asList(people));
        }
    }
    // SNIPPET derived
    /*
    // SNIPPET derived
    public interface PersonRepository extends GraphRepository<Person> {
        Iterable<Person> findByGroupTitle(String name);
    }

    // SNIPPET derived
    */
    // SNIPPET derived
    @Autowired PersonRepository personRepository;

    // SNIPPET derived
    @Autowired GroupRepository groupRepository;

    @Test
    @Transactional
    public void documentDerivedFinders() {
        title ="Derived Finder Methods";
        paragraphs = new String[] {"Use the meta information of your domain model classes to declare repository finders " +
                "that navigate along relationships and compare properties. The path defined with the method name is used to " +
                "create a Cypher query that is executed on the graph."};

        snippetTitle = "Repository and usage of derived finder methods";
        snippet = "derived";

        // SNIPPET derived
        Person oliver=personRepository.save(new Person("Oliver"));
        final Group springData = new Group("spring-data",oliver);
        groupRepository.save(springData);

        final Iterable<Person> members = personRepository.findByGroupTitle("spring-data");
        assertThat(members.iterator().next().name, is(oliver.name));
        // SNIPPET derived
    }

}
