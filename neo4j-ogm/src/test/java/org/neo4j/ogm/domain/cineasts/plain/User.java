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

package org.neo4j.ogm.domain.cineasts.plain;

import java.util.Set;

public class User {

    String login;
    String name;
    String password;

    Set<Rating> ratings;
    Set<User> friends;

    Rating rate(Movie movie, int stars, String comment) {
        return null;
    }

    void befriend(User user) {
    }
}
