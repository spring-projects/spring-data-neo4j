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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class DerivedFinderMethodTest {

    @NodeEntity
    public static class Thing {
        @GraphId
        Long id;
        @Indexed
        String firstName;
        @Indexed
        String lastName;

        String name;
        boolean tagged;

        @GraphProperty(propertyType = Long.class) Date born;

        @Indexed(indexType = IndexType.FULLTEXT, indexName = "search") String description;

        int age;

        public Thing() {
        }

        public Thing(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    @Autowired
    ThingRepository repository;
    @Autowired
    Neo4jMappingContext ctx;

    @Autowired
    Neo4jTemplate template;

    @Test
    public void testCreateIndexQuery() throws Exception {
        CypherQueryBuilder builder = new CypherQueryBuilder(ctx, Thing.class, template);
        builder.addRestriction(new Part("firstName",Thing.class));
        builder.addRestriction(new Part("lastName",Thing.class));
        CypherQueryDefinition query = builder.buildQuery();
        assertEquals("START `thing`=node:`Thing`({0}) RETURN `thing`", query.toQueryString());
    }

    @Test
    public void testIndexQueryWithTwoParams() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameAndLastName",new Object[]{"foo", "bar"},
                "START `thing`=node:`Thing`({0})",
                "firstName:foo AND lastName:bar");
    }

    @Test
    public void testIndexQueryWithOneParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstName",new Object[]{"foo"},
                "START `thing`=node:`Thing`(`firstName`={0})",
                "foo");
    }

    @Test
    public void testIndexQueryWithOneParamFullText() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByDescription", new Object[]{"foo"},
                "START `thing`=node:`search`({0})",
                "description:foo");
    }

    @Test
    public void testIndexQueryWithOneParamFullTextAndOneParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByDescriptionAndFirstName", new Object[]{"foo","bar"},
                "START `thing`=node:`search`({0}) WHERE `thing`.`firstName`! = {1}",
                "description:foo","bar");
    }

    @Test
    public void testIndexQueryWithOneParamAndOneParamFullText() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameAndDescription", new Object[]{"foo","bar"},
                "START `thing`=node:`Thing`(`firstName`={0}) WHERE `thing`.`description`! = {1}",
                "foo","bar");
    }

    @Test
    public void testIndexQueryWithOneNonIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByAge", new Object[]{100},
                "WHERE `thing`.`age`! = {0}",
                100);
    }

    @Test
    public void testIndexQueryWithOneNonIndexedParamAndOneIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByAgeAndFirstName", new Object[]{100,"foo"},
                "START `thing`=node:`Thing`(`firstName`={1}) WHERE `thing`.`age`! = {0}",
                100,"foo");
    }

    @Test
    public void testIndexQueryWithLikeIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameLike", new Object[]{"foo"},
                "START `thing`=node:`Thing`({0})",
                "firstName:foo");
    }

    @Test
    public void testIndexQueryWithLikeIndexedParamWithSpaces() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameLike", new Object[]{"foo bar"},
                "START `thing`=node:`Thing`({0})",
                "firstName:\"foo bar\"");
    }

    @Test
    public void testIndexQueryWithContainsIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameContains", new Object[]{"foo"},
                "START `thing`=node:`Thing`({0})",
                "firstName:*foo*");
    }
    @Test
    public void testIndexQueryWithStartsWithIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameStartsWith", new Object[]{"foo"},
                "START `thing`=node:`Thing`({0})",
                "firstName:foo*");
    }

    @Test
    public void testIndexQueryWithEndsWithIndexedParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameEndsWith", new Object[]{"foo"},
                "START `thing`=node:`Thing`({0})",
                "firstName:*foo");
    }

    @Test(expected = RepositoryQueryException.class)
    public void testFailIndexQueryWithStartsWithIndexedParamWithSpaces() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByFirstNameStartsWith", new Object[]{"foo bar"},
                "START `thing`=node:`Thing`({0})");
    }

    @Test
    public void testFindBySimpleStringParam() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByName", new Object[]{"foo"},
                "WHERE `thing`.`name`! = {0}",
                "foo");
    }

    @Test
    public void testFindBySimpleStringParamStartsWith() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameStartsWith", new Object[]{"foo"},
                "WHERE `thing`.`name`! =~ {0}",
                "^foo.*");
    }
    @Test
    public void testFindBySimpleStringParamEndsWith() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameEndsWith", new Object[]{"foo"},
                "WHERE `thing`.`name`! =~ {0}",
                ".*foo$");
    }

    @Test
    public void testFindBySimpleStringParamContains() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameContains", new Object[]{"foo"},
                "WHERE `thing`.`name`! =~ {0}",
                ".*foo.*");
    }
    @Test
    public void testFindBySimpleStringParamLike() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameLike", new Object[]{"foo"},
                "WHERE `thing`.`name`! =~ {0}",
                "foo");
    }
    @Test
    public void testFindBySimpleStringParamNotLike() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameNotLike", new Object[]{"foo"},
                "WHERE not( `thing`.`name`! =~ {0} )",
                "foo");
    }

    @Test
    public void testFindBySimpleStringParamRegexp() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameMatches", new Object[]{"foo"},
                "WHERE `thing`.`name`! =~ {0}",
                "foo");
    }
    @Test
    public void testFindBySimpleBooleanIsTrue() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByTaggedIsTrue", new Object[]{},
                "WHERE `thing`.`tagged`! = true");
    }
    @Test
    public void testFindBySimpleBooleanIsFalse() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByTaggedIsFalse", new Object[]{},
                "WHERE `thing`.`tagged`! = false");
    }
    @Test
    public void testFindBySimpleStringExists() throws Exception {
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameExists", new Object[]{},
                "WHERE has(`thing`.`name`!  )");
    }

    @Test
    public void testFindBySimpleStringInCollection() throws Exception {
        List<String> param = asList("foo");
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameIn", new Object[]{param},
                "WHERE `thing`.`name`! in {0}",
                param);
    }
    @Test
    public void testFindBySimpleStringInCollectionOfEnums() throws Exception {
        List<TimeUnit> param = asList(TimeUnit.MINUTES);
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameIn", new Object[]{param},
                "WHERE `thing`.`name`! in {0}",
                param);
    }

    @Test
    public void testFindBySimpleStringNotInCollection() throws Exception {
        List<String> param = asList("foo");
        assertRepositoryQueryMethod(ThingRepository.class, "findByNameNotIn", new Object[]{param},
                "WHERE not( `thing`.`name`! in {0} )",
                param);
    }
    @Test
    public void testFindBySimpleDateBefore() throws Exception {
        Date param = new Date(1337);
        assertRepositoryQueryMethod(ThingRepository.class, "findByBornBefore", new Object[]{param},
                "WHERE `thing`.`born`! < {0}",
                param.getTime());
    }

    @Test
    public void testFindBySimpleDateAfter() throws Exception {
        Date param = new Date(1337);
        assertRepositoryQueryMethod(ThingRepository.class, "findByBornAfter", new Object[]{param},
                "WHERE `thing`.`born`! > {0}",
                param.getTime());
    }

    private void assertRepositoryQueryMethod(Class<ThingRepository> repositoryClass, String methodName, Object[] paramValues, String expectedQuery, Object...expectedParam) {
        Method method = methodFor(repositoryClass, methodName);
        DerivedCypherRepositoryQuery derivedCypherRepositoryQuery = new DerivedCypherRepositoryQuery(ctx, new GraphQueryMethod(method, new DefaultRepositoryMetadata(repositoryClass), null, ctx), template);
        Parameters parameters = new Parameters(method);
        ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, paramValues);
        String query = derivedCypherRepositoryQuery.createQueryWithPagingAndSorting(accessor);
        Map<String, Object> params = derivedCypherRepositoryQuery.resolveParams(accessor);
        String firstWord = expectedQuery.split("\\s+")[0];
        assertEquals(expectedQuery,query.substring(query.indexOf(firstWord)).substring(0,expectedQuery.length()));
        assertEquals(expectedParam.length,params.size());
        for (int i = 0; i < expectedParam.length; i++) {
            assertEquals(expectedParam[i],params.get(String.valueOf(i)));
        }
    }

    private Method methodFor(Class<? extends org.springframework.data.repository.Repository> repositoryClass, String methodName) {
        for (Method method : repositoryClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) return method;
        }
        throw new NoSuchMethodError("Method "+methodName+" not found in "+repositoryClass);
    }

    @Test
    @Transactional
    public void testMultipleIndexedFields() throws Exception {
        Thing thing = repository.save(new Thing("John", "Doe"));
        assertEquals(thing.id, repository.findByFirstNameAndLastName("John", "Doe").id);
    }
}
