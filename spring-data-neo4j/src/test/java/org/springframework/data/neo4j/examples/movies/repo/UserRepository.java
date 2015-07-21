/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.examples.movies.repo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.*;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
@Repository
public interface UserRepository extends GraphRepository<User> {

    Collection<User> findByName(String name);

    Collection<User> findByMiddleName(String middleName);

    List<User> findByRatingsStars(int stars);

    List<User> findByRatingsStarsAndInterestedName(int stars, String name);

    @Query("MATCH (user:User) RETURN COUNT(user)")
    int findTotalUsers();

    @Query("MATCH (user:User) RETURN user.id")
    List<Integer> getUserIds();

    @Query("MATCH (user:User) RETURN user.name, user.id")
    Iterable<Map<String,Object>> getUsersAsProperties();

    @Query("MATCH (user:User) RETURN user")
    Collection<User> getAllUsers();

    @Query("MATCH (m:Movie)<-[:ACTED_IN]-(a:User) RETURN m.name as movie, collect(a.name) as cast")
    List<Map<String, Object>> getGraph();

    @Query("MATCH (user:User{name:{name}}) RETURN user")
    User findUserByNameWithNamedParam(@Param("name") String name);

    @Query("MATCH (user:User{name:{0}}) RETURN user")
    User findUserByName(String name);

    @Query("MATCH (user:User) RETURN id(user) AS userId, user.name AS userName, user.age ORDER BY user.age")
    Iterable<UserQueryResult> retrieveAllUsersAndTheirAges();

    @Query("MATCH (user:User{name:{0}}) RETURN user.name AS name")
    UnmanagedUserPojo findIndividualUserAsDifferentObject(String name);

    @Query("MATCH (user:User) WHERE user.name={0} RETURN user.name, user.age AS ageOfUser")
    UserQueryResultInterface findIndividualUserAsProxiedObject(String name);

    @Query("MATCH (user:User) WHERE user.gender={0} RETURN user.name AS UserName, user.gender AS UserGender, user.account as UserAccount, user.deposits as UserDeposits")
    Iterable<RichUserQueryResult> findUsersByGender(Gender gender);

    @Query("MATCH (user:User) WHERE user.name={0} RETURN user")
    EntityWrappingQueryResult findWrappedUserByName(String userName);

    @Query("MATCH (user:User) RETURN ID(user)")
    List<Long> getUserNodeIds();

}
