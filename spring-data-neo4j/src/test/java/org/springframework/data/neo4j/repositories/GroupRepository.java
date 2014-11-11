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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;


/**
 * @author mh
 * @since 29.03.11
 */
public interface GroupRepository extends GraphRepository<Group>, NamedIndexRepository<Group> {
    Iterable<Group> findByFullTextNameLike(String name);
    Page<Group> findByName(String name, Pageable page);

    @Query("match (group:g{name:{0}})-[:persons]->(aMember) return aMember")
    Set<Person> getTeamMembersAsSetViaQuery(String groupName);

    @Query("match (group:g)-[:persons]->(aMember) where group.name IN {groups} return aMember")
    Set<Person> getTeamMembersByGroupNames(@Param("groups") List<String> groups);
}
