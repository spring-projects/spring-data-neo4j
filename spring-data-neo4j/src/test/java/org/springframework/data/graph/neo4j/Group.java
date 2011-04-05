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

package org.springframework.data.graph.neo4j;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.data.graph.*;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.GraphTraversal;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.annotation.Indexed;

import static org.springframework.data.graph.neo4j.Person.persistedPerson;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

import java.lang.reflect.Field;
import java.util.Collection;

@NodeEntity
public class Group {

    public final static String OTHER_NAME_INDEX="other_name";
    public static final String SEARCH_GROUPS_INDEX = "search-groups";

    @RelatedTo(direction = Direction.OUTGOING, elementClass = Person.class)
    private Collection<Person> persons;

    @RelatedTo(type = "persons", elementClass = Person.class)
    private Iterable<Person> readOnlyPersons;

    @GraphTraversal(traversalBuilder = PeopleTraversalBuilder.class, elementClass = Person.class, params = "persons")
    private Iterable<Person> people;

    @GraphProperty
    @Indexed
    private String name;

    @GraphProperty
    private String unindexedName;

    private String unindexedName2;

    @GraphProperty
    @Indexed(indexName = SEARCH_GROUPS_INDEX, fulltext = true)
    private String fullTextName;

    @Indexed(fieldName = OTHER_NAME_INDEX)
    private String otherName;

    public String getFullTextName() {
        return fullTextName;
    }

    public void setFullTextName(String fullTextName) {
        this.fullTextName = fullTextName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPersons(Collection<Person> persons) {
        this.persons = persons;
    }

    public void addPerson(Person person) {
        persons.add(person);
    }

    public Collection<Person> getPersons() {
        return persons;
    }

    public Iterable<Person> getReadOnlyPersons() {
        return readOnlyPersons;
    }

    public void setReadOnlyPersons(Iterable<Person> p) {
        readOnlyPersons = p;
    }

    public Iterable<Person> getPeople() {
        return people;
    }

    public void setOtherName(String otherName) {
        this.otherName = otherName;
    }

    private static class PeopleTraversalBuilder implements FieldTraversalDescriptionBuilder {
        @Override
        public TraversalDescription build(NodeBacked start, Field field, String...params) {
            return new TraversalDescriptionImpl()
                    .relationships(DynamicRelationshipType.withName(params[0]))
                    .filter(Traversal.returnAllButStartNode());

        }
    }

    public String getUnindexedName() {
        return unindexedName;
    }

    public void setUnindexedName(String unindexedName) {
        this.unindexedName = unindexedName;
    }

    public String getUnindexedName2() {
        return unindexedName2;
    }

    public void setUnindexedName2(String unindexedName2) {
        this.unindexedName2 = unindexedName2;
    }
}
