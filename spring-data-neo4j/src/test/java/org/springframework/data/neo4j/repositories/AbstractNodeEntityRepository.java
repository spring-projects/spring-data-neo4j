package org.springframework.data.neo4j.repositories;

import org.springframework.data.neo4j.model.AbstractNodeEntity;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * This Repository has specifically been created against the abstract node entity
 * class, to be able to test loading concrete specific node entities.
 */
public interface AbstractNodeEntityRepository extends GraphRepository<AbstractNodeEntity> {
}
