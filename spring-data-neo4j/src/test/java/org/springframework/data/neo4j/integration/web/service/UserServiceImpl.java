package org.springframework.data.neo4j.integration.web.service;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.web.domain.User;
import org.springframework.data.neo4j.integration.web.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

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
        return session.loadByProperty(User.class, new Property(propertyName, propertyValue));
    }

}
