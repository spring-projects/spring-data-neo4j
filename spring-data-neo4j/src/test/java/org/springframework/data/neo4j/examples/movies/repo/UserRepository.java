/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.examples.movies.repo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.Gender;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.RichUserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.UserQueryResultObject;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Repository
public interface UserRepository extends PersonRepository<User, Long> {

	Collection<User> findByMiddleName(String middleName);

	List<User> findByRatingsStars(int stars);

	List<User> findByRatingsStarsAndInterestedName(int stars, String name);

	List<User> findByRatingsStarsAndInterestedNameIgnoreCase(int stars, String name);

	List<User> findByRatingsStarsAndInterestedNameAllIgnoreCase(int stars, String name);

	List<User> findByRatingsStarsIgnoreCase(int stars);

	@Query("MATCH (user:User) RETURN COUNT(user)")
	int findTotalUsers();

	@Query("MATCH (user:User) RETURN id(user)")
	List<Integer> getUserIds();

	@Query("MATCH (user:User) RETURN user.name, id(user)")
	Iterable<Map<String, Object>> getUsersAsProperties();

	@Query("MATCH (user:User) RETURN user")
	Collection<User> getAllUsers();

	@Query("MATCH (m:Movie)<-[:ACTED_IN]-(a:User) RETURN m.name as movie, collect(a.name) as cast")
	List<Map<String, Object>> getGraph();

	@Query("MATCH (user:User{name:{name}}) RETURN user")
	User findUserByNameWithNamedParam(@Param("name") String name);

	@Query("MATCH (user:User{name:{0}}) RETURN user")
	User findUserByName(String name);

	@Query("MATCH (user:User) RETURN id(user) AS userId, id(user) as id, user.name AS userName, user.age ORDER BY user.age")
	Iterable<UserQueryResult> retrieveAllUsersAndTheirAges();

	@Query("MATCH (user:User{name:{0}}) RETURN user.name AS name")
	UnmanagedUserPojo findIndividualUserAsDifferentObject(String name);

	@Query("MATCH (user:User) WHERE user.name={0} RETURN user.name AS name, user.age AS ageOfUser")
	UserQueryResultObject findIndividualUserAsProxiedObject(String name);

	@Query("MATCH (user:User) WHERE user.name={0} RETURN user as user, user.age AS ageOfUser")
	UserQueryResultObject findWrappedUserAsProxiedObject(String name);

	@Query("MATCH (user:User) WHERE user.gender={0} RETURN user.name AS UserName, user.gender AS UserGender, user.account as UserAccount, user.deposits as UserDeposits")
	Iterable<RichUserQueryResult> findUsersByGender(Gender gender);

	@Query("MATCH (user:User) WHERE user.name={0} RETURN user")
	EntityWrappingQueryResult findWrappedUserByName(String userName);

	@Query("MATCH (user:User)-[:FRIEND_OF]->(f) WHERE user.name={0} RETURN user, collect(f) as friends")
	EntityWrappingQueryResult findWrappedUserAndFriendsDepth0(String userName);

	@Query("MATCH (user:User)-[r:FRIEND_OF]->(f) WHERE user.name={0} RETURN user, collect(r) as rels, collect(f) as friends")
	EntityWrappingQueryResult findWrappedUserAndFriendsDepth1(String userName);

	@Query("MATCH (user:User)-[r:RATED]->(m) WHERE user.name={0} RETURN user, collect(r) as ratings, collect(m) as movies, avg(r.stars) as avgRating")
	EntityWrappingQueryResult findWrappedUserAndRatingsByName(String userName);

	@Query("MATCH (user:User)-[r:RATED]->(m) RETURN user, collect(r) as ratings, collect(m) as movies, avg(r.stars) as avgRating order by user.name desc")
	List<EntityWrappingQueryResult> findAllUserRatings();

	@Query("MATCH (user:User) RETURN ID(user)")
	List<Long> getUserNodeIds();

	@Query("MATCH (user:User) WHERE ID(user)={0} return user")
	User loadUserById(User user);

	@Query("MATCH (user:User) WHERE ID(user)={userId} RETURN user")
	User loadUserByNamedId(@Param("userId") User user);

	@Query("MATCH (user:User) RETURN user")
	Iterable<User> getAllUsersIterable();

	@Query("MATCH (user:User) set user.name={0}")
	void setNamesNull(String name);

	List<User> findByNameIsNotLike(String name);

	@Depth(value = 0)
	User findBySurname(String surname);

	@Query("MATCH (user:User) RETURN user.unknown as allRatings")
	EntityWrappingQueryResult findAllRatingsNull();

	@Query("match (u:User)-[r:RATED]->(m:Movie) return  u as user, collect({username: u.name, movietitle: m.title, stars:r.stars}) as literalMap")
	List<EntityWrappingQueryResult> findRatingsWithLiteralMap();

	Page<User> findByNameAndSurname(String name, String surname, Pageable pageable);

	Slice<User> findByNameAndRatingsStars(String name, int stars, Pageable pageable);

	@Query("invalid")
	void invalidQuery();

	User findByEmailAddressesContains(List<String> emails);

	List<User> findByEmailAddressesNotContaining(String email);

	@Query("MATCH (user:User) WHERE user.name=:#{#searchUser.name} RETURN user")
	User findUserByNameUsingSpElWithObject(@Param("searchUser") User user);

	@Query("MATCH (user:User) WHERE user.name=?#{[0]} RETURN user")
	User findUserByNameUsingSpElWithIndex(String name);

	@Query("MATCH (user:User) WHERE user.name=:#{[0]} and user.name=:#{[0]}RETURN user")
	User findUserByNameUsingSpElWithIndexColon(String name);

	@Query("MATCH (user:User) WHERE user.age=?#{5 + 5} RETURN user")
	User findUserByNameUsingSpElWithSpElExpression();

	@Query("MATCH (user:User) WHERE user.name=:#{#searchUser.name} and user.middleName=?#{#searchUser.middleName} RETURN user")
	User findUserByNameAndMiddleNameUsingSpElWithObject(@Param("searchUser") User user);

	@Query("MATCH (user:User) WHERE user.name=:#{'Michal'} RETURN user")
	User findUserByNameAndMiddleNameUsingSpElWithValue();

	@Query("MATCH (user:User) WHERE user.name=?#{[0]} and user.surname={name} RETURN user")
	User findUserByNameAndSurnameUsingSpElIndexAndPlaceholderWithOneParameter(@Param("name") String queryName);

	@Query("MATCH (user:User) WHERE user.name=:#{#name} and user.surname={name} RETURN user")
	User findUserByNameAndSurnameUsingSpElPropertyAndPlaceholderWithOneParameter(@Param("name") String queryName);

	@Query("MATCH (user:User) WHERE user.name=:#{#name} and user.surname={name} RETURN user")
	User findUserByNameAndSurnameUsingSpElPropertyAndIndexWithOneParameter(@Param("name") String queryName);

	@Query("MATCH (user:User) WHERE user.name=:#{#name} and user.surname=:#{#name} RETURN user")
	User findUserByNameAndSurnameUsingSpElPropertyTwice(@Param("name") String queryName);

	@Query("MATCH (user:User) WHERE user.name=:#{#name} and user.surname=:#{[0]} RETURN user")
	User findUserByNameAndSurnameUsingSpElPropertyAndSpElIndex(@Param("name") String queryName);

	@Query("MATCH (user:User) WHERE user.name={name} and user.name={0} and user.name=:#{#name} and user.name=:#{[0]} RETURN user")
	User findUserByNameUsingNativeIndexAndNameAndSpElNameAndSpElIndex(@Param("name") String queryName);
}
