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
package org.springframework.data.neo4j.examples.movies.service;

import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.UseBookmark;
import org.springframework.data.neo4j.examples.movies.domain.Genre;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.GenreRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Michal Bachman
 * @author Mark Paluch
 * @author Jens Schauder
 */
@Service
public class UserServiceImpl implements UserService {

	@Autowired private UserRepository userRepository;

	@Autowired private GenreRepository genreRepository;

	@Override
	public void updateUser(User user, String newName) {
		user.setName(newName);
	}

	@Override
	public void notInterestedIn(Long userId, Long genreId) {
		Optional<User> user = userRepository.findById(userId);

		user.ifPresent(u -> {
			Optional<Genre> genre = genreRepository.findById(genreId);
			genre.ifPresent(u::notInterestedIn);

			userRepository.save(u);
		});
	}

	@Override
	public void saveWithTxAnnotationOnInterface(User user) {
		userRepository.save(user);
	}

	@Transactional
	@Override
	public void saveWithTxAnnotationOnImpl(User user) {
		userRepository.save(user);
	}

	@Override
	@Transactional
	@UseBookmark
	public Collection<User> getAllUsersWithBookmark() {
		return userRepository.getAllUsers();
	}
}
