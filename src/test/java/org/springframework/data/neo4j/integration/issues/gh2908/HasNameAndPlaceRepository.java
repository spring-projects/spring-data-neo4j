/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2908;

import org.neo4j.driver.types.Point;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * Just extending {@link Neo4jRepository} here did not work, so it's split in the concrete
 * interface.
 *
 * @param <T> concrete type of the entity
 * @author Michael J. Simons
 */
public interface HasNameAndPlaceRepository<T extends HasNameAndPlace> {

	GeoResults<T> findAllAsGeoResultsByPlaceNear(Point point);

	GeoResults<T> findAllByPlaceNear(Point p, Distance max);

	GeoResults<T> findAllByPlaceNear(Point p, Range<Distance> between);

	GeoPage<T> findAllByPlaceNear(Point p, Range<Distance> between, Pageable pageable);

}
