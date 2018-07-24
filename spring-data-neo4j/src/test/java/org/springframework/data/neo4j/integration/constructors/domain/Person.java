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

package org.springframework.data.neo4j.integration.constructors.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * @author Nicolas Mervaillie
 */
@NodeEntity
public class Person {

	@Id private String name;

	@Relationship(type = "IS_FRIEND") private List<Friendship> friendships = new ArrayList<>();

	public Person(String name) {
		Assert.notNull(name, "name should not be null");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<Friendship> getFriendships() {
		return friendships;
	}

	public Friendship addFriend(Person newFriend) {
		Friendship friendship = new Friendship(this, newFriend, new Date(), new Point(1, 2));
		this.friendships.add(friendship);
		return friendship;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Person person = (Person) o;
		return Objects.equals(name, person.name);
	}

	@Override
	public int hashCode() {

		return Objects.hash(name);
	}
}
