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

package org.springframework.data.neo4j.aspects;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

@NodeEntity
public class Group {

    public final static String OTHER_NAME_INDEX="other_name";
    public static final String SEARCH_GROUPS_INDEX = "search-groups";

    @RelatedTo(direction = Direction.OUTGOING)
    private Collection<Person> persons;

    @RelatedTo(type = "persons", elementClass = Person.class)
    private Iterable<Person> readOnlyPersons;

    @GraphTraversal(traversal = PeopleTraversalBuilder.class, elementClass = Person.class, params = "persons")
    private Iterable<Person> people;

    @GraphTraversal(traversal = PeopleTraversalBuilder.class, params = "persons")
    private Iterable<Node> peopleNodes;

    @GraphTraversal(traversal = PeopleTraversalBuilder.class, params = "persons")
    private Iterable<Relationship> peopleRelationships;

    @GraphProperty
    @Indexed(indexType = IndexType.SIMPLE)
    private String name;

    @GraphProperty
    @Indexed(indexType = IndexType.SIMPLE)
    private Boolean admin;

    @Query("start n=node({self}) match n-[:persons]->() return count(*)")
    private Long memberCount;

    @RelatedToVia(type="mentors", direction = Direction.INCOMING)
    Mentorship mentorship;

    @GraphProperty
    private String unindexedName;

    private String unindexedName2;

    @GraphProperty
    @Indexed(indexName = SEARCH_GROUPS_INDEX, indexType = IndexType.FULLTEXT)
    private String fullTextName;

    @Indexed(fieldName = OTHER_NAME_INDEX,indexType = IndexType.SIMPLE)
    private String otherName;

    @Indexed(level=Indexed.Level.GLOBAL,indexType = IndexType.SIMPLE)
    private String globalName;

    @Indexed(level=Indexed.Level.CLASS,indexType = IndexType.SIMPLE)
    private String classLevelName;

    @Indexed(level=Indexed.Level.INSTANCE,indexType = IndexType.SIMPLE)
    private String indexLevelName;
    private String[] roleNames;

    @Indexed(indexType = IndexType.SIMPLE, numeric = false)
    private Byte secret;

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @GraphProperty(propertyType = Long.class)
    private Date creationDate;


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

    public void setRoleNames(String...roleNames) {
        this.roleNames=roleNames;
    }

    public String[] getRoleNames() {
        return roleNames;
    }

    public Long getMemberCount() {
        return memberCount;
    }

    public enum Role { ADMIN, USER }
    Role[] roles;

    public Role[] getRoles() {
        return roles;
    }

    public void setRoles(Role...roles) {
        this.roles = roles;
    }

    Collection<String> roleNamesColl;
    Collection<Role> rolesColl;
    Set<String> roleNamesSet;
    Set<Role> rolesSet;
    Iterable<String> roleNamesIterable;
    Iterable<Role> rolesIterable;


    private static class PeopleTraversalBuilder implements FieldTraversalDescriptionBuilder {
        @Override
        public TraversalDescription build(Object start, Neo4jPersistentProperty property, String...params) {
            return Traversal.description()
                    .relationships(DynamicRelationshipType.withName(params[0]))
                    .evaluator(Evaluators.excludeStartPosition());

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

    public Iterable<Node> getPeopleNodes() {
        return peopleNodes;
    }

    public Iterable<Relationship> getPeopleRelationships() {
        return peopleRelationships;
    }

    public Collection<String> getRoleNamesColl() {
        return roleNamesColl;
    }

    public void setRoleNamesColl(Collection<String> roleNamesColl) {
        this.roleNamesColl = roleNamesColl;
    }

    public Collection<Role> getRolesColl() {
        return rolesColl;
    }

    public void setRolesColl(Collection<Role> rolesColl) {
        this.rolesColl = rolesColl;
    }

    public Set<String> getRoleNamesSet() {
        return roleNamesSet;
    }

    public void setRoleNamesSet(Set<String> roleNamesSet) {
        this.roleNamesSet = roleNamesSet;
    }

    public Set<Role> getRolesSet() {
        return rolesSet;
    }

    public void setRolesSet(Set<Role> rolesSet) {
        this.rolesSet = rolesSet;
    }

    public Iterable<String> getRoleNamesIterable() {
        return roleNamesIterable;
    }

    public void setRoleNamesIterable(Iterable<String> roleNamesIterable) {
        this.roleNamesIterable = roleNamesIterable;
    }

    public Iterable<Role> getRolesIterable() {
        return rolesIterable;
    }

    public void setRolesIterable(Iterable<Role> rolesIterable) {
        this.rolesIterable = rolesIterable;
    }

    public Mentorship getMentorship() {
        return mentorship;
    }

    public void setMentorship(Mentorship mentorship) {
        this.mentorship = mentorship;
    }

    public Byte getSecret() {
        return secret;
    }

    public void setSecret(Byte secret) {
        this.secret = secret;
    }
}
