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
package org.springframework.data.neo4j.repository.query;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.parser.Part;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link CypherQueryBuilder}.
 * 
 * @author Oliver Gierke
 */
public class CypherQueryBuilderUnitTests {

    CypherQueryBuilder query;
    private final String CLASS_NAME = Person.class.getSimpleName();
    private String DEFAULT_START_CLAUSE = "START `person`=node:__types__(className=\"" + CLASS_NAME + "\")";

    @Before
    public void setUp() {
        Neo4jMappingContext context = new Neo4jMappingContext();
        query = new CypherQueryBuilder(context, Person.class, null);
    }

    @Test
    public void createsQueryForSimplePropertyReference() {

        Part part = new Part("name", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is("START `person`=node:`Person`(`name`={0}) RETURN `person`"));
    }

    @Test
    public void createsQueryForLikePropertyIndex() {

        Part part = new Part("titleLike", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is("START `person`=node:`title`({0}) RETURN `person`"));
    }
    @Test
    public void createsQueryForLikeProperty() {

        Part part = new Part("infoLike", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is(DEFAULT_START_CLAUSE+" WHERE `person`.`info`! =~ {0} RETURN `person`"));
    }
    @Test
    public void createsQueryForGreaterThanPropertyReference() {

        Part part = new Part("ageGreaterThan", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is(DEFAULT_START_CLAUSE+" WHERE `person`.`age`! > {0} RETURN `person`"));
    }
    @Test
    public void createsQueryForTwoPropertyExpressions() {

        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("info", Person.class));

        assertThat(query.toString(), is(DEFAULT_START_CLAUSE+" WHERE `person`.`age`! > {0} AND `person`.`info`! = {1} RETURN `person`"));
    }

    @Test
    public void createsQueryForIsNullPropertyReference() {

        Part part = new Part("ageIsNull", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is(DEFAULT_START_CLAUSE+" WHERE `person`.`age`! is null  RETURN `person`"));
    }

    @Test
    public void createsQueryForPropertyOnRelationShipReference() {

        Part part = new Part("group.name", Person.class);
        query.addRestriction(part);

        assertThat(query.toString(), is("START `person_group`=node:`Group`(`name`={0}) MATCH `person`<-[:`members`]-`person_group` RETURN `person`"));
    }

    @Test
    public void createsQueryForMultipleStartClauses() {

        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("group.name", Person.class));

        assertThat(query.toString(),
                is("START `person`=node:`Person`(`name`={0}), `person_group`=node:`Group`(`name`={1}) MATCH `person`<-[:`members`]-`person_group` RETURN `person`"));
    }

    @Test
    public void createsSimpleWhereClauseCorrectly() {

        query.addRestriction(new Part("age", Person.class));

        final String className = Person.class.getName();
        assertThat(query.toString(), is(DEFAULT_START_CLAUSE +" WHERE `person`.`age`! = {0} RETURN `person`"));
    }
    @Test
    public void createsSimpleTraversalClauseCorrectly() {
        query.addRestriction(new Part("group", Person.class));
        assertThat(query.toString(), is(DEFAULT_START_CLAUSE + " MATCH `person`<-[:`members`]-`person_group` RETURN `person`"));
    }


    @Test
    public void buildsComplexQueryCorrectly() {

        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("groupName", Person.class));
        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("groupMembersAge", Person.class));

        assertThat(query.toString(), is(
                "START `person`=node:`Person`(`name`={0}), `person_group`=node:`Group`(`name`={1}) " +
                        "MATCH `person`<-[:`members`]-`person_group`, `person`<-[:`members`]-`person_group`-[:`members`]->`person_group_members` " +
                        "WHERE `person`.`age`! > {2} AND `person_group_members`.`age`! = {3} " +
                        "RETURN `person`"
                ));
    }

    @Test
    public void buildsQueryWithSort() {
        query.addRestriction(new Part("name",Person.class));
        assertThat(query.buildQuery(new Sort("person.name")).toQueryString(), is("START `person`=node:`Person`(`name`={0}) RETURN `person` ORDER BY person.name ASC"));
    }
    @Test
    public void buildsQueryWithTwoSorts() {
        query.addRestriction(new Part("name",Person.class));
        Sort sort = new Sort(new Sort.Order("person.name"),new Sort.Order(Sort.Direction.DESC, "person.age"));
        assertThat(query.buildQuery(sort).toQueryString(), is("START `person`=node:`Person`(`name`={0}) RETURN `person` ORDER BY person.name ASC,person.age DESC"));
    }

    @Test
    public void buildsQueryWithPage() {
        query.addRestriction(new Part("name",Person.class));
        Pageable pageable = new PageRequest(3,10,new Sort("person.name"));
        assertThat(query.buildQuery().toQueryString(pageable), is("START `person`=node:`Person`(`name`={0}) RETURN `person` ORDER BY person.name ASC SKIP 30 LIMIT 10"));
    }
}
