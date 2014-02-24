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
import org.springframework.data.repository.query.parser.Part;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link org.springframework.data.neo4j.repository.query.CypherQueryBuilder}
 * Specifically where the Index Based Type Representation Strategy is being used.
 *
 * @author Oliver Gierke & Nicki Watt
 */
public class CypherQueryBuilderForIndexBasedTRSUnitTests extends AbstractCypherQueryBuilderTestBase {

    private final static String DEFAULT_START_CLAUSE = "START `person`=node:__types__(className=\"" + CLASS_NAME + "\")";

    @Before
    public void setUp() {
        super.setUp();
    }

    @Override
    @Test
    public void createsQueryForLikeProperty() {
        this.trsSpecificExpectedQuery = DEFAULT_START_CLAUSE+" WHERE `person`.`info` =~ {0} RETURN `person`";
        super.createsQueryForLikeProperty();
    }

    @Override
    @Test
    public void createsQueryForGreaterThanPropertyReference() {
        this.trsSpecificExpectedQuery = DEFAULT_START_CLAUSE+" WHERE `person`.`age` > {0} RETURN `person`";
        super.createsQueryForGreaterThanPropertyReference();
    }

    @Override
    @Test
    public void createsQueryForTwoPropertyExpressions() {
        this.trsSpecificExpectedQuery = DEFAULT_START_CLAUSE+" WHERE `person`.`age` > {0} AND `person`.`info` = {1} RETURN `person`";
        super.createsQueryForTwoPropertyExpressions();
    }

    @Override
    @Test
    public void createsQueryForIsNullPropertyReference() {
        this.trsSpecificExpectedQuery = DEFAULT_START_CLAUSE+" WHERE `person`.`age` is null  RETURN `person`";
        super.createsQueryForIsNullPropertyReference();
    }

    @Override
    @Test
    public void createsQueryForPropertyOnRelationShipReference() {
        this.trsSpecificExpectedQuery = "START `person_group`=node:`Group`(`name2`={0}) MATCH (`person`)<-[:`members`]-(`person_group`) RETURN `person`";
        Part part = new Part("group.name2", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(), is( trsSpecificExpectedQuery));
    }

    @Override
    @Test
    public void createsQueryForMultipleStartClauses() {
        this.trsSpecificExpectedQuery = "START `person`=node:`Person`(`name2`={0}), `person_group`=node:`Group`(`name2`={1}) MATCH (`person`)<-[:`members`]-(`person_group`) RETURN `person`";
        query.addRestriction(new Part("name2", Person.class));
        query.addRestriction(new Part("group.name2", Person.class));
        assertThat(query.toString(), is( trsSpecificExpectedQuery));
    }

    @Override
    public void createsQueryForSimplePropertyReference() {
        Part part = new Part("name2", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(),
                is("START `person`=node:`Person`(`name2`={0}) RETURN `person`"));
    }

    @Override
    @Test
    public void createsSimpleWhereClauseCorrectly() {
        this.trsSpecificExpectedQuery = DEFAULT_START_CLAUSE +" WHERE `person`.`age` = {0} RETURN `person`";
        super.createsSimpleWhereClauseCorrectly();
    }

    @Override
    @Test
    public void createsSimpleTraversalClauseCorrectly() {
        this.trsSpecificExpectedQuery = "START `person_group`=node({0}) MATCH (`person`)<-[:`members`]-(`person_group`) WHERE `person`.__type__ IN ['Person'] RETURN `person`";
        super.createsSimpleTraversalClauseCorrectly();
    }

    @Override
    @Test
    public void buildsComplexQueryCorrectly() {
        this.trsSpecificExpectedQuery =
                        "START `person`=node:`Person`(`name2`={0}), `person_group`=node:`Group`(`name2`={1}) " +
                        "MATCH (`person`)<-[:`members`]-(`person_group`), (`person`)<-[:`members`]-(`person_group`)-[:`members`]->(`person_group_members`) " +
                        "WHERE `person`.`age` > {2} AND `person_group_members`.`age` = {3} " +
                        "RETURN `person`";
        query.addRestriction(new Part("name2", Person.class));
        query.addRestriction(new Part("group_Name2", Person.class));
        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("groupMembersAge", Person.class));
        assertThat(query.toString(), is( trsSpecificExpectedQuery ));
    }


    @Override
    @Test
    public void shouldFindByNodeEntity() throws Exception {
        this.trsSpecificExpectedQuery = "START `person_pet`=node({0}) MATCH (`person`)-[:`owns`]->(`person_pet`) WHERE `person`.__type__ IN ['Person'] RETURN `person`";
        super.shouldFindByNodeEntity();
    }

    @Override
    @Test
    public void shouldFindByNodeEntityForIncomingRelationship() {
        this.trsSpecificExpectedQuery = "START `person_group`=node({0}) MATCH (`person`)<-[:`members`]-(`person_group`) WHERE `person`.__type__ IN ['Person'] RETURN `person`";
        super.shouldFindByNodeEntityForIncomingRelationship();
    }

    @Override
    public void buildsQueryWithSort() {
        query.addRestriction(new Part("name2",Person.class));
        String queryString = query.buildQuery(new Sort("person.name2")).toQueryString();
        assertThat(queryString, is("START `person`=node:`Person`(`name2`={0}) RETURN `person` ORDER BY person.name2 ASC"));
    }

    @Override
    public void buildsQueryWithTwoSorts() {
        query.addRestriction(new Part("name2",Person.class));
        Sort sort = new Sort(new Sort.Order("person.name2"),new Sort.Order(Sort.Direction.DESC, "person.age"));
        String queryString = query.buildQuery(sort).toQueryString();
        assertThat(queryString, is("START `person`=node:`Person`(`name2`={0}) RETURN `person` ORDER BY person.name2 ASC,person.age DESC"));
    }

    @Override
    public void buildsQueryWithPage() {
        query.addRestriction(new Part("name2",Person.class));
        Pageable pageable = new PageRequest(3,10,new Sort("person.name2"));
        String queryString = query.buildQuery().toQueryString(pageable);
        assertThat(queryString, is("START `person`=node:`Person`(`name2`={0}) RETURN `person` ORDER BY person.name2 ASC SKIP 30 LIMIT 10"));
    }
}
