/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.domain.cineasts.annotated;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

public class Actor {

    private Long id;
    private String name;
    private Set<Movie> filmography;

    @Relationship(type="ACTS_IN", direction="OUTGOING")
    private Set<Role> roles;

    private Set<Nomination> nominations;

    Actor() {
        // default constructor for OGM
    }

    public Actor(String name) {
        this.name = name;
    }

    public Role playedIn(Movie movie, String roleName) {
        if(roles==null) {
            roles = new HashSet<>();
        }
        Role role = new Role(movie,this,roleName);
        roles.add(role);
        return role;
    }

    public Nomination nominatedFor(Movie movie, String nominationName, int year) {
        if(nominations==null) {
            nominations = new HashSet<>();
        }
        Nomination nomination = new Nomination(movie,this,nominationName,year);
        nominations.add(nomination);
        return nomination;
    }

    public String getName() {
        return name;
    }
}
