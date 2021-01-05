/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.nativetypes;

import org.neo4j.ogm.types.spatial.CartesianPoint2d;
import org.neo4j.ogm.types.spatial.CartesianPoint3d;
import org.neo4j.ogm.types.spatial.GeographicPoint2d;
import org.neo4j.ogm.types.spatial.GeographicPoint3d;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
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

	List<SpatialDomain> findBySdnPointNear(Distance distance, Point aPoint);
}
