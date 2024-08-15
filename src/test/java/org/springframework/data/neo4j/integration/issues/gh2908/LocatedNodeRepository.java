package org.springframework.data.neo4j.integration.issues.gh2908;

import java.util.List;

import org.neo4j.driver.types.Point;
import org.springframework.data.geo.Distance;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface LocatedNodeRepository extends Neo4jRepository<LocatedNode, String> {

	List<LocatedNode> findAllByPlaceNear(Point point);

	List<LocatedNode> findAllByPlaceNear(Point p, Distance max);
}
