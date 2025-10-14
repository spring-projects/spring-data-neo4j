/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.falkordb.examples;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.falkordb.repository.FalkorDBRepository;

/**
 * Example repository interface demonstrating JPA-style operations for FalkorDB. This
 * shows how users can define repository methods following Spring Data conventions.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public interface PersonRepository extends FalkorDBRepository<Person, Long> {

	/**
	 * Find a person by their name.
	 * @param name the person's name
	 * @return the person if found
	 */
	Optional<Person> findByName(String name);

	/**
	 * Find people by their email address.
	 * @param email the email address
	 * @return list of people with that email
	 */
	List<Person> findByEmail(String email);

	/**
	 * Find people whose age is greater than the specified value.
	 * @param age the minimum age
	 * @return list of people older than the specified age
	 */
	List<Person> findByAgeGreaterThan(int age);

	/**
	 * Find people whose age is between the specified values.
	 * @param minAge minimum age (inclusive)
	 * @param maxAge maximum age (inclusive)
	 * @return list of people in the age range
	 */
	List<Person> findByAgeBetween(int minAge, int maxAge);

	/**
	 * Find people whose name contains the specified string (case insensitive).
	 * @param nameFragment fragment of the name to search for
	 * @return list of people whose name contains the fragment
	 */
	List<Person> findByNameContainingIgnoreCase(String nameFragment);

	/**
	 * Find people ordered by name.
	 * @return list of all people ordered by name
	 */
	List<Person> findAllByOrderByNameAsc();

	/**
	 * Find people with pagination support.
	 * @param pageable pagination parameters
	 * @return page of people
	 */
	Page<Person> findByAgeGreaterThan(int age, Pageable pageable);

	/**
	 * Count people by age.
	 * @param age the age to count
	 * @return number of people with the specified age
	 */
	long countByAge(int age);

	/**
	 * Check if a person with the given email exists.
	 * @param email the email to check
	 * @return true if a person with that email exists
	 */
	boolean existsByEmail(String email);

	/**
	 * Delete people by age.
	 * @param age the age of people to delete
	 * @return number of people deleted
	 */
	long deleteByAge(int age);

}
