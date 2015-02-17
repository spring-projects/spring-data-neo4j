package org.springframework.data.neo4j.integration.helloworld.repo;

import org.springframework.data.neo4j.integration.helloworld.domain.World;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorldRepository extends GraphRepository<World> {}
