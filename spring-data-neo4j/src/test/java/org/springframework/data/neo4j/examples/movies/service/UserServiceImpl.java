/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
