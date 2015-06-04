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

package org.springframework.data.neo4j.examples.movies.service;

import org.springframework.data.neo4j.examples.movies.domain.Genre;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.movies.domain.Genre;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.GenreRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Michal Bachman
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Override
    public void updateUser(User user, String newName) {
        user.setName(newName);
    }

    @Override
    public void notInterestedIn(Long userId, Long genreId) {
        User user = userRepository.findOne(userId);
        Genre genre = genreRepository.findOne(genreId);

        user.notInterestedIn(genre);
        userRepository.save(user);
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
}
