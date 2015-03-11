package org.neo4j.cineasts.domain;


import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author mh
 * @since 10.11.11
 */
public class Actor extends Person {

    @Relationship(type = "ACTS_IN", direction = Relationship.OUTGOING)
    Set<Role> roles = new HashSet<Role>();

    public Actor() {
    }

    public Actor(String id, String name) {
        super(id, name);
    }

    public Actor(String id) {
        super(id, null);
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Role playedIn(Movie movie, String roleName) {
        final Role role = new Role(this, movie, roleName);
        roles.add(role);
        movie.addRole(role);
        return role;
    }

}
