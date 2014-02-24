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
package org.springframework.data.neo4j.conversion;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.mapping.Neo4jPersistentTestBase;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.conversion.NoSuchColumnFoundException;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.helpers.collection.MapUtil.map;


public class QueryMapResultConverterTests extends Neo4jPersistentTestBase {

    private Map<String, Object> simpleMap;
    private List<String> friends;
    private Map<String, Object> advancedMap;

    public interface PersonAndFriendsData {
        @ResultColumn( "person" )
        Person getPerson();

        @ResultColumn( "collect(r)" )
        Iterable<Friendship> getFriends();
    }

    public interface SimplestQuery {
        @ResultColumn( "name" )
        String getName();

        @ResultColumn( "age" )
        Integer getAge();

        @ResultColumn( "friends" )
        Iterable<String> getFriendNames();
    }

    @Override
    protected void setBasePackage(Neo4jMappingContext mappingContext) throws ClassNotFoundException {
        super.setBasePackage(mappingContext,Person.class.getPackage().getName());
    }

    @Before
    public void init() throws Exception {
        storeInGraph( michael );
        storeInGraph( andres );
        storeInGraph( emil );

        makeFriends( michaelNode(), andresNode(), 19 );
        makeFriends( michaelNode(), emilNode(), 6 );

        friends = asList( "Michael", "Emil", "Anders" );
        simpleMap = map( "name", "Andres", "age", 36L, "friends", friends );
        advancedMap = map( "person", michaelNode(), "collect(r)", michaelNode().getRelationships( KNOWS ) );

        michael = readPerson( michaelNode() );
    }

    @Test
    public void shouldBeAbleToGetAStringFromAResultMap() throws Exception {
        QueryMapResultConverter<SimplestQuery> converter = getConverter();
        SimplestQuery query = converter.convert( simpleMap, SimplestQuery.class );

        assertThat( query.getName(), equalTo( "Andres" ) );
        assertThat( query.getAge(), equalTo( 36 ) );
    }

    @Test
    public void shouldBeAbleToHandleAnIterableOfString() throws Exception {
        QueryMapResultConverter<SimplestQuery> converter = getConverter();
        SimplestQuery query = converter.convert( simpleMap, SimplestQuery.class );

        assertThat( query.getFriendNames(), hasItems( "Michael", "Emil", "Anders" ) );
    }

    @Test
    public void shouldHandleANodeBackedEntity() throws Exception {
        QueryMapResultConverter<PersonAndFriendsData> converter = new QueryMapResultConverter<PersonAndFriendsData>(
                template );
        PersonAndFriendsData result = converter.convert( advancedMap, PersonAndFriendsData.class );

        assertThat( result.getPerson(), equalTo( michael ) );
        assertThat( asCollection( result.getFriends() ), equalTo( asCollection( michael.getFriendships() ) ) );
    }

    @Test( expected = NoSuchColumnFoundException.class )
    public void shouldThrowNiceException() throws Exception {
        QueryMapResultConverter<PersonAndFriendsData> converter = new QueryMapResultConverter<PersonAndFriendsData>(
                template );
        PersonAndFriendsData convert = converter.convert( map(), PersonAndFriendsData.class );
         convert.getFriends();
    }

    @Test
    public void testShouldBeAbleToCompareTwoResults() throws Exception {
        QueryMapResultConverter<SimplestQuery> converter = getConverter();

        final SimplestQuery query1 = converter.convert(simpleMap, SimplestQuery.class);
        final SimplestQuery query2 = converter.convert(simpleMap, SimplestQuery.class);
        final SimplestQuery query1Clone = converter.convert(new HashMap<String, Object>(simpleMap), SimplestQuery.class);

        final HashMap<String, Object> copy = new HashMap<String, Object>(simpleMap);
        copy.put("name", "Michael");
        final SimplestQuery otherQuery = converter.convert(copy, SimplestQuery.class);

        assertEquals(query1.hashCode(),query2.hashCode());
        assertEquals(query1,query2);
        assertEquals(query1, query1Clone);
        assertEquals(query1.hashCode(), query1Clone.hashCode());
        assertEquals(false, query1.equals(otherQuery));
        assertEquals(false, query1.hashCode() == otherQuery.hashCode());
    }

    @Test
    public void testPutQueryResultsInSet() throws Exception {
        QueryMapResultConverter<SimplestQuery> converter = getConverter();

        final SimplestQuery query1 = converter.convert(simpleMap, SimplestQuery.class);
        final SimplestQuery query2 = converter.convert(simpleMap, SimplestQuery.class);
        Set<SimplestQuery> set=new HashSet<SimplestQuery>();
        set.add(query1);

        assertEquals(true,set.contains(query1));
        assertEquals(true,set.contains(query2));

    }

    private QueryMapResultConverter<SimplestQuery> getConverter() {
        return new QueryMapResultConverter<SimplestQuery>( template );
    }
}
