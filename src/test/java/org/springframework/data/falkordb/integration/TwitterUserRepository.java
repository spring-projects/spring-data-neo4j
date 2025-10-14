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

	// Custom query methods would be added here using @Query annotation
	// For example:
	// @Query("MATCH (u:User)-[:FOLLOWS]->(f:User) WHERE u.username = $username RETURN f")
	// List<TwitterUser> findFollowing(@Param("username") String username);

}
