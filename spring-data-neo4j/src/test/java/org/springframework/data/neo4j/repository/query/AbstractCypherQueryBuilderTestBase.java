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
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.repository.query.CypherQueryBuilder;
import org.springframework.data.neo4j.repository.query.Person;
import org.springframework.data.neo4j.support.Infrastructure;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.typerepresentation.IndexBasedNodeTypeRepresentationStrategy;
import org.springframework.data.repository.query.parser.Part;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Base Unit test definitions for {@link org.springframework.data.neo4j.repository.query.CypherQueryBuilder}.
 * Depending on which Type Representation Strategy is in use, the queries will look
 * different, thus there are currently two subclasses which deal with each case.
 * TODO - There is some consolidation which can still be done here
 *
 * @author Oliver Gierke & Nicki Watt
 */
public abstract class AbstractCypherQueryBuilderTestBase {

    // Allow subclasses to provide specific expectations
    protected String   trsSpecificExpectedQuery;
    protected CypherQueryBuilder query;

    final static String CLASS_NAME = Person.class.getSimpleName();

    @Before
    public void setUp() {
        Neo4jMappingContext context = new Neo4jMappingContext();
        Neo4jTemplate template = Mockito.mock(Neo4jTemplate.class);
        finishMock(template);
        this.query = new CypherQueryBuilder(context, Person.class, template);
        this.trsSpecificExpectedQuery = null;
    }

    protected void finishMock(Neo4jTemplate template) {
    }


    @Test
    public void createsQueryForSimplePropertyReference() {
        Part part = new Part("name", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(),
                is(getExpectedQuery("MATCH (`person`:`Person`) WHERE `person`.`name` = {0} RETURN `person`")));
    }
    @Test
    public void createsQueryForSimpleIndexedPropertyReference() {
        Part part = new Part("name2", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(),
                is(getExpectedQuery("START `person`=node:`Person`(`name2`={0}) RETURN `person`")));
    }

    @Test
    public void createsQueryForLikePropertyIndex() {
        Part part = new Part("titleLike", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(),
                is(getExpectedQuery("START `person`=node:`title`({0}) RETURN `person`")));
    }

    @Test
    public void createsQueryForLikeProperty() {
        Part part = new Part("infoLike", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void createsQueryForGreaterThanPropertyReference() {
        Part part = new Part("ageGreaterThan", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void createsQueryForTwoPropertyExpressions() {
        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("info", Person.class));
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void createsQueryForIsNullPropertyReference() {
        Part part = new Part("ageIsNull", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void createsQueryForPropertyOnRelationShipReference() {
        Part part = new Part("group.name", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void createsQueryForMultipleStartClauses() {
        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("group.name", Person.class));
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void createsSimpleWhereClauseCorrectly() {
        query.addRestriction(new Part("age", Person.class));
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }


    @Test
    public void createsSimpleTraversalClauseCorrectly() {
        query.addRestriction(new Part("group", Person.class));
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }


    @Test
    public void buildsComplexQueryCorrectly() {
        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("groupName", Person.class));
        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("groupMembersAge", Person.class));
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void buildsQueryWithSort() {
        query.addRestriction(new Part("name",Person.class));
        String queryString = query.buildQuery(new Sort("person.name")).toQueryString();
        assertThat(queryString, is("MATCH (`person`:`Person`) WHERE `person`.`name` = {0} RETURN `person` ORDER BY person.name ASC"));
    }

    @Test
    public void buildsQueryWithTwoSorts() {
        query.addRestriction(new Part("name",Person.class));
        Sort sort = new Sort(new Sort.Order("person.name"),new Sort.Order(Sort.Direction.DESC, "person.age"));
        String queryString = query.buildQuery(sort).toQueryString();
        assertThat(queryString, is("MATCH (`person`:`Person`) WHERE `person`.`name` = {0} RETURN `person` ORDER BY person.name ASC,person.age DESC"));
    }

    @Test
    public void buildsQueryWithPage() {
        query.addRestriction(new Part("name",Person.class));
        Pageable pageable = new PageRequest(3,10,new Sort("person.name"));
        String queryString = query.buildQuery().toQueryString(pageable);
        assertThat(queryString, is("MATCH (`person`:`Person`) WHERE `person`.`name` = {0} RETURN `person` ORDER BY person.name ASC SKIP 30 LIMIT 10"));
    }

    @Test
    public void shouldFindByNodeEntity() throws Exception {
        query.addRestriction(new Part("pet", Person.class));
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    @Test
    public void shouldFindByNodeEntityForIncomingRelationship() {
        query.addRestriction(new Part("group", Person.class));
        assertThat(query.toString(), is( getExpectedQuery("trs-specific-test-subclass-expected-to-set-value")));
    }

    /**
     * This Abstract class defines the template for how to test, however
     * gives subclasses an opportunity to override the expected query
     * if it is different / specific for the TRS being used.
     * This method will either return the trs specific query string if
     * this was set, otherwise the default value passed in.
     */
    private String getExpectedQuery(String defaultQueryString) {
        return (this.trsSpecificExpectedQuery != null)
                ? this.trsSpecificExpectedQuery
                : defaultQueryString;
    }
}
