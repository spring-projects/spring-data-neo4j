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

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.neo4j.helpers.collection.MapUtil.map;


public class QueryMapResultConverterTest extends Neo4jPersistentTestBase {

    private Map<String, Object> simpleMap;
    private List<String> friends;
    private Map<String, Object> advancedMap;

    public interface PersonAndFriendsData {
        @ResultColumn("person")
        Person getPerson();

        @ResultColumn("collect(r)")
        Iterable<Friendship> getFriends();
    }

    public interface SimplestQuery {
        @ResultColumn("name")
        String getName();

        @ResultColumn("age")
        Integer getAge();

        @ResultColumn("friends")
        Iterable<String> getFriendNames();
    }

    @Before
    public void init() throws Exception {
        storeInGraph(michael);
        storeInGraph(andres);
        storeInGraph(emil);

        friends = asList("Michael", "Emil", "Anders");
        simpleMap = map("name", "Andres", "age", 36L, "friends", friends);
        advancedMap = map("person", michaelNode(), "collect(r)", michaelNode().getRelationships(KNOWS));

        makeFriends(michaelNode(), andresNode(), 19);
        makeFriends(michaelNode(), emilNode(), 6);
    }

    @Test
    public void shouldBeAbleToGetAStringFromAResultMap() throws Exception {
        QueryMapResulConverter<SimplestQuery> converter = getConverter();
        SimplestQuery query = converter.convert(simpleMap, SimplestQuery.class);

        assertThat(query.getName(), equalTo("Andres"));
        assertThat(query.getAge(), equalTo(36));
    }

    @Test
    public void shouldBeAbleToHandleAnIterableOfString() throws Exception {
        QueryMapResulConverter<SimplestQuery> converter = getConverter();
        SimplestQuery query = converter.convert(simpleMap, SimplestQuery.class);

        assertThat(query.getFriendNames(), hasItems("Michael", "Emil", "Anders"));
    }

    @Test
    public void shouldHandleANodeBackedEntity() throws Exception {
        QueryMapResulConverter<PersonAndFriendsData> converter = new QueryMapResulConverter<PersonAndFriendsData>(template);
        PersonAndFriendsData result = converter.convert(advancedMap, PersonAndFriendsData.class);

        assertThat(result.getPerson(), equalTo(michael));
        assertThat(result.getFriends(), equalTo(michael.getFriendships()));
    }

    private QueryMapResulConverter<SimplestQuery> getConverter() {
        return new QueryMapResulConverter<SimplestQuery>(template);
    }
}