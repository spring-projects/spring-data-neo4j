package org.springframework.data.neo4j.integration.movies.service;

import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */

public interface UserService {

    @Transactional
    void updateUser(User user, String newName);

    @Transactional
    void notInterestedIn(Long userId, Long genreId);

    @Transactional
    void saveWithTxAnnotationOnInterface(User user);

    void saveWithTxAnnotationOnImpl(User user);
}
