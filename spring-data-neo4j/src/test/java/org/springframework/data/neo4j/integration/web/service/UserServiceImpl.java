package org.springframework.data.neo4j.integration.web.service;

import org.springframework.data.neo4j.integration.web.domain.User;
import org.springframework.data.neo4j.integration.web.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @Override
    public User getUserByName(String name) {
        Iterable<User> users = userRepository.findByProperty("name", name);
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
}
