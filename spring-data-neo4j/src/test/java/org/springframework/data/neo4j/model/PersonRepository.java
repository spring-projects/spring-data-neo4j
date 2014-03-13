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

package org.springframework.data.neo4j.model;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author mh
 * @since 29.03.11
 */
public interface PersonRepository extends GraphRepository<Person>, NamedIndexRepository<Person> {

    @Query("start team=node({p_team}) match (team)-[:persons]->(member) return member")
    Iterable<Person> findAllTeamMembers(@Param("p_team") Group team);

    @Query("start team=node({p_team}) match (team)-[:persons]->(member) return member.name,member.age")
    Iterable<Map<String,Object>> findAllTeamMemberData(@Param("p_team") Group team);

    @Query("start person=node({p_person}) match (boss)-[:boss]->(person) return boss")
    Person findBoss(@Param("p_person") Person person);

    Group findTeam(@Param("p_person") Person person);

    @Query("start team=node({p_team}) match (team)-[:persons]->(member) return member")
    Page<Person> findAllTeamMembersPaged(@Param("p_team") Group team, Pageable page);
    @Query("start team=node({p_team}) match (team)-[:persons]->(member) return member")
    Iterable<Person> findAllTeamMembersSorted(@Param("p_team") Group team, Sort sort);
}
