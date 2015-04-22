/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.movies.domain.queryresult;

import org.neo4j.ogm.annotation.Property;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.integration.movies.repo.UserRepository;

/**
 * Example interface annotated with {@link QueryResult} to test mapping onto proxied objects, where only getter methods are
 * needed to define the mapped result columns.
 *
 * @see UserRepository
 */
@QueryResult
public interface UserQueryResultInterface {

    @Property(name = "user.name")
    String getNameOfUser();

    int getAgeOfUser();

}
