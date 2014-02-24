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
package org.springframework.data.neo4j.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.template.Neo4jOperations;

/**
 * @author mh
 * @since 08.11.11
 */
public class PersonRepositoryImpl implements PersonRepositoryFriendship {
    @Autowired
    Neo4jOperations template;
    @Override
    public Friendship befriend(Person p1, Person p2) {
        return template.createRelationshipBetween(p1, p2, Friendship.class, "knows", false);
    }
}
