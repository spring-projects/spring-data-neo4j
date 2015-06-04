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

package org.springframework.data.neo4j.web.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michal Bachman
 */
public class Cinema {

    private Long id;
    private String name;

    @Relationship(direction = Relationship.INCOMING)
    private Set<User> visited = new HashSet<>();

    public Cinema() {
    }

    public Cinema(String name) {
        this.name = name;
    }

    public void addVisitor(User user) {
        visited.add(user);
    }

    public String getName() {
        return name;
    }
}
