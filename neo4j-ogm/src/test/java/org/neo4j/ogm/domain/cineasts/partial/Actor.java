/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.domain.cineasts.partial;

import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Actor {

    Long id;
    String name;

    @Relationship(type="ACTS_IN")
    List<Role> roles = new ArrayList<>();

    public Actor()  {}

    public Actor(String name) {
        this.name = name;
    }

    public void addRole(String character, Movie movie) {
        roles.add(new Role(character, this, movie));
    }

    public List<Role> roles() {
        return Collections.unmodifiableList(roles);
    }

    public void removeRole(String character) {

        Iterator<Role> iterator = roles.iterator();
        while (iterator.hasNext()) {
            Role role = iterator.next();
            if (role.played.equals(character)) {
                iterator.remove();
            }
        }
    }

}


