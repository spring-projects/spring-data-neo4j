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
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.typerepresentation.LabelBasedNodeTypeRepresentationStrategy;
import org.springframework.data.repository.query.parser.Part;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link org.springframework.data.neo4j.repository.query.CypherQueryBuilder}
 * Specifically where the Label Type Representation Strategy is being used.
 *
 * @author Oliver Gierke & Nicki Watt
 */
public class CypherQueryBuilderForLabelBasedTRSUnitTests extends AbstractCypherQueryBuilderTestBase {

    private final static String DEFAULT_MATCH_STARTING_CLAUSE = "MATCH (`person`:`" + CLASS_NAME + "`)";

    @Before
    public void setUp() {
        super.setUp();
    }

    @Override
    protected void finishMock(Neo4jTemplate template) {
        Mockito.when(template.isLabelBased()).thenReturn(true);
    }

    @Override
    @Test
    public void createsQueryForLikeProperty() {
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_STARTING_CLAUSE +" WHERE `person`.`info` =~ {0} RETURN `person`";
        super.createsQueryForLikeProperty();
    }

    @Override
    @Test
    public void createsQueryForGreaterThanPropertyReference() {
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_STARTING_CLAUSE +" WHERE `person`.`age` > {0} RETURN `person`";
        super.createsQueryForGreaterThanPropertyReference();
    }

    @Override
    @Test
    public void createsQueryForTwoPropertyExpressions() {
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_STARTING_CLAUSE +" WHERE `person`.`age` > {0} AND `person`.`info` = {1} RETURN `person`";
        super.createsQueryForTwoPropertyExpressions();
    }

    @Override
    @Test
    public void createsQueryForIsNullPropertyReference() {
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_STARTING_CLAUSE +" WHERE `person`.`age` is null  RETURN `person`";
        super.createsQueryForIsNullPropertyReference();
    }

    @Override
    @Test
    public void createsQueryForPropertyOnRelationShipReference() {
        Part part = new Part("group.name", Person.class);
        query.addRestriction(part);
        assertThat(query.toString(), is( "MATCH (`person`)<-[:`members`]-(`person_group`) WHERE `person_group`.`name` = {0} RETURN `person`"));
    }

    @Override
    @Test
    public void createsQueryForMultipleStartClauses() {
        this.trsSpecificExpectedQuery =
                   "MATCH (`person`)<-[:`members`]-(`person_group`) WHERE `person`.`name` = {0} AND `person_group`.`name` = {1} RETURN `person`";
        super.createsQueryForMultipleStartClauses();
    }

    @Override
    @Test
    public void createsSimpleWhereClauseCorrectly() {
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_STARTING_CLAUSE +" WHERE `person`.`age` = {0} RETURN `person`";
        super.createsSimpleWhereClauseCorrectly();
    }

    @Override
    @Test
    public void createsSimpleTraversalClauseCorrectly() {
        this.trsSpecificExpectedQuery = "MATCH (`person`)<-[:`members`]-(`person_group`) WHERE id(`person_group`) = {0} AND `person`:`Person` RETURN `person`";
        super.createsSimpleTraversalClauseCorrectly();
    }

    @Override
    @Test
    public void buildsComplexQueryCorrectly() {
        this.trsSpecificExpectedQuery =
                "MATCH (`person`)<-[:`members`]-(`person_group`), (`person`)<-[:`members`]-(`person_group`)-[:`members`]->(`person_group_members`) WHERE `person`.`name` = {0} AND `person_group`.`name` = {1} AND `person`.`age` > {2} AND `person_group_members`.`age` = {3} RETURN `person`";
        super.buildsComplexQueryCorrectly();

    }

    @Override
    @Test
    public void shouldFindByNodeEntity() throws Exception {
        this.trsSpecificExpectedQuery = "MATCH (`person`)-[:`owns`]->(`person_pet`) WHERE id(`person_pet`) = {0} AND `person`:`Person` RETURN `person`";
        super.shouldFindByNodeEntity();
    }

    @Override
    @Test
    public void shouldFindByNodeEntityForIncomingRelationship() {
        this.trsSpecificExpectedQuery = "MATCH (`person`)<-[:`members`]-(`person_group`) WHERE id(`person_group`) = {0} AND `person`:`Person` RETURN `person`";
        super.shouldFindByNodeEntityForIncomingRelationship();
    }

}
