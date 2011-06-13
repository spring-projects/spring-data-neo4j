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

package org.springframework.data.graph.neo4j;

import org.springframework.data.graph.annotation.GraphQuery;
import org.springframework.data.graph.neo4j.repository.NamedIndexRepository;
import org.springframework.data.graph.neo4j.repository.GraphRepository;

import java.util.Collection;
import java.util.Map;

/**
 * @author mh
 * @since 29.03.11
 */
public interface PersonRepository extends GraphRepository<Person>, NamedIndexRepository<Person> {

    @GraphQuery("start team=(%d) match (team)-[:persons]->(member) return member")
    Iterable<Person> findAllTeamMembers(Group team);

    @GraphQuery("start team=(%d) match (team)-[:persons]->(member) return member.name,member.age")
    Iterable<Map<String,Object>> findAllTeamMemberData(Group team);

    @GraphQuery("start person=(%d) match (boss)-[:boss]->(person) return boss")
    Person findBoss(Person person);

}
