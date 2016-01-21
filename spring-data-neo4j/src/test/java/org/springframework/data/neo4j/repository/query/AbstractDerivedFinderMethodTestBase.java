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
import org.neo4j.index.lucene.ValueContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Defines the tests for the various finder method based scenarios with
 * expected results irrespective of which Type Representation Strategy (TRS)
 * is being employed. Subclasses testing against specific TRS
 * can overwrite expectations where they differ from the common approach.
 *
 * @author Oliver Gierke & Nicki Watt
 */
public abstract class AbstractDerivedFinderMethodTestBase {

    @NodeEntity
    public static class Thing {
        @GraphId
        Long id;
        @Indexed(indexType = IndexType.SIMPLE)
        String firstName;
    	@Indexed(numeric = true,indexType = IndexType.SIMPLE)
    	int number;
        @Indexed(indexType = IndexType.SIMPLE)
        String lastName;

        @Indexed(indexType = IndexType.LABEL)
        String alias;

        String name;
        boolean tagged;

        @GraphProperty(propertyType = Long.class) Date born;

        @Indexed(indexType = IndexType.FULLTEXT, indexName = "search") String description;

        Person owner;

        int age;

        public Thing() {
        }

        public Thing(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    protected final static String THING_NAME = Thing.class.getSimpleName();
    @Autowired
    ThingRepository repository;
    @Autowired
    Neo4jMappingContext ctx;

    @Autowired
    Neo4jTemplate template;

    @Before
    public void setup() {
        this.trsSpecificExpectedQuery = null;
        this.trsSpecificExpectedParams = null;
    }

    // Allow subclasses to provide specific expectations
    protected String   trsSpecificExpectedQuery;
    protected Object[] trsSpecificExpectedParams;

    @Test
    public void testCypherQueryBuilderWithTwoIndexedParams() throws Exception {
        CypherQueryBuilder builder = new CypherQueryBuilder(ctx, Thing.class, template);
        builder.addRestriction(new Part("firstName",Thing.class));
        builder.addRestriction(new Part("lastName",Thing.class));
        CypherQueryDefinition query = builder.buildQuery();
        assertEquals(
                getExpectedQuery("START `thing`=node:`Thing`({0}) RETURN `thing`"),
                query.toQueryString());

    }

    @Test
    public void testQueryWithGraphId() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findById",
                new Object[]{123},
                getExpectedQuery("START `thing`=node({0})"),
                getExpectedParams(123));
    }

    @Test
    public void testQueryWithEntityGraphId() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByOwnerId",
                new Object[]{123},
                getExpectedQuery("this-should-def-be-overwritten-to-supply-trs-specific-query"),
                getExpectedParams(123));
    }

    @Test
    public void testIndexQueryWithTwoParams() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameAndLastName",
                new Object[]{"foo", "bar"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("firstName:foo AND lastName:bar"));
    }

    @Test
    public void testIndexQueryWithOneParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstName",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("foo"));
    }

    @Test
    public void testSchemaIndexQueryWithOneParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByAlias",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("foo"));
    }

    @Test
    public void testIndexQueryWithOneParamFullText() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByDescription",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("description:foo"));
    }

    @Test
    public void testIndexQueryWithOneParamFullTextAndOneParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByDescriptionAndFirstName",
                new Object[]{"foo","bar"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("description:foo","bar"));
    }

    @Test
    public void testIndexQueryWithOneParamAndOneParamFullText() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameAndDescription",
                new Object[]{"foo","bar"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("foo","bar"));
    }

    @Test
    public void testIndexQueryWithOneNonIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByAge", new Object[]{100},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(100));
    }

    @Test
    public void testIndexQueryWithOneNonIndexedParamAndOneIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByAgeAndFirstName",
                new Object[]{100, "foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(100, "foo"));
    }

    @Test
    public void testIndexQueryWithLikeIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameLike",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("firstName:foo"));
    }

    @Test
    public void testIndexQueryWithLikeIndexedParamWithSpaces() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameLike",
                new Object[]{"foo bar"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("firstName:\"foo bar\""));
    }

    @Test
    public void testIndexQueryWithContainsIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameContains",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("firstName:*foo*"));
    }
    @Test
    public void testIndexQueryWithStartsWithIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameStartsWith",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("firstName:foo*"));
    }

    @Test
    public void testIndexQueryWithEndsWithIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameEndsWith",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("firstName:*foo"));
    }

    @Test(expected = RepositoryQueryException.class)
    public void testFailIndexQueryWithStartsWithIndexedParamWithSpaces() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByFirstNameStartsWith",
                new Object[]{"foo bar"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"));
    }

    @Test
    public void testFindBySimpleStringParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByName",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("foo"));
    }

    @Test
    public void testFindBySimpleStringParamStartsWith() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameStartsWith",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("^foo.*"));
    }
    @Test
    public void testFindBySimpleStringParamEndsWith() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameEndsWith",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(".*foo$"));
    }

    @Test
    public void testFindBySimpleStringParamContains() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameContains",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(".*foo.*"));
    }
    @Test
    public void testFindBySimpleStringParamLike() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameLike",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("foo"));
    }
    @Test
    public void testFindBySimpleStringParamNotLike() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameNotLike",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("foo"));
    }

    @Test
    public void testFindBySimpleStringParamRegexp() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameMatches",
                new Object[]{"foo"},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams("foo"));
    }
    @Test
    public void testFindBySimpleBooleanIsTrue() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByTaggedIsTrue", new Object[]{},
                getExpectedQuery("subclass-to-supply-trs-specific-query"));
    }
    @Test
    public void testFindBySimpleBooleanIsFalse() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByTaggedIsFalse", new Object[]{},
                getExpectedQuery("subclass-to-supply-trs-specific-query"));
    }
    @Test
    public void testFindBySimpleStringExists() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameExists", new Object[]{},
                getExpectedQuery("subclass-to-supply-trs-specific-query"));
    }

    @Test
    public void testFindBySimpleStringInCollection() throws Exception {
        List<String> param = asList("foo");
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameIn", new Object[]{param},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(param));
    }

    @Test
    public void testFindBySimpleStringInCollectionOfEnums() throws Exception {
        List<TimeUnit> param = asList(TimeUnit.MINUTES);
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameIn", new Object[]{param},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(param));
    }

    @Test
    public void testFindBySimpleStringNotInCollection() throws Exception {
        List<String> param = asList("foo");
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNameNotIn", new Object[]{param},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(param));
    }

    @Test
    public void testFindBySimpleDateBefore() throws Exception {
        Date param = new Date(1337);
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByBornBefore", new Object[]{param},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(param.getTime()));
    }

    @Test
    public void testFindBySimpleDateAfter() throws Exception {
        Date param = new Date(1337);
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByBornAfter", new Object[]{param},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(param.getTime()));
    }

    @Test
    public void testFindByNumericIndexedField() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class,
                "findByNumber", new Object[]{10},
                getExpectedQuery("subclass-to-supply-trs-specific-query"),
                getExpectedParams(ValueContext.numeric(10)));
    }

    @Test
    @Transactional
    public void testMultipleIndexedFields() throws Exception {
        Thing thing = repository.save(new Thing("John", "Doe"));
        assertEquals(thing.id, repository.findByFirstNameAndLastName("John", "Doe").id);
    }

    /**
     * This Abstract class defines the template for how to test, however
     * gives subclasses an opportunity to override the expected query
     * if it is different / specific for the TRS being used.
     * This method will either return the trs specific query string if
     * this was set, otherwise the default value passed in.
     */
    protected String getExpectedQuery(String defaultQueryString) {
        return (this.trsSpecificExpectedQuery != null)
                ? this.trsSpecificExpectedQuery
                : defaultQueryString;
    }

    /**
     * This Abstract class defines the template for how to test, however
     * gives subclasses an opportunity to override the expected params
     * if it is different / specific for the TRS being used.
     * This method will either return the trs specific query params if
     * this was set, otherwise the default value passed in.
     */
    protected Object[] getExpectedParams(Object... defaultVals) {
        return (this.trsSpecificExpectedParams != null)
                ? this.trsSpecificExpectedParams
                : (defaultVals == null) ? new Object[0] : defaultVals;
    }

    protected void assertRepositoryQueryMethod(Class<ThingRepository> repositoryClass, String methodName, Object[] paramValues, String expectedQuery, Object...expectedParam) {
        Method method = methodFor(repositoryClass, methodName);
        DerivedCypherRepositoryQuery derivedCypherRepositoryQuery = new DerivedCypherRepositoryQuery(ctx, new GraphQueryMethod(method, new DefaultRepositoryMetadata(repositoryClass), null, ctx), template);
        Parameters<?, ?> parameters = new DefaultParameters(method);
        ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, paramValues);
        String query = derivedCypherRepositoryQuery.createQueryWithPagingAndSorting(accessor);
        Map<String, Object> params = derivedCypherRepositoryQuery.resolveParams(accessor);
        String firstWord = expectedQuery.split("\\s+")[0];
        int beginIndex = query.indexOf(firstWord);
        assertTrue("didn't find word "+firstWord+" in "+query,beginIndex != -1);
        String actual = query.substring(beginIndex);
        actual = actual.substring(0, Math.min(expectedQuery.length(),actual.length()));
        assertEquals(expectedQuery, actual);
        assertEquals(expectedParam.length,params.size());
        for (int i = 0; i < expectedParam.length; i++) {
            if (expectedParam[i] instanceof ValueContext) {
                assertEquals(((ValueContext)expectedParam[i]).getValue(),((ValueContext)params.get(String.valueOf(i))).getValue());
            } else {
                assertEquals(expectedParam[i],params.get(String.valueOf(i)));
            }
        }
    }

    private Method methodFor(Class<? extends org.springframework.data.repository.Repository> repositoryClass, String methodName) {
        for (Method method : repositoryClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) return method;
        }
        throw new NoSuchMethodError("Method "+methodName+" not found in "+repositoryClass);
    }
}
