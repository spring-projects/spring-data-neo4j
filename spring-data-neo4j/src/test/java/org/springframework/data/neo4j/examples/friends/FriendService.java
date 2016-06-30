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

package org.springframework.data.neo4j.examples.friends;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.friends.domain.Friendship;
import org.springframework.data.neo4j.examples.friends.domain.Person;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luanne Misquitta
 */
@Service
public class FriendService {

	@Autowired
	Neo4jOperations neo4jOperations;

	@Transactional
	public void createPersonAndFriends() {
		Person john = new Person();
		john.setFirstName("John");
		neo4jOperations.save(john);

		Person bob = new Person();
		bob.setFirstName("Bob");
		neo4jOperations.save(bob);

		Person bill = new Person();
		bill.setFirstName("Bill");
		neo4jOperations.save(bill);

		john = neo4jOperations.load(Person.class, john.getId());
		bob = neo4jOperations.load(Person.class, bob.getId());
		Friendship friendship1 = john.addFriend(bob);
		friendship1.setTimestamp(System.currentTimeMillis());
		neo4jOperations.save(john);
		john = neo4jOperations.load(Person.class, john.getId());
		bill = neo4jOperations.load(Person.class, bill.getId());
		Friendship friendship2 = john.addFriend(bill);
		friendship2.setTimestamp(System.currentTimeMillis());
		neo4jOperations.save(john);
	}
}
