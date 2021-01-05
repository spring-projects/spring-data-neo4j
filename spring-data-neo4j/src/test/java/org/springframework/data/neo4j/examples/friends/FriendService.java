/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.examples.friends;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.friends.domain.Friendship;
import org.springframework.data.neo4j.examples.friends.domain.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@Service
public class FriendService {

	@Autowired Session session;

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
