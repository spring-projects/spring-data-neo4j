/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.web.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 */
@NodeEntity
public class User {

	@Id @GeneratedValue private Long id;

	@Convert(UuidStringConverter.class) @Index(unique = true) private UUID uuid;

	private String name;

	private Collection<Genre> interested;

	@Relationship(type = "FRIEND_OF", direction = Relationship.UNDIRECTED) private Set<User> friends;

	public User() {}

	public User(String name) {
		this.interested = new HashSet<>();
		this.friends = new HashSet<>();
		this.uuid = UUID.randomUUID();
		this.name = name;
	}

	public void interestedIn(Genre genre) {
		interested.add(genre);
	}

	public void notInterestedIn(Genre genre) {
		interested.remove(genre);
	}

	public void befriend(User user) {
		friends.add(user);
	}

	// this doesn't need to be part of the domain, but this class is for testing
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<User> getFriends() {
		return friends;
	}

	public UUID getUuid() {
		return uuid;
	}
}
