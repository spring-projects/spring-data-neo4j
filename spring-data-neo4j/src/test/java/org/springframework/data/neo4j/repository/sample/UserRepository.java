/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
package org.springframework.data.neo4j.repository.sample;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for {@code User}s.
 *
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 */
public interface UserRepository extends Neo4jRepository<User, Long> {

	/**
	 * Retrieve users by their lastname. The finder {@literal User.findByLastname} is declared in
	 * {@literal META-INF/orm.xml} .
	 *
	 * @param lastname
	 * @return all users with the given lastname
	 */
	List<User> findByLastname(String lastname);

	/**
	 * Redeclaration of {@link CrudRepository#findOne(java.io.Serializable)} to change transaction configuration.
	 */
	@Override
	@Transactional
	Optional<User> findById(Long primaryKey);

	/**
	 * Redeclaration of {@link CrudRepository#delete(java.io.Serializable)}. to make sure the transaction configuration of
	 * the original method is considered if the redeclaration does not carry a {@link Transactional} annotation.
	 */
	@Override
	void deleteById(Long id);

	/**
	 * Retrieve users by their email address. The finder {@literal User.findByEmailAddress} is declared as annotation at
	 * {@code User}.
	 *
	 * @param emailAddress
	 * @return the user with the given email address
	 */
	User findByEmailAddress(String emailAddress);

	/**
	 * Retrieves a user by its username using the query annotated to the method.
	 *
	 * @param emailAddress
	 * @return
	 */
	@Query("MATCH (n:User{emailAddress:{emailAddress}}) return n")
	@Transactional(readOnly = true)
	User findByAnnotatedQuery(@Param("emailAddress") String emailAddress);
}
