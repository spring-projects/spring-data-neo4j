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

package org.springframework.data.neo4j.examples.galaxy.repo;

import org.neo4j.ogm.session.result.QueryStatistics;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vince Bickers
 */
@Repository
public interface WorldRepository extends GraphRepository<World> {

    @Query("MATCH (n:World) SET n.updated=timestamp()")
    public void touchAllWorlds();

    @Query("MATCH (n:World) SET n.updated=timestamp()")
    public QueryStatistics touchAllWorldsWithStatistics();

    @Query("MATCH (n:World)-[:REACHABLE_BY_ROCKET]->(m) SET m.updated=timestamp()")
    public Void touchOnlyWorldsReachableByRocket();

}
