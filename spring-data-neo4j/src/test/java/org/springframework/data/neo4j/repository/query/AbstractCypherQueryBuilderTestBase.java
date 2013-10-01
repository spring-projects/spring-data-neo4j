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

    CypherQueryBuilder query;
    String queryString;
    final static String CLASS_NAME = Person.class.getSimpleName();

    @Before
    public void setUp() {
        Neo4jMappingContext context = new Neo4jMappingContext();
        Neo4jTemplate template = Mockito.mock(Neo4jTemplate.class);
        Infrastructure inf = Mockito.mock(Infrastructure.class);
        when (template.getInfrastructure()).thenReturn(inf);
        when (inf.getNodeTypeRepresentationStrategy()).thenReturn(getNodeTypeRepresentationStrategy());
        query = new CypherQueryBuilder(context, Person.class, template);
        queryString = null;
    }

    abstract NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy();

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForSimplePropertyReference()
     */
    @Test
    public abstract void createsQueryForSimplePropertyReference();
    protected void buildQueryForCreatesQueryForSimplePropertyReference()
    {
        Part part = new Part("name", Person.class);
        query.addRestriction(part);
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForLikePropertyIndex()
     */
    @Test
    public abstract void createsQueryForLikePropertyIndex();
    protected void buildQueryForCreatesQueryForLikePropertyIndex() {
        Part part = new Part("titleLike", Person.class);
        query.addRestriction(part);
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForLikeProperty()
     */
    @Test
    public abstract void createsQueryForLikeProperty();
    public void buildQueryForCreatesQueryForLikeProperty() {
        Part part = new Part("infoLike", Person.class);
        query.addRestriction(part);
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForGreaterThanPropertyReference()
     */
    @Test
    public abstract void createsQueryForGreaterThanPropertyReference();
    public void buildQueryForCreatesQueryForGreaterThanPropertyReference() {
        Part part = new Part("ageGreaterThan", Person.class);
        query.addRestriction(part);
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForTwoPropertyExpressions()
     */
    @Test
    public abstract void createsQueryForTwoPropertyExpressions();
    public void buildQueryForCreatesQueryForTwoPropertyExpressions() {
        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("info", Person.class));
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForIsNullPropertyReference()
     */
    @Test
    public abstract void createsQueryForIsNullPropertyReference();
    public void buildQueryForCreatesQueryForIsNullPropertyReference() {
        Part part = new Part("ageIsNull", Person.class);
        query.addRestriction(part);
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForPropertyOnRelationShipReference()
     */
    @Test
    public abstract void createsQueryForPropertyOnRelationShipReference();
    public void buildQueryForCreatesQueryForPropertyOnRelationShipReference() {
        Part part = new Part("group.name", Person.class);
        query.addRestriction(part);
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesQueryForMultipleStartClauses()
     */
    @Test
    public abstract void createsQueryForMultipleStartClauses();
    public void buildQueryForCreatesQueryForMultipleStartClauses() {
        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("group.name", Person.class));
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesSimpleWhereClauseCorrectly()
     */
    @Test
    public abstract void createsSimpleWhereClauseCorrectly();
    public void buildQueryForCreatesSimpleWhereClauseCorrectly() {
        query.addRestriction(new Part("age", Person.class));
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForCreatesSimpleTraversalClauseCorrectly()
     */
    @Test
    public abstract void createsSimpleTraversalClauseCorrectly();
    public void buildQueryForCreatesSimpleTraversalClauseCorrectly() {
        query.addRestriction(new Part("group", Person.class));
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForBuildsComplexQueryCorrectly()
     */
    @Test
    public abstract void buildsComplexQueryCorrectly();
    public void buildQueryForBuildsComplexQueryCorrectly() {
        query.addRestriction(new Part("name", Person.class));
        query.addRestriction(new Part("groupName", Person.class));
        query.addRestriction(new Part("ageGreaterThan", Person.class));
        query.addRestriction(new Part("groupMembersAge", Person.class));
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForBuildsQueryWithSort()
     */
    @Test
    public abstract void buildsQueryWithSort();
    public void buildQueryForBuildsQueryWithSort() {
        query.addRestriction(new Part("name",Person.class));
        queryString = query.buildQuery(new Sort("person.name")).toQueryString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForBuildsQueryWithSort()
     */
    @Test
    public abstract void buildsQueryWithTwoSorts();
    public void buildQueryForBuildsQueryWithTwoSorts() {
        query.addRestriction(new Part("name",Person.class));
        Sort sort = new Sort(new Sort.Order("person.name"),new Sort.Order(Sort.Direction.DESC, "person.age"));
        queryString = query.buildQuery(sort).toQueryString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForBuildsQueryWithSort()
     */
    @Test
    public abstract void buildsQueryWithPage();
    public void buildQueryForBuildsQueryWithPage() {
        query.addRestriction(new Part("name",Person.class));
        Pageable pageable = new PageRequest(3,10,new Sort("person.name"));
        queryString = query.buildQuery().toQueryString(pageable);
    }

    /**
     * To be used in conjunction with
     * buildQueryForShouldFindByNodeEntityForIncomingRelationship()
     */
    @Test
    public abstract void shouldFindByNodeEntity() throws Exception;
    public void buildQueryForShouldFindByNodeEntity() throws Exception {
        query.addRestriction(new Part("pet", Person.class));
        queryString = query.toString();
    }

    /**
     * To be used in conjunction with
     * buildQueryForShouldFindByNodeEntityForIncomingRelationship()
     */
    @Test
    public abstract void shouldFindByNodeEntityForIncomingRelationship();
    public void buildQueryForShouldFindByNodeEntityForIncomingRelationship() {
        query.addRestriction(new Part("group", Person.class));
        queryString = query.toString();
    }
}
