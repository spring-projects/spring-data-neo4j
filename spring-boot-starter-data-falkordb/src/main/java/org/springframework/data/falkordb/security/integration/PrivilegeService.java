/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.integration;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.Role;

/**
 * Service responsible for loading and caching privileges for a given set of roles.
 *
 * MVP implementation: caches privileges per username for a short TTL to avoid
 * repeated graph queries on every request.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class PrivilegeService {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(1);

	private final FalkorDBTemplate template;

	private final Duration ttl;

	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	public PrivilegeService(FalkorDBTemplate template) {
		this(template, DEFAULT_TTL);
	}

	public PrivilegeService(FalkorDBTemplate template, Duration ttl) {
		this.template = template;
		this.ttl = ttl != null ? ttl : DEFAULT_TTL;
	}

	public Set<Privilege> loadPrivileges(String username, Set<Role> roles) {
		if (username == null || roles == null || roles.isEmpty()) {
			return Collections.emptySet();
		}

		CacheEntry entry = this.cache.get(username);
		if (entry != null && !entry.isExpired()) {
			return entry.privileges;
		}

		Set<Privilege> loaded = loadPrivilegesFromGraph(roles);
		this.cache.put(username, new CacheEntry(loaded, Instant.now().plus(this.ttl)));
		return loaded;
	}

	/**
	 * Manually evict cached privileges for a user.
	 */
	public void invalidate(String username) {
		if (username != null) {
			this.cache.remove(username);
		}
	}

	/**
	 * Manually evict all cached privileges.
	 */
	public void invalidateAll() {
		this.cache.clear();
	}

	private Set<Privilege> loadPrivilegesFromGraph(Set<Role> roles) {
		Set<String> roleNames = new HashSet<>();
		for (Role role : roles) {
			if (role != null && role.getName() != null) {
				roleNames.add(role.getName());
			}
		}
		if (roleNames.isEmpty()) {
			return Collections.emptySet();
		}

		String cypher = "MATCH (r:_Security_Role)<-[:GRANTED_TO]-(p:_Security_Privilege) "
				+ "WHERE r.name IN $roleNames RETURN p";
		return new HashSet<>(this.template.query(cypher,
				Collections.singletonMap("roleNames", roleNames), Privilege.class));
	}

	private static class CacheEntry {

		private final Set<Privilege> privileges;

		private final Instant expiresAt;

		CacheEntry(Set<Privilege> privileges, Instant expiresAt) {
			this.privileges = Collections.unmodifiableSet(privileges);
			this.expiresAt = expiresAt;
		}

		boolean isExpired() {
			return Instant.now().isAfter(this.expiresAt);
		}
	}

}
