/*
 * Copyright 2011-2024 the original author or authors.
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
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

import reactor.core.publisher.Flux;

/**
 * Repository spotting all supported Geo* results.
 * @author Michael J. Simons
 */
public interface ReactiveLocatedNodeRepository extends ReactiveNeo4jRepository<LocatedNode, String> {

	Flux<GeoResult<LocatedNode>> findAllAsGeoResultsByPlaceNear(Point point);

	Flux<GeoResult<LocatedNode>> findAllByPlaceNear(Point p, Distance max);

	Flux<GeoResult<LocatedNode>> findAllByPlaceNear(Point p, Range<Distance> between);
}
