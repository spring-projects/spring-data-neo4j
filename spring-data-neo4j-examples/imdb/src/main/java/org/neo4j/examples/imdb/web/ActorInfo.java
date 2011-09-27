package org.neo4j.examples.imdb.web;

import org.neo4j.examples.imdb.domain.Actor;
import org.neo4j.examples.imdb.domain.Role;

/**
* @author mh
* @since 01.04.11
*/
public final class ActorInfo implements Comparable<ActorInfo> {
    private String name;
    private String role;

    public ActorInfo(final Actor actor, final Role role) {
        setName(actor.getName());
        if (role == null || role.getName() == null) {
            setRole("(unknown)");
        } else {
            setRole(role.getName());
        }
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public int compareTo(ActorInfo otherActorInfo) {
        return getName().compareTo(otherActorInfo.getName());
    }
}
