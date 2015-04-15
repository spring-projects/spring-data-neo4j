/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.domain.social;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.Relationship;

/**
 * POJO used to test the direction of an undirected relationship.
 *
 * @author Luanne Misquitta
 */
public class User {

	private Long id;
	private String name;

	@Relationship(type = "FRIEND", direction = "UNDIRECTED")
	private List<User> friends=new ArrayList<>();

	public User() {
	}

	public User(String name) {
		this.name = name;
	}

	public List<User> getFriends() {
		return friends;
	}

	public void setFriends(List<User> friends) {
		this.friends = friends;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
