/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.examples.friends;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luanne Misquitta
 */
@Service
public class FriendService {

	@Autowired
	Session session;

	@Transactional
	public void createPersonAndFriends() {
		Person john = new Person();
		john.setFirstName("John");
		session.save(john);

		Person bob = new Person();
		bob.setFirstName("Bob");
		session.save(bob);

		Person bill = new Person();
		bill.setFirstName("Bill");
		session.save(bill);

		john = session.load(Person.class, john.getId());
		bob = session.load(Person.class, bob.getId());
		Friendship friendship1 = john.addFriend(bob);
		friendship1.setTimestamp(System.currentTimeMillis());
		session.save(john);
		john = session.load(Person.class, john.getId());
		bill = session.load(Person.class, bill.getId());
		Friendship friendship2 = john.addFriend(bill);
		friendship2.setTimestamp(System.currentTimeMillis());
		session.save(john);
	}
}
