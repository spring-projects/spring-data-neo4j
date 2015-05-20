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

package org.springframework.data.neo4j.integration.web.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.web.domain.User;
import org.springframework.data.neo4j.integration.web.repo.UserRepository;
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
    private Session session;

    @Transactional
    @Override
    public User getUserByName(String name) {
        Iterable<User> users = findByProperty("name", name);
        if (!users.iterator().hasNext()) {
            return null;
        }
        return users.iterator().next();
    }

    @Transactional
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

    protected Iterable<User> findByProperty(String propertyName, Object propertyValue) {
        return session.loadAll(User.class, new Filter(propertyName, propertyValue));
    }

}
