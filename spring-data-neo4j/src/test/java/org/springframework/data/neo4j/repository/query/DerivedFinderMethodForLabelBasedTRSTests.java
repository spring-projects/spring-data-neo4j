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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.typerepresentation.LabelBasedNodeTypeRepresentationStrategy;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

/**
 * Tests for the various finder method based scenarios
 * , specifically where the Label Type Representation Strategy is being used.
 *
 * @author Oliver Gierke & Nicki Watt
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class DerivedFinderMethodForLabelBasedTRSTests extends AbstractDerivedFinderMethodTestBase {

    private static final String DEFAULT_MATCH_CLAUSE = "MATCH (`thing`:`"+THING_NAME+"`)";

    @Autowired
    NodeTypeRepresentationStrategy strategy;

    @Before
    public void setup() {
        super.setup();
        assertThat("The tests in this class should be configured to use the Label " +
                   "based Type Representation Strategy, however it is not ... ",
                strategy, instanceOf(LabelBasedNodeTypeRepresentationStrategy.class));
    }

    @Test
    @Override
    public void testQueryWithEntityGraphId() throws Exception {
        // findByOwnerId
        this.trsSpecificExpectedQuery = "MATCH (`thing`)-[:`owner`]->(`thing_owner`) WHERE id(`thing_owner`) = {0} AND `thing`:`"+THING_NAME+"` RETURN `thing`";
        super.testQueryWithEntityGraphId();
    }

    @Test
    public void testQueryWithGraphId() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findById",
                new Object[]{123},
                getExpectedQuery("MATCH (`thing`) WHERE id(`thing`) = {0}"),
                getExpectedParams(123));
    }

    @Test
    @Override
    public void testIndexQueryWithTwoParams() throws Exception {
        // "findByFirstNameAndLastName",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`({0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "firstName:foo AND lastName:bar" };
        super.testIndexQueryWithTwoParams();
    }

    @Test
    @Override
    public void testIndexQueryWithOneParam() throws Exception {
        // "findByFirstName",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`(`firstName`={0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "foo" };
        super.testIndexQueryWithOneParam();
    }

    @Test
    @Override
    public void testIndexQueryWithOneParamFullText() throws Exception {
        // "findByDescription",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`search`({0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "description:foo" };
        super.testIndexQueryWithOneParamFullText();
    }

    @Test
    @Override
    public void testIndexQueryWithOneParamFullTextAndOneParam() throws Exception {
        // "findByDescriptionAndFirstName",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`search`({0}) WHERE `thing`.`firstName` = {1} RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "description:foo","bar" };
        super.testIndexQueryWithOneParamFullTextAndOneParam();
    }

    @Test
    @Override
    public void testIndexQueryWithOneParamAndOneParamFullText() throws Exception {
        // "findByFirstNameAndDescription",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`(`firstName`={0}) WHERE `thing`.`description` = {1} RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "foo","bar" };
        super.testIndexQueryWithOneParamAndOneParamFullText();
    }

    @Test
    @Override
    public void testIndexQueryWithOneNonIndexedParam() throws Exception {
        // "findByAge",
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`age` = {0} RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { 100 };
        super.testIndexQueryWithOneNonIndexedParam();
    }

    @Test
    @Override
    public void testIndexQueryWithOneNonIndexedParamAndOneIndexedParam() throws Exception {
        // "findByAgeAndFirstName",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`(`firstName`={1}) WHERE `thing`.`age` = {0} RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { 100, "foo" };
        super.testIndexQueryWithOneNonIndexedParamAndOneIndexedParam();
    }

    @Test
    @Override
    public void testIndexQueryWithLikeIndexedParam() throws Exception {
        // "findByFirstNameLike",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`({0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "firstName:foo" };
        super.testIndexQueryWithLikeIndexedParam();
    }

    @Test
    @Override
    public void testIndexQueryWithLikeIndexedParamWithSpaces() throws Exception {
        // "findByFirstNameLike",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`({0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "firstName:\"foo bar\"" };
        super.testIndexQueryWithLikeIndexedParamWithSpaces();
    }

    @Test
    @Override
    public void testIndexQueryWithContainsIndexedParam() throws Exception {
        // "findByFirstNameContains",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`({0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "firstName:*foo*" };
        super.testIndexQueryWithContainsIndexedParam();
    }

    @Test
    @Override
    public void testIndexQueryWithStartsWithIndexedParam() throws Exception {
        // "findByFirstNameStartsWith",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`({0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "firstName:foo*" };
        super.testIndexQueryWithStartsWithIndexedParam();
    }

    @Test
    @Override
    public void testIndexQueryWithEndsWithIndexedParam() throws Exception {
        // "findByFirstNameEndsWith",
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`({0}) RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "firstName:*foo" };
        super.testIndexQueryWithEndsWithIndexedParam();
    }

    @Test(expected = RepositoryQueryException.class)
    @Override
    public void testFailIndexQueryWithStartsWithIndexedParamWithSpaces() throws Exception {
        // findByFirstNameStartsWith
        this.trsSpecificExpectedQuery =
                "START `thing`=node:`Thing`({0})" +
                        " RETURN `thing`";
        super.testFailIndexQueryWithStartsWithIndexedParamWithSpaces();
    }


    @Test
    @Override
    public void testFindBySimpleStringParam() throws Exception {
        // findByName
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` = {0}" +
                " RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "foo" };
        super.testFindBySimpleStringParam();
    }

    @Test
    @Override
    public void testFindBySimpleStringParamStartsWith() throws Exception {
        // findByNameStartsWith
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` =~ {0}" +
                " RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "^foo.*" };
        super.testFindBySimpleStringParamStartsWith();
    }

    @Test
    @Override
    public void testFindBySimpleStringParamEndsWith() throws Exception {
        // findByNameEndsWith
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` =~ {0}" +
                " RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { ".*foo$" };
        super.testFindBySimpleStringParamEndsWith();
    }



    @Test
    @Override
    public void testFindBySimpleStringParamContains() throws Exception {
        // findByNameContains
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` =~ {0}" +
                " RETURN `thing`";
        super.testFindBySimpleStringParamContains();
    }

    @Test
    @Override
    public void testFindBySimpleStringParamLike() throws Exception {
        // findByNameLike
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` =~ {0}" +
                " RETURN `thing`";
        super.testFindBySimpleStringParamLike();
    }

    @Test
    @Override
    public void testFindBySimpleStringParamNotLike() throws Exception {
        // findByNameNotLike
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE not( `thing`.`name` =~ {0} )" +
                " RETURN `thing`";
        super.testFindBySimpleStringParamNotLike();
    }


    @Test
    @Override
    public void testFindBySimpleStringParamRegexp() throws Exception {
        // findByNameMatches
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` =~ {0}" +
                " RETURN `thing`";
        super.testFindBySimpleStringParamRegexp();
    }

    @Test
    @Override
    public void testFindBySimpleBooleanIsTrue() throws Exception {
        // findByTaggedIsTrue
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`tagged` = true" +
                "  RETURN `thing`";
        super.testFindBySimpleBooleanIsTrue();
    }

    @Test
    @Override
    public void testFindBySimpleBooleanIsFalse() throws Exception {
        // findByTaggedIsFalse
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`tagged` = false" +
                "  RETURN `thing`";
        super.testFindBySimpleBooleanIsFalse();
    }

    @Test
    @Override
    public void testFindBySimpleStringExists() throws Exception {
        // findByNameExists
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE has(`thing`.`name`  )" +
                " RETURN `thing`";
        super.testFindBySimpleStringExists();
    }

    @Test
    @Override
    public void testFindBySimpleStringInCollection() throws Exception {
        // findByNameIn
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` in {0}" +
                " RETURN `thing`";
        super.testFindBySimpleStringInCollection();
    }

    @Test
    @Override
    public void testFindBySimpleStringInCollectionOfEnums() throws Exception {
        // findByNameIn
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`name` in {0}" +
                " RETURN `thing`";
        super.testFindBySimpleStringInCollectionOfEnums();
    }

    @Test
    @Override
    public void testFindBySimpleStringNotInCollection() throws Exception {
        // findByNameNotIn
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE not( `thing`.`name` in {0} )" +
                " RETURN `thing`";
        super.testFindBySimpleStringNotInCollection();
    }

    @Test
    @Override
    public void testFindBySimpleDateBefore() throws Exception {
        // findByBornBefore
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`born` < {0} RETURN `thing`";
        super.testFindBySimpleDateBefore();
    }

    @Test
    @Override
    public void testFindBySimpleDateAfter() throws Exception {
        // findByBornAfter
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`born` > {0} RETURN `thing`";
        super.testFindBySimpleDateAfter();
    }

    @Test
    @Override
    public void testFindByNumericIndexedField() throws Exception {
        // findByNumber
        this.trsSpecificExpectedQuery = "START `thing`=node:`Thing`(`number`={0}) RETURN `thing`";
        super.testFindByNumericIndexedField();
    }

    @Test
    @Override
    public void testSchemaIndexQueryWithOneParam() throws Exception {
        // findByAlias
        this.trsSpecificExpectedQuery = DEFAULT_MATCH_CLAUSE +
                " WHERE `thing`.`alias` = {0}" +
                " RETURN `thing`";
        this.trsSpecificExpectedParams = new Object[] { "foo" };
        super.testSchemaIndexQueryWithOneParam();
    }
}
