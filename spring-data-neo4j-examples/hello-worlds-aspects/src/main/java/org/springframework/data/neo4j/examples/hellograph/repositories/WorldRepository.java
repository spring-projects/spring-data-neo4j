package org.springframework.data.neo4j.examples.hellograph.repositories;

import org.springframework.data.neo4j.examples.hellograph.domain.World;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface WorldRepository extends GraphRepository<World> {}
