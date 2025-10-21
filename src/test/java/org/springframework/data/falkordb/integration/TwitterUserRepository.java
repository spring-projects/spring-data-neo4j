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
package org.springframework.data.falkordb.integration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.falkordb.repository.FalkorDBRepository;
import org.springframework.data.falkordb.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for TwitterUser entities.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public interface TwitterUserRepository extends FalkorDBRepository<TwitterUser, Long> {

	// Derived query methods
	Optional<TwitterUser> findByUsername(String username);

	List<TwitterUser> findByDisplayNameContaining(String displayName);

	List<TwitterUser> findByVerified(Boolean verified);

	List<TwitterUser> findByFollowerCountGreaterThan(Integer followerCount);

	List<TwitterUser> findByLocationContaining(String location);

	// Custom query methods using @Query annotation
	@Query("MATCH (u:User)-[:FOLLOWS]->(f:User) WHERE u.username = $username RETURN f")
	List<TwitterUser> findFollowing(@Param("username") String username);

	@Query("MATCH (f:User)-[:FOLLOWS]->(u:User) WHERE u.username = $username RETURN f")
	List<TwitterUser> findFollowers(@Param("username") String username);

	@Query("MATCH (u:User) WHERE u.followerCount > $0 AND u.verified = $1 RETURN u ORDER BY u.followerCount DESC")
	List<TwitterUser> findTopVerifiedUsers(Integer minFollowers, Boolean verified);

	@Query(value = "MATCH (u:User)-[:FOLLOWS]->() WHERE u.username = $username RETURN count(*)", count = true)
	Long countFollowing(@Param("username") String username);

	@Query(value = "MATCH ()-[:FOLLOWS]->(u:User) WHERE u.username = $username RETURN count(*)", count = true)
	Long countFollowers(@Param("username") String username);

}
