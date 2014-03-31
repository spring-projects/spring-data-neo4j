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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Polygon;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.Personality;
import org.springframework.data.neo4j.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Map;

/**
 * Sample repository interface to manage {@link Person}s.
 *
 * @author Michael Hunger
 * @author Oliver Gierke
 * @since 29.03.11
 */
public interface PersonRepository extends GraphRepository<Person>, NamedIndexRepository<Person>, SpatialRepository<Person>, PersonRepositoryFriendship, CypherDslRepository<Person>, RelationshipOperationsRepository<Person> {

    @Query("match (team:g)-[:persons]->(member) where id(team) = {p_team} return member")
    Iterable<Person> findAllTeamMembers(@Param("p_team") Group team);

    @Query("match (team:g)-[:persons]->(member) where id(team) = {p_team} return team.name as name,collect(member) as members")
    TeamResult findAllTeamMembersAsGroup(@Param("p_team") Group team);

    @Query("match (team:g)-[:persons]->(member) where id(team) = {p_team} return member.name,member.age")
    Iterable<Map<String, Object>> findAllTeamMemberData(@Param("p_team") Group team);

    @Query("match team-[:persons]->member<-[:boss]-boss where id(member) = {p_person} return collect(team), boss")
    Iterable<MemberData> findMemberData(@Param("p_person") Person person);

    @Query("match team-[:persons]->member<-[:boss]-boss where id(member) = {p_person} return collect(team), boss, boss.name as someonesName, boss.age as someonesAge ")
    MemberDataPOJO findMemberDataPojo(@Param("p_person") Person person);

    @Query("match team-[:persons]->member<-[:boss]-boss where id(member) = {p_person} return member")
    Iterable<MemberData> nonWorkingQuery(@Param("p_person") Person person);

    @Query("MATCH (team:g {name : {p_team}})-[:persons]->(member) return member order by member.name skip {skip} limit {limit}")
    Iterable<Person> findSomeTeamMembers(@Param("p_team") String team, @Param("skip") Integer skip, @Param("limit") Integer limit);

    @Query("match (boss)-[:boss]->(person) where id(person) = {p_person} return boss")
    Person findBoss(@Param("p_person") Person person);

    Collection<Person> findByWktNearAndName(Circle circle, String name);
    Collection<Person> findByWktWithinAndAgeGreaterThan(Circle circle, int age);
    Collection<Person> findByWktWithinAndPersonality(Polygon polygon, Personality personality);
    Collection<Person> findByWktWithin(Box box);

    @Query("match (boss)-[:boss]->(person) where id(person) = {p_person}  return boss")
    Person findBoss(@Param("p_person") Long person);

    @Query("match (boss)-[:boss]->(person) where id(boss) = {0} with person, count(*) as cnt order by cnt return person")
    Page<Person> findSubordinates(Person boss,Pageable page);

    @Query(value = "match (boss)-[:boss]->(person) where id(boss) = {0} with person, count(*) as cnt order by cnt return person",countQuery = "start boss=node({0}) match (boss)-[:boss]->(person) with person return count(*)")
    Page<Person> findSubordinatesWithCount(Person boss,Pageable page);

    Group findTeam(@Param("p_person") Person person);

    @Query("match (team)-[:persons]->(member) where id(team) = {p_team} return member")
    Page<Person> findAllTeamMembersPaged(@Param("p_team") Group team, Pageable page);

    @Query("match (team)-[:persons]->(member) where id(team) = {p_team} return member")
    Slice<Person> findAllTeamMembersSliced(@Param("p_team") Group team, Pageable page);

    @Query("match (team)-[:persons]->(member) where id(team) = {p_team} return member")
    Iterable<Person> findAllTeamMembersSorted(@Param("p_team") Group team, Sort sort);


    Long countByName(String name);

    // Derived queries
    Iterable<Person> findByName(String name);

    Iterable<Person> findByPersonality(String personality);

    Iterable<Person> findByAge(int age);

    @Query("start person=node:`name-index`('name:*') return person.name as name, person order by name asc ")
    Iterable<NameAndPersonResult> getAllNamesAndPeople();

    Result<Person> findByHeight(short height);

    @QueryResult
    interface NameAndPersonResult
    {
        @ResultColumn("name")
        String getName();

        @ResultColumn("person")
        Person getPerson();
    }
    @QueryResult
    interface TeamResult
    {
        String getName();

        @ResultColumn("members")
        Collection<Person> getMembers();
    }
}

