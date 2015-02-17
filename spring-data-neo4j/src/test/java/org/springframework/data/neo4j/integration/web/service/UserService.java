package org.springframework.data.neo4j.integration.web.service;

import org.springframework.data.neo4j.integration.web.domain.User;

import java.util.Collection;

public interface UserService {

    User getUserByName(String name);

    Collection<User> getNetwork(User user);
}
