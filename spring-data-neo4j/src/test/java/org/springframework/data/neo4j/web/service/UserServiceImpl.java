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
package org.springframework.data.neo4j.web.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.web.domain.User;
import org.springframework.data.neo4j.web.repo.UserRepository;
import org.springframework.stereotype.Service;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 */
@Service
public class UserServiceImpl implements UserService {

	@Autowired private UserRepository userRepository;

	@Override
	public User getUserByUuid(UUID uuid) {
		return userRepository.findById(uuid).get();
	}

	@Override
	public Collection<User> getNetwork(User user) {
		Set<User> network = new TreeSet<>(new Comparator<User>() {
			@Override
			public int compare(User u1, User u2) {
				return u1.getName().compareTo(u2.getName());
			}
		});
		buildNetwork(user, network);
		network.remove(user);
		return network;
	}

	private void buildNetwork(User user, Set<User> network) {
		for (User friend : user.getFriends()) {
			if (!network.contains(friend)) {
				network.add(friend);
				buildNetwork(friend, network);
			}
		}
	}
}
