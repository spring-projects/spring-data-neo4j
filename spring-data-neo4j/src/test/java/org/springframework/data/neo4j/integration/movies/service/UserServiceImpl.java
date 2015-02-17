package org.springframework.data.neo4j.integration.movies.service;

import org.springframework.data.neo4j.integration.movies.domain.Genre;
import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.data.neo4j.integration.movies.repo.GenreRepository;
import org.springframework.data.neo4j.integration.movies.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
