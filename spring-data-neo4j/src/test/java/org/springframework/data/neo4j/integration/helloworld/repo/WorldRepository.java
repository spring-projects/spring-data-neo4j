package org.springframework.data.neo4j.integration.helloworld.repo;

import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.helloworld.domain.World;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorldRepository extends GraphRepository<World> {

}
