/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.nativetypes;

import org.neo4j.ogm.types.spatial.CartesianPoint2d;
import org.neo4j.ogm.types.spatial.CartesianPoint3d;
import org.neo4j.ogm.types.spatial.GeographicPoint2d;
import org.neo4j.ogm.types.spatial.GeographicPoint3d;
import org.springframework.data.geo.Distance;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface SpatialDomainRepository extends Neo4jRepository<SpatialDomain, Long> {
	List<SpatialDomain> findByGeographicPoint2d(GeographicPoint2d geographicPoint2d);

	List<SpatialDomain> findByGeographicPoint3d(GeographicPoint3d geographicPoint3d);

	List<SpatialDomain> findByCartesianPoint2d(CartesianPoint2d cartesianPoint2d);

	List<SpatialDomain> findByCartesianPoint3d(CartesianPoint3d cartesianPoint3d);

	List<SpatialDomain> findByCartesianPoint2dNear(Distance distance, CartesianPoint2d cartesianPoint2d);

	List<SpatialDomain> findByCartesianPoint3dNear(Distance distance, CartesianPoint3d cartesianPoint3d);

	List<SpatialDomain> findByGeographicPoint2dNear(Distance distance, GeographicPoint2d geographicPoint2d);

	List<SpatialDomain> findByGeographicPoint3dNear(Distance distance, GeographicPoint3d office);
}
