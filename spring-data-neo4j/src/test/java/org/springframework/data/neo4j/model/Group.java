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

package org.springframework.data.neo4j.model;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.GraphTraversal;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@NodeEntity
@TypeAlias("g")
public class Group implements  IGroup , Serializable {

    private static final long serialVersionUID = 1L;

    public final static String OTHER_NAME_INDEX = "other_name";
    public static final String SEARCH_GROUPS_INDEX = "search_groups";
    public static final String SEARCH_GROUPS_INDEX_BUG = "search-groups";

    @RelatedTo(direction = Direction.OUTGOING)
    private Collection<Person> persons;

    @Fetch @RelatedTo
    private Collection<Person> fetchedPersons;

    @RelatedTo(type = "persons", elementClass = Person.class)
    private Iterable<Person> readOnlyPersons;

    @GraphTraversal(traversal = PeopleTraversalBuilder.class, params = "persons")
    transient private Iterable<Person> people;

    @GraphProperty
    @Indexed
    private String name;

    @GraphProperty
    @Indexed
    private Boolean admin;

    @GraphProperty
    private String unindexedName;

    private String unindexedName2;

    @GraphProperty
    @Indexed(indexName = SEARCH_GROUPS_INDEX, indexType = IndexType.FULLTEXT)
    private String fullTextName;

    @GraphProperty
    @Indexed(indexName = SEARCH_GROUPS_INDEX_BUG, indexType = IndexType.FULLTEXT)
    private String fullTextNameBug;

    @Indexed(fieldName = OTHER_NAME_INDEX)
    private String otherName;

    @Indexed(level = Indexed.Level.GLOBAL,indexType = IndexType.SIMPLE)
    private String globalName;

    @Indexed(level = Indexed.Level.CLASS)
    private String classLevelName;

    @Indexed(level = Indexed.Level.INSTANCE)
    private String indexLevelName;

    public String getFullTextNameBug() {
        return fullTextNameBug;
    }

    public void setFullTextNameBug(String fullTextNameBug) {
        this.fullTextNameBug = fullTextNameBug;
    }

    @GraphId
    private Long id;

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

    public void setPersons(Set<Person> persons) {
        this.persons = persons;
    }

    public void addPerson(Person person) {
        if (persons==null) persons=new HashSet<Person>();
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

    public Long getId() {
        return id;
    }

    private static class PeopleTraversalBuilder implements FieldTraversalDescriptionBuilder {
        @SuppressWarnings("deprecation")
        @Override
        public TraversalDescription build(Object start, Neo4jPersistentProperty property, String... params) {
            //return new TraversalDescriptionImpl().relationships(DynamicRelationshipType.withName(params[0])).filter(Traversal.returnAllButStartNode());
            return Traversal.description().relationships(DynamicRelationshipType.withName(params[0])).evaluator(Evaluators.excludeStartPosition());
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

    public void setGlobalName(String globalName) {
        this.globalName = globalName;
    }

    public void setClassLevelName(String classLevelName) {
        this.classLevelName = classLevelName;
    }

    public void setIndexLevelName(String indexLevelName) {
        this.indexLevelName = indexLevelName;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        Group that = (Group) obj;

        return ObjectUtils.nullSafeEquals(this.id, that.id);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.id);
    }

    public Collection<Person> getFetchedPersons() {
        return fetchedPersons;
    }
}
